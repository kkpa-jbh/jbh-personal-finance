package com.jbh.finance.infra.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Settings of the bank statement import: Gemini reads the PDF, Jev classifies each movement.
 *
 * <p>An empty API key env var makes {@code apiKey()} empty, so the service still starts without
 * keys. The import endpoint then answers with a clear error.
 */
@ConfigMapping(prefix = "jbh.statement")
public interface StatementImportConfig {

  @WithName("max-file-size-bytes")
  long maxFileSizeBytes();

  Gemini gemini();

  Jev jev();

  interface Gemini {
    @WithName("api-key")
    Optional<String> apiKey();

    @WithName("base-url")
    String baseUrl();

    String model();

    @WithName("timeout-seconds")
    int timeoutSeconds();
  }

  interface Jev {
    @WithName("api-key")
    Optional<String> apiKey();

    @WithName("base-url")
    String baseUrl();

    String model();

    @WithName("batch-size")
    int batchSize();

    @WithName("min-confidence")
    BigDecimal minConfidence();

    @WithName("timeout-seconds")
    int timeoutSeconds();
  }
}
