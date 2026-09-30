package com.jbh.finance.infra.adapters.out.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.statement.dto.CategoryOptionDTO;
import com.jbh.finance.application.feature.statement.dto.ClassificationAnswerDTO;
import com.jbh.finance.application.feature.statement.dto.ClassificationQuestionDTO;
import com.jbh.finance.application.feature.statement.dto.StatementMovementDirection;
import com.jbh.finance.application.feature.statement.ports.output.MovementClassifier;
import com.jbh.finance.infra.adapters.exceptions.FinanceInfraBusinessExceptionType;
import com.jbh.finance.infra.config.StatementImportConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classifies statement movements with TypeSafe Jev (REST {@code /v1/systemone}, Choice
 * questions).
 *
 * <p>Only the movement description and direction are sent. The amount is not sent: Jev is weak
 * with numbers. Descriptions and request/response bodies are never logged.
 */
@ApplicationScoped
public class JevMovementClassifierAdapter implements MovementClassifier {

  private static final Logger LOGGER = LoggerFactory.getLogger(JevMovementClassifierAdapter.class);

  private static final int HTTP_OK = 200;
  private static final int HTTP_TOO_MANY_REQUESTS = 429;
  private static final int HTTP_OVERLOADED = 529;
  private static final int MAX_ATTEMPTS = 3;
  private static final long FIRST_RETRY_WAIT_MILLIS = 1000L;
  private static final long MAX_RETRY_AFTER_SECONDS = 10L;
  private static final long MILLIS_PER_SECOND = 1000L;
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);

  private static final String SYSTEMONE_PATH = "/v1/systemone";
  private static final String RETRY_AFTER_HEADER = "retry-after";
  private static final String CHOICE_TYPE = "choice";
  private static final String QUESTION = "Which category best describes `movement`?";
  private static final String CONTEXT =
      "Movements from a personal bank or credit card statement from Colombia. Descriptions are in"
          + " Spanish and often abbreviated (for example PSE, COMPRA, PAGO, TRANSF).";
  private static final String MONEY_IN = "money came into the account";
  private static final String MONEY_OUT = "money left the account";

  private final StatementImportConfig config;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  @Inject
  public JevMovementClassifierAdapter(
      final StatementImportConfig config, final ObjectMapper objectMapper) {
    this.config = config;
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
  }

  @Override
  // The cause is dropped on purpose: it may carry parts of the request or the answer.
  @SuppressWarnings("PMD.PreserveStackTrace")
  public List<ClassificationAnswerDTO> classify(final List<ClassificationQuestionDTO> questions)
      throws BusinessException {
    final String apiKey =
        config
            .jev()
            .apiKey()
            .filter(key -> !key.isBlank())
            .orElseThrow(
                () ->
                    new BusinessException(
                        FinanceInfraBusinessExceptionType.STATEMENT_AI_NOT_CONFIGURED));

    try {
      final HttpRequest request = buildRequest(apiKey, questions);
      final HttpResponse<byte[]> response = sendWithRetry(request);
      if (response.statusCode() != HTTP_OK) {
        LOGGER.warn("Jev returned HTTP {}", response.statusCode());
        throw classifierFailed();
      }
      return parseAnswers(objectMapper.readTree(response.body()), questions);

    } catch (final IOException e) {
      LOGGER.warn("Jev call failed: {}", e.getClass().getSimpleName());
      throw classifierFailed();
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      throw classifierFailed();
    }
  }

  private HttpResponse<byte[]> sendWithRetry(final HttpRequest request)
      throws IOException, InterruptedException {
    long waitMillis = FIRST_RETRY_WAIT_MILLIS;
    int attempt = 1;
    HttpResponse<byte[]> response =
        httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

    while (isRetryable(response.statusCode()) && attempt < MAX_ATTEMPTS) {
      LOGGER.warn("Jev returned HTTP {}, retrying", response.statusCode());
      Thread.sleep(retryAfterMillis(response).orElse(waitMillis));
      waitMillis *= 2;
      attempt++;
      response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }
    return response;
  }

  private static boolean isRetryable(final int statusCode) {
    return statusCode == HTTP_TOO_MANY_REQUESTS || statusCode == HTTP_OVERLOADED;
  }

  private static Optional<Long> retryAfterMillis(final HttpResponse<byte[]> response) {
    final Optional<String> header = response.headers().firstValue(RETRY_AFTER_HEADER);
    if (header.isEmpty()) {
      return Optional.empty();
    }
    try {
      final long seconds = Long.parseLong(header.get().trim());
      return Optional.of(Math.min(Math.max(seconds, 0L), MAX_RETRY_AFTER_SECONDS) * MILLIS_PER_SECOND);
    } catch (final NumberFormatException e) {
      // An HTTP date is not supported: use the default wait.
      return Optional.empty();
    }
  }

  private HttpRequest buildRequest(
      final String apiKey, final List<ClassificationQuestionDTO> questions)
      throws JsonProcessingException {
    return HttpRequest.newBuilder(URI.create(config.jev().baseUrl() + SYSTEMONE_PATH))
        .timeout(Duration.ofSeconds(config.jev().timeoutSeconds()))
        .header("Authorization", "Bearer " + apiKey)
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofByteArray(objectMapper.writeValueAsBytes(buildBody(questions))))
        .build();
  }

  private ObjectNode buildBody(final List<ClassificationQuestionDTO> questions) {
    final ObjectNode body = objectMapper.createObjectNode();
    body.put("model", config.jev().model());
    body.putObject("state").put("context", CONTEXT);

    final ObjectNode questionsNode = body.putObject("questions");
    for (final ClassificationQuestionDTO question : questions) {
      final ObjectNode questionNode = questionsNode.putObject(question.key());
      questionNode.put("type", CHOICE_TYPE);

      final ObjectNode instructions = questionNode.putObject("instructions");
      final ObjectNode movement = instructions.putObject("movement");
      movement.put("description", question.description());
      movement.put("direction", describe(question.direction()));
      instructions.put("question", QUESTION);

      // ObjectNode keeps insertion order, so the option order is kept.
      final ObjectNode criteria = questionNode.putObject("criteria");
      for (final CategoryOptionDTO option : question.options()) {
        criteria.put(option.alias(), option.rule());
      }
    }
    return body;
  }

  private static String describe(final StatementMovementDirection direction) {
    return direction == StatementMovementDirection.CREDIT ? MONEY_IN : MONEY_OUT;
  }

  private static List<ClassificationAnswerDTO> parseAnswers(
      final JsonNode root, final List<ClassificationQuestionDTO> questions) {
    final JsonNode answers = root.path("answers");
    final List<ClassificationAnswerDTO> result = new ArrayList<>(questions.size());

    for (final ClassificationQuestionDTO question : questions) {
      final JsonNode answer = answers.path(question.key());
      final JsonNode choice = answer.path(CHOICE_TYPE);
      final JsonNode confidence = answer.path("confidence");
      if (choice.isTextual() && confidence.isNumber()) {
        result.add(
            new ClassificationAnswerDTO(
                question.key(), choice.asText(), BigDecimal.valueOf(confidence.asDouble())));
      }
    }
    LOGGER.info("Jev answered {} of {} questions", result.size(), questions.size());
    return result;
  }

  private static BusinessException classifierFailed() {
    return new BusinessException(FinanceInfraBusinessExceptionType.STATEMENT_CLASSIFIER_FAILED);
  }
}
