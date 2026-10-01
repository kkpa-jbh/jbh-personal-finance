package com.jbh.finance.infra.adapters.out.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.statement.dto.ExtractedStatementDTO;
import com.jbh.finance.application.feature.statement.ports.output.StatementReader;
import com.jbh.finance.infra.adapters.exceptions.FinanceInfraBusinessExceptionType;
import com.jbh.finance.infra.config.StatementImportConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads a statement PDF with Google Gemini (REST {@code generateContent}).
 *
 * <p>This is the only Gemini-specific class. Active when {@code jbh.statement.reader=gemini}; see
 * {@code StatementUseCasesCDIConfig}.
 *
 * <p>The PDF, the prompt answer and the movements are personal data: only HTTP status codes and
 * counts are logged.
 */
@ApplicationScoped
public class GeminiStatementReaderAdapter implements StatementReader {

  private static final Logger LOGGER = LoggerFactory.getLogger(GeminiStatementReaderAdapter.class);

  private static final int HTTP_OK = 200;
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
  private static final String PDF_MIME_TYPE = "application/pdf";
  private static final String JSON_MIME_TYPE = "application/json";
  private static final String API_KEY_HEADER = "x-goog-api-key";
  private static final String CONTENT_TYPE_HEADER = "Content-Type";
  private static final String USER_PROMPT = "Extract every movement from this bank statement.";
  private static final String TEXT = "text";
  private static final String PARTS = "parts";

  private static final String SYSTEM_PROMPT =
      "You read bank account and credit card statements. Return every movement row.\n"
          + StatementExtractionFormat.RULES;

  private static final String RESPONSE_SCHEMA =
      """
      {
        "type": "OBJECT",
        "properties": {
          "openingBalance": { "type": "STRING", "nullable": true },
          "closingBalance": { "type": "STRING", "nullable": true },
          "movements": {
            "type": "ARRAY",
            "items": {
              "type": "OBJECT",
              "properties": {
                "date": { "type": "STRING", "description": "yyyy-MM-dd" },
                "description": { "type": "STRING" },
                "amount": { "type": "STRING", "description": "Positive decimal, dot as decimal separator" },
                "direction": { "type": "STRING", "format": "enum", "enum": ["CREDIT", "DEBIT"] }
              },
              "required": ["date", "description", "amount", "direction"]
            }
          }
        },
        "required": ["movements"]
      }
      """;

  private final StatementImportConfig config;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;
  private final JsonNode responseSchema;

  @Inject
  public GeminiStatementReaderAdapter(
      final StatementImportConfig config, final ObjectMapper objectMapper) {
    this.config = config;
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
    try {
      this.responseSchema = objectMapper.readTree(RESPONSE_SCHEMA);
    } catch (final JsonProcessingException e) {
      throw new IllegalStateException("Invalid Gemini response schema", e);
    }
  }

  @Override
  // The cause is dropped on purpose: it may carry parts of the request or the answer.
  @SuppressWarnings("PMD.PreserveStackTrace")
  public ExtractedStatementDTO read(final byte[] pdfContent) throws BusinessException {
    final String apiKey =
        config
            .gemini()
            .apiKey()
            .filter(key -> !key.isBlank())
            .orElseThrow(
                () ->
                    new BusinessException(
                        FinanceInfraBusinessExceptionType.STATEMENT_AI_NOT_CONFIGURED));

    try {
      final HttpResponse<byte[]> response =
          httpClient.send(buildRequest(apiKey, pdfContent), HttpResponse.BodyHandlers.ofByteArray());
      if (response.statusCode() != HTTP_OK) {
        LOGGER.warn("Gemini returned HTTP {}", response.statusCode());
        throw readerFailed();
      }
      return parseStatement(objectMapper.readTree(response.body()));

    } catch (final IOException e) {
      LOGGER.warn("Gemini call failed: {}", e.getClass().getSimpleName());
      throw readerFailed();
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      throw readerFailed();
    }
  }

  private HttpRequest buildRequest(final String apiKey, final byte[] pdfContent)
      throws JsonProcessingException {
    final String url =
        config.gemini().baseUrl() + "/models/" + config.gemini().model() + ":generateContent";

    return HttpRequest.newBuilder(URI.create(url))
        .timeout(Duration.ofSeconds(config.gemini().timeoutSeconds()))
        .header(CONTENT_TYPE_HEADER, JSON_MIME_TYPE)
        .header(API_KEY_HEADER, apiKey)
        .POST(HttpRequest.BodyPublishers.ofByteArray(objectMapper.writeValueAsBytes(buildBody(pdfContent))))
        .build();
  }

  private ObjectNode buildBody(final byte[] pdfContent) {
    final ObjectNode body = objectMapper.createObjectNode();

    body.putObject("systemInstruction").putArray(PARTS).addObject().put(TEXT, SYSTEM_PROMPT);

    final ObjectNode content = body.putArray("contents").addObject();
    content.put("role", "user");
    final ArrayNode parts = content.putArray(PARTS);
    final ObjectNode inlineData = parts.addObject().putObject("inlineData");
    inlineData.put("mimeType", PDF_MIME_TYPE);
    inlineData.put("data", Base64.getEncoder().encodeToString(pdfContent));
    parts.addObject().put(TEXT, USER_PROMPT);

    final ObjectNode generationConfig = body.putObject("generationConfig");
    generationConfig.put("temperature", 0);
    generationConfig.put("responseMimeType", JSON_MIME_TYPE);
    generationConfig.set("responseSchema", responseSchema);

    return body;
  }

  private ExtractedStatementDTO parseStatement(final JsonNode root)
      throws BusinessException, JsonProcessingException {
    final String text =
        root.path("candidates").path(0).path("content").path(PARTS).path(0).path(TEXT).asText("");
    if (text.isBlank()) {
      // For example a safety block (promptFeedback.blockReason).
      LOGGER.warn("Gemini returned no statement text");
      throw readerFailed();
    }

    return StatementExtractionFormat.parse(objectMapper.readTree(text));
  }

  private static BusinessException readerFailed() {
    return new BusinessException(FinanceInfraBusinessExceptionType.STATEMENT_READER_FAILED);
  }
}
