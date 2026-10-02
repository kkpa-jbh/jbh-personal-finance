package com.jbh.finance.infra.adapters.out.ai.llmcompare;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.infra.adapters.exceptions.FinanceInfraBusinessExceptionType;
import com.jbh.finance.infra.adapters.out.ai.llm.StatementExtractionFormat;
import com.jbh.finance.infra.adapters.out.ai.llm.StatementLlm;
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
 * Extracts statement movements from text with Ollama (REST {@code /api/chat}), for the LLM
 * comparison only.
 *
 * <p>Ollama Cloud does not support structured outputs, so the JSON shape is written in the prompt.
 * {@code StatementExtractionFormat.parse} removes a markdown code fence around the answer.
 *
 * <p>The text, the answer and the movements are personal data: only HTTP status codes, error
 * types, the model name and counts are logged.
 */
@ApplicationScoped
public class OllamaLlmAdapter implements StatementLlm {

  private static final Logger LOGGER = LoggerFactory.getLogger(OllamaLlmAdapter.class);
  private static final String LOG_PREFIX = "[StatementImport:Ollama]";

  private static final int HTTP_OK = 200;
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
  private static final String CHAT_PATH = "/chat";
  private static final String JSON_MIME_TYPE = "application/json";
  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER = "Bearer ";
  private static final String CONTENT_TYPE_HEADER = "Content-Type";
  private static final String ROLE = "role";
  private static final String CONTENT = "content";

  private static final String JSON_SHAPE =
      """

      Answer with one JSON object only, no other text:
      {"balances": [{"currency": "COP", "openingBalance": "123.45" or null,
                     "closingBalance": "123.45" or null}],
       "movements": [{"date": "yyyy-MM-dd", "description": "...", "amount": "123.45",
                      "currency": "COP", "direction": "CREDIT" or "DEBIT"}]}
      """;

  private final LlmCompareConfig config;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  @Inject
  public OllamaLlmAdapter(final LlmCompareConfig config, final ObjectMapper objectMapper) {
    this.config = config;
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
  }

  @Override
  // The cause is dropped on purpose: it may carry parts of the request or the answer.
  @SuppressWarnings("PMD.PreserveStackTrace")
  public String answer(final String statementText) throws BusinessException {
    final String apiKey =
        config
            .ollama()
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
          config.ollama().model());
      final HttpResponse<byte[]> response =
          httpClient.send(
              buildRequest(apiKey, statementText), HttpResponse.BodyHandlers.ofByteArray());
      LOGGER.info(
          LOG_PREFIX + " HTTP {} in {} ms",
          response.statusCode(),
          Duration.ofNanos(System.nanoTime() - started).toMillis());
      if (response.statusCode() != HTTP_OK) {
        LOGGER.warn(LOG_PREFIX + " Ollama returned HTTP {}", response.statusCode());
        throw readerFailed();
      }
      return answerText(response.body());

    } catch (final IOException e) {
      LOGGER.warn(LOG_PREFIX + " Ollama call failed: {}", e.getClass().getSimpleName());
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
    return HttpRequest.newBuilder(URI.create(config.ollama().baseUrl() + CHAT_PATH))
        .timeout(Duration.ofSeconds(config.ollama().timeoutSeconds()))
        .header(CONTENT_TYPE_HEADER, JSON_MIME_TYPE)
        .header(AUTHORIZATION_HEADER, BEARER + apiKey)
        .POST(
            HttpRequest.BodyPublishers.ofByteArray(
                objectMapper.writeValueAsBytes(buildBody(statementText))))
        .build();
  }

  private ObjectNode buildBody(final String statementText) {
    final ObjectNode body = objectMapper.createObjectNode();
    body.put("model", config.ollama().model());
    body.put("stream", false);

    final ArrayNode messages = body.putArray("messages");
    messages
        .addObject()
        .put(ROLE, "system")
        .put(CONTENT, StatementExtractionFormat.SYSTEM_PROMPT + JSON_SHAPE);
    messages
        .addObject()
        .put(ROLE, "user")
        .put(CONTENT, StatementExtractionFormat.userPrompt(statementText));

    body.putObject("options").put("temperature", 0);
    return body;
  }

  private String answerText(final byte[] body) throws BusinessException, IOException {
    final String content =
        objectMapper.readTree(body).path("message").path(CONTENT).asText("");
    if (content.isBlank()) {
      LOGGER.warn(LOG_PREFIX + " Ollama returned no statement text");
      throw readerFailed();
    }
    return content;
  }

  private static BusinessException readerFailed() {
    return new BusinessException(FinanceInfraBusinessExceptionType.STATEMENT_READER_FAILED);
  }
}
