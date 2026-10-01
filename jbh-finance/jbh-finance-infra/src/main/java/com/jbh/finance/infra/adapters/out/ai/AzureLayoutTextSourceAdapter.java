package com.jbh.finance.infra.adapters.out.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Turns a statement PDF into markdown with Azure AI Document Intelligence ({@code prebuilt-layout},
 * REST v4). Tables come back as HTML tables inside the markdown, with their columns kept.
 *
 * <p>OCR only: Azure gets the PDF bytes and no instructions. The PDF is sent in parts of {@code
 * jbh.statement.azure.pages-per-request} pages, because the free tier (F0) reads only the first 2
 * pages of each request. Active when {@code jbh.statement.text-source=azure}.
 *
 * <p>The PDF and the text are personal data: only HTTP status codes, Azure error codes and counts
 * are logged.
 */
@ApplicationScoped
public class AzureLayoutTextSourceAdapter implements StatementTextSource {

  private static final Logger LOGGER = LoggerFactory.getLogger(AzureLayoutTextSourceAdapter.class);
  private static final String LOG_PREFIX = "[StatementImport:Azure]";

  private static final int HTTP_OK = 200;
  private static final int HTTP_ACCEPTED = 202;
  private static final int HTTP_TOO_MANY_REQUESTS = 429;
  private static final int MAX_THROTTLED_ATTEMPTS = 3;
  private static final long DEFAULT_WAIT_MILLIS = 1000L;
  private static final long MAX_WAIT_SECONDS = 10L;
  private static final long MILLIS_PER_SECOND = 1000L;
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
  private static final String JSON_MIME_TYPE = "application/json";
  private static final String KEY_HEADER = "Ocp-Apim-Subscription-Key";
  private static final String CONTENT_TYPE_HEADER = "Content-Type";
  private static final String OPERATION_LOCATION_HEADER = "Operation-Location";
  private static final String RETRY_AFTER_HEADER = "Retry-After";
  private static final String STATUS_SUCCEEDED = "succeeded";
  private static final String STATUS_FAILED = "failed";
  private static final String PART_SEPARATOR = "\n\n";
  private static final String UNKNOWN = "unknown";
  private static final String SLASH = "/";

  private final StatementImportConfig config;
  private final ObjectMapper objectMapper;
  private final PdfStatementSplitter splitter;
  private final HttpClient httpClient;

  @Inject
  public AzureLayoutTextSourceAdapter(
      final StatementImportConfig config,
      final ObjectMapper objectMapper,
      final PdfStatementSplitter splitter) {
    this.config = config;
    this.objectMapper = objectMapper;
    this.splitter = splitter;
    this.httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
  }

  @Override
  // The cause is dropped on purpose: it may carry parts of the request or the answer.
  @SuppressWarnings("PMD.PreserveStackTrace")
  public String extractText(final byte[] pdfContent) throws BusinessException {
    final String endpoint = required(config.azure().endpoint());
    final String apiKey = required(config.azure().apiKey());

    final List<byte[]> parts = splitter.split(pdfContent, config.azure().pagesPerRequest());
    final StringBuilder text = new StringBuilder();
    try {
      int partNumber = 0;
      for (final byte[] part : parts) {
        partNumber++;
        final long partStarted = System.nanoTime();
        LOGGER.info(
            LOG_PREFIX + " Part {}/{} sent ({} bytes)", partNumber, parts.size(), part.length);
        final URI operation = startAnalysis(endpoint, apiKey, part);
        final String content = waitForContent(operation, apiKey);
        text.append(content).append(PART_SEPARATOR);
        LOGGER.info(
            LOG_PREFIX + " Part {}/{} done in {} ms: {} characters",
            partNumber,
            parts.size(),
            Duration.ofNanos(System.nanoTime() - partStarted).toMillis(),
            content.length());
      }
    } catch (final IOException e) {
      LOGGER.warn(LOG_PREFIX + " Azure call failed: {}", e.getClass().getSimpleName());
      throw readerFailed();
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      throw readerFailed();
    }

    LOGGER.info(
        LOG_PREFIX + " Read the statement in {} parts: {} characters", parts.size(), text.length());
    if (text.toString().isBlank()) {
      throw new BusinessException(FinanceInfraBusinessExceptionType.STATEMENT_PDF_HAS_NO_TEXT);
    }
    return text.toString();
  }

