package com.jbh.finance.infra.adapters.out.ai.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jbh.commons.exception.BusinessException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extracts statement movements from text with Google Gemini (REST {@code generateContent}).
 *
 * <p>This is the only Gemini-specific class. Active when {@code jbh.statement.llm=gemini}; see
 * {@code StatementUseCasesCDIConfig}.
 *
 * <p>The text, the answer and the movements are personal data: only HTTP status codes and counts
 * are logged.
 */
@ApplicationScoped
public class GeminiLlmAdapter implements StatementLlm {

  private static final Logger LOGGER = LoggerFactory.getLogger(GeminiLlmAdapter.class);
  private static final String LOG_PREFIX = "[StatementImport:Gemini]";

  private static final int HTTP_OK = 200;
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
  private static final String JSON_MIME_TYPE = "application/json";
  private static final String API_KEY_HEADER = "x-goog-api-key";
  private static final String CONTENT_TYPE_HEADER = "Content-Type";
  private static final String TEXT = "text";
  private static final String PARTS = "parts";

  private static final String RESPONSE_SCHEMA =
      """
      {
        "type": "OBJECT",
        "properties": {
          "balances": {
            "type": "ARRAY",
            "items": {
              "type": "OBJECT",
              "properties": {
                "currency": { "type": "STRING", "description": "ISO 4217 code, for example COP" },
                "openingBalance": { "type": "STRING", "nullable": true },
                "closingBalance": { "type": "STRING", "nullable": true }
              },
              "required": ["currency"]
            }
          },
          "movements": {
            "type": "ARRAY",
            "items": {
              "type": "OBJECT",
              "properties": {
                "date": { "type": "STRING", "description": "yyyy-MM-dd" },
                "description": { "type": "STRING" },
                "amount": { "type": "STRING", "description": "Positive decimal, dot as decimal separator" },
                "currency": { "type": "STRING", "description": "ISO 4217 code, for example COP" },
                "direction": { "type": "STRING", "format": "enum", "enum": ["CREDIT", "DEBIT"] }
              },
              "required": ["date", "description", "amount", "currency", "direction"]
            }
          }
        },
        "required": ["balances", "movements"]
      }
      """;

  private final StatementImportConfig config;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;
  private final JsonNode responseSchema;

  @Inject
  public GeminiLlmAdapter(
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
  public String answer(final String statementText) throws BusinessException {
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
      final long started = System.nanoTime();
      LOGGER.info(
          LOG_PREFIX + " Sending {} characters to model {}",
          statementText.length(),
          config.gemini().model());
      final HttpResponse<byte[]> response =
          httpClient.send(
              buildRequest(apiKey, statementText), HttpResponse.BodyHandlers.ofByteArray());
      LOGGER.info(
          LOG_PREFIX + " HTTP {} in {} ms",
          response.statusCode(),
          Duration.ofNanos(System.nanoTime() - started).toMillis());
      if (response.statusCode() != HTTP_OK) {
        LOGGER.warn(LOG_PREFIX + " Gemini returned HTTP {}", response.statusCode());
        throw readerFailed();
      }
      return answerText(objectMapper.readTree(response.body()));

    } catch (final IOException e) {
      LOGGER.warn(LOG_PREFIX + " Gemini call failed: {}", e.getClass().getSimpleName());
      throw readerFailed();
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      throw readerFailed();
    }
  }

  @Override
  public String requestBody(final String statementText) {
    try {
      return objectMapper
          .writerWithDefaultPrettyPrinter()
          .writeValueAsString(buildBody(statementText));
    } catch (final JsonProcessingException e) {
      return "";
    }
  }

  private HttpRequest buildRequest(final String apiKey, final String statementText)
      throws JsonProcessingException {
    final String url =
        config.gemini().baseUrl() + "/models/" + config.gemini().model() + ":generateContent";

    return HttpRequest.newBuilder(URI.create(url))
        .timeout(Duration.ofSeconds(config.gemini().timeoutSeconds()))
        .header(CONTENT_TYPE_HEADER, JSON_MIME_TYPE)
        .header(API_KEY_HEADER, apiKey)
        .POST(HttpRequest.BodyPublishers.ofByteArray(objectMapper.writeValueAsBytes(buildBody(statementText))))
        .build();
  }

  private ObjectNode buildBody(final String statementText) {
    final ObjectNode body = objectMapper.createObjectNode();

    body.putObject("systemInstruction")
        .putArray(PARTS)
        .addObject()
        .put(TEXT, StatementExtractionFormat.SYSTEM_PROMPT);

    final ObjectNode content = body.putArray("contents").addObject();
    content.put("role", "user");
    content
        .putArray(PARTS)
        .addObject()
        .put(TEXT, StatementExtractionFormat.userPrompt(statementText));

    final ObjectNode generationConfig = body.putObject("generationConfig");
    generationConfig.put("temperature", 0);
    generationConfig.put("responseMimeType", JSON_MIME_TYPE);
    generationConfig.set("responseSchema", responseSchema);

    return body;
  }

  private String answerText(final JsonNode root) throws BusinessException {
    final String text =
        root.path("candidates").path(0).path("content").path(PARTS).path(0).path(TEXT).asText("");
    if (text.isBlank()) {
      // For example a safety block (promptFeedback.blockReason).
      LOGGER.warn(LOG_PREFIX + " Gemini returned no statement text");
      throw readerFailed();
    }

    return text;
  }

  private static BusinessException readerFailed() {
    return new BusinessException(FinanceInfraBusinessExceptionType.STATEMENT_READER_FAILED);
  }
}
