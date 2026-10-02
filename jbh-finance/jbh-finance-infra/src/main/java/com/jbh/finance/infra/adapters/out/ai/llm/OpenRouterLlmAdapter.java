package com.jbh.finance.infra.adapters.out.ai.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
 * Extracts statement movements from text with OpenRouter (REST {@code /chat/completions}).
 *
 * <p>OpenRouter tries the configured models in order and moves to the next one when a model is busy
 * or fails. Active when {@code jbh.statement.llm=openrouter}; see {@code
 * StatementUseCasesCDIConfig}.
 *
 * <p>The text, the answer and the movements are personal data: only HTTP status codes, error
 * types, the model name and counts are logged.
 */
@ApplicationScoped
public class OpenRouterLlmAdapter implements StatementLlm {

  private static final Logger LOGGER = LoggerFactory.getLogger(OpenRouterLlmAdapter.class);
  private static final String LOG_PREFIX = "[StatementImport:OpenRouter]";

  private static final int HTTP_OK = 200;
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
  private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";
  private static final String JSON_MIME_TYPE = "application/json";
  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER = "Bearer ";
  private static final String CONTENT_TYPE_HEADER = "Content-Type";
  private static final String TITLE_HEADER = "X-OpenRouter-Title";
  private static final String APP_TITLE = "JBH Personal Finance";
  private static final String ROLE = "role";
  private static final String CONTENT = "content";
  private static final String ERROR = "error";
  private static final String UNKNOWN = "unknown";

  private static final String RESPONSE_SCHEMA =
      """
      {
        "type": "object",
        "properties": {
          "balances": {
            "type": "array",
            "items": {
              "type": "object",
              "properties": {
                "currency": { "type": "string", "description": "ISO 4217 code, for example COP" },
                "openingBalance": { "type": ["string", "null"] },
                "closingBalance": { "type": ["string", "null"] }
              },
              "required": ["currency", "openingBalance", "closingBalance"],
              "additionalProperties": false
            }
          },
          "movements": {
            "type": "array",
            "items": {
              "type": "object",
              "properties": {
                "date": { "type": "string", "description": "yyyy-MM-dd" },
                "description": { "type": "string" },
                "amount": { "type": "string", "description": "Positive decimal, dot as decimal separator" },
                "currency": { "type": "string", "description": "ISO 4217 code, for example COP" },
                "direction": { "type": "string", "enum": ["CREDIT", "DEBIT"] }
              },
              "required": ["date", "description", "amount", "currency", "direction"],
              "additionalProperties": false
            }
          }
        },
        "required": ["balances", "movements"],
        "additionalProperties": false
      }
      """;

  private final StatementImportConfig config;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;
  private final JsonNode responseSchema;

  @Inject
  public OpenRouterLlmAdapter(final StatementImportConfig config, final ObjectMapper objectMapper) {
    this.config = config;
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
    try {
      this.responseSchema = objectMapper.readTree(RESPONSE_SCHEMA);
    } catch (final JsonProcessingException e) {
      throw new IllegalStateException("Invalid OpenRouter response schema", e);
    }
  }

  @Override
  // The cause is dropped on purpose: it may carry parts of the request or the answer.
  @SuppressWarnings("PMD.PreserveStackTrace")
  public String answer(final String statementText) throws BusinessException {
    final String apiKey =
        config
            .openRouter()
            .apiKey()
            .filter(key -> !key.isBlank())
            .orElseThrow(
                () ->
                    new BusinessException(
                        FinanceInfraBusinessExceptionType.STATEMENT_AI_NOT_CONFIGURED));

    try {
      final long started = System.nanoTime();
      LOGGER.info(
          LOG_PREFIX + " Sending {} characters to models {}",
          statementText.length(),
          config.openRouter().models());
      final HttpResponse<byte[]> response =
          httpClient.send(
              buildRequest(apiKey, statementText), HttpResponse.BodyHandlers.ofByteArray());
      LOGGER.info(
          LOG_PREFIX + " HTTP {} in {} ms",
          response.statusCode(),
          Duration.ofNanos(System.nanoTime() - started).toMillis());
      if (response.statusCode() != HTTP_OK) {
        LOGGER.warn(
            LOG_PREFIX + " OpenRouter returned HTTP {}, error type {}",
            response.statusCode(),
            errorTypeOf(response.body()));
        throw readerFailed();
      }
      final JsonNode root = objectMapper.readTree(response.body());
      if (root.has(ERROR)) {
        // All models failed after the answer started; the HTTP status is still 200.
        LOGGER.warn(LOG_PREFIX + " OpenRouter returned an error, error type {}", errorTypeOf(root));
        throw readerFailed();
      }
      return answerText(root);

    } catch (final IOException e) {
      LOGGER.warn(LOG_PREFIX + " OpenRouter call failed: {}", e.getClass().getSimpleName());
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
    return HttpRequest.newBuilder(URI.create(config.openRouter().baseUrl() + CHAT_COMPLETIONS_PATH))
        .timeout(Duration.ofSeconds(config.openRouter().timeoutSeconds()))
        .header(CONTENT_TYPE_HEADER, JSON_MIME_TYPE)
        .header(AUTHORIZATION_HEADER, BEARER + apiKey)
        .header(TITLE_HEADER, APP_TITLE)
        .POST(HttpRequest.BodyPublishers.ofByteArray(objectMapper.writeValueAsBytes(buildBody(statementText))))
        .build();
  }

  private ObjectNode buildBody(final String statementText) {
    final ObjectNode body = objectMapper.createObjectNode();

    // Tried in order: the next model answers when one is busy or fails.
    final ArrayNode models = body.putArray("models");
    config.openRouter().models().forEach(models::add);

    final ArrayNode messages = body.putArray("messages");
    messages.addObject().put(ROLE, "system").put(CONTENT, StatementExtractionFormat.SYSTEM_PROMPT);
    messages
        .addObject()
        .put(ROLE, "user")
        .put(CONTENT, StatementExtractionFormat.userPrompt(statementText));

    body.put("temperature", 0);

    final ObjectNode jsonSchema =
        body.putObject("response_format").put("type", "json_schema").putObject("json_schema");
    jsonSchema.put("name", "bank_statement");
    jsonSchema.put("strict", true);
    jsonSchema.set("schema", responseSchema);

    // Skip providers that would ignore response_format instead of following it.
    body.putObject("provider").put("require_parameters", true);

    return body;
  }

  private String answerText(final JsonNode root) throws BusinessException {
    final String content =
        root.path("choices").path(0).path("message").path(CONTENT).asText("");
    if (content.isBlank()) {
      LOGGER.warn(LOG_PREFIX + " OpenRouter returned no statement text");
      throw readerFailed();
    }

    LOGGER.info(LOG_PREFIX + " OpenRouter answered with model {}", root.path("model").asText(UNKNOWN));
    return content;
  }

  /** The normalized error type of an error body, for example {@code rate_limit_exceeded}. */
  private String errorTypeOf(final byte[] body) {
    try {
      return errorTypeOf(objectMapper.readTree(body));
    } catch (final IOException e) {
      return UNKNOWN;
    }
  }

  private static String errorTypeOf(final JsonNode root) {
    return root.path(ERROR).path("metadata").path("error_type").asText(UNKNOWN);
  }

  private static BusinessException readerFailed() {
    return new BusinessException(FinanceInfraBusinessExceptionType.STATEMENT_READER_FAILED);
  }
}