  /** Sends one part. Returns the URL to poll for the result. */
  private URI startAnalysis(final String endpoint, final String apiKey, final byte[] part)
      throws IOException, InterruptedException, BusinessException {
    final String body =
        objectMapper
            .createObjectNode()
            .put("base64Source", Base64.getEncoder().encodeToString(part))
            .toString();
    final HttpRequest request =
        HttpRequest.newBuilder(URI.create(analyzeUrl(endpoint)))
            .timeout(requestTimeout())
            .header(CONTENT_TYPE_HEADER, JSON_MIME_TYPE)
            .header(KEY_HEADER, apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

    final HttpResponse<byte[]> response = sendThrottled(request);
    final Optional<String> operation = response.headers().firstValue(OPERATION_LOCATION_HEADER);
    if (response.statusCode() != HTTP_ACCEPTED || operation.isEmpty()) {
      LOGGER.warn(
          LOG_PREFIX + " Azure returned HTTP {}, error code {}", response.statusCode(), errorCodeOf(response));
      throw readerFailed();
    }
    return URI.create(operation.get());
  }

  /** Polls until the analysis ends. Returns its markdown. */
  private String waitForContent(final URI operation, final String apiKey)
      throws IOException, InterruptedException, BusinessException {
    final HttpRequest request =
        HttpRequest.newBuilder(operation)
            .timeout(requestTimeout())
            .header(KEY_HEADER, apiKey)
            .GET()
            .build();
    final Instant deadline = Instant.now().plus(requestTimeout());

    while (Instant.now().isBefore(deadline)) {
      final HttpResponse<byte[]> response = sendThrottled(request);
      if (response.statusCode() != HTTP_OK) {
        LOGGER.warn(
            LOG_PREFIX + " Azure returned HTTP {}, error code {}", response.statusCode(), errorCodeOf(response));
        throw readerFailed();
      }

      final JsonNode result = objectMapper.readTree(response.body());
      final String status = result.path("status").asText(UNKNOWN);
      if (STATUS_SUCCEEDED.equals(status)) {
        return result.path("analyzeResult").path("content").asText("");
      }
      if (STATUS_FAILED.equals(status)) {
        LOGGER.warn(
            LOG_PREFIX + " Azure analysis failed, error code {}", result.path("error").path("code").asText(UNKNOWN));
        throw readerFailed();
      }
      Thread.sleep(waitMillis(response));
    }

    LOGGER.warn(LOG_PREFIX + " Azure analysis did not end in {} seconds", config.azure().timeoutSeconds());
    throw readerFailed();
  }

  /** Sends a request, and waits and sends it again while Azure answers 429 (free tier limits). */
  private HttpResponse<byte[]> sendThrottled(final HttpRequest request)
      throws IOException, InterruptedException {
    HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    int attempt = 1;
    while (response.statusCode() == HTTP_TOO_MANY_REQUESTS && attempt < MAX_THROTTLED_ATTEMPTS) {
      LOGGER.warn(LOG_PREFIX + " Azure returned HTTP 429, retrying");
      Thread.sleep(waitMillis(response));
      attempt++;
      response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }
    return response;
  }

  private String analyzeUrl(final String endpoint) {
    final String base =
        endpoint.endsWith(SLASH) ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
    return base
        + "/documentintelligence/documentModels/"
        + config.azure().modelId()
        + ":analyze?api-version="
        + config.azure().apiVersion()
        + "&outputContentFormat=markdown";
  }

  private Duration requestTimeout() {
    return Duration.ofSeconds(config.azure().timeoutSeconds());
  }

  /** The {@code Retry-After} seconds, capped, or one second. */
  private static long waitMillis(final HttpResponse<byte[]> response) {
    final Optional<String> header = response.headers().firstValue(RETRY_AFTER_HEADER);
    if (header.isEmpty()) {
      return DEFAULT_WAIT_MILLIS;
    }
    try {
      final long seconds = Long.parseLong(header.get().trim());
      return Math.min(Math.max(seconds, 1L), MAX_WAIT_SECONDS) * MILLIS_PER_SECOND;
    } catch (final NumberFormatException e) {
      return DEFAULT_WAIT_MILLIS;
    }
  }

  /** The Azure error code of an error body, for example {@code InvalidRequest}. */
  private String errorCodeOf(final HttpResponse<byte[]> response) {
    try {
      return objectMapper.readTree(response.body()).path("error").path("code").asText(UNKNOWN);
    } catch (final IOException e) {
      return UNKNOWN;
    }
  }

  private static String required(final Optional<String> value) throws BusinessException {
    return value
        .filter(text -> !text.isBlank())
        .orElseThrow(
            () -> new BusinessException(FinanceInfraBusinessExceptionType.STATEMENT_AI_NOT_CONFIGURED));
  }

  private static BusinessException readerFailed() {
    return new BusinessException(FinanceInfraBusinessExceptionType.STATEMENT_READER_FAILED);
  }
}
