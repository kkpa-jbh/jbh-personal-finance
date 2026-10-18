package com.jbh.finance.infra.adapters.out.ai.llmcompare;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;
import java.util.Optional;

/**
 * Settings of the temporary LLM comparison. Off by default: the statement import then works
 * exactly as before.
 *
 * <p>Temporary: delete this package, its {@code jbh.statement.compare.*} properties and the two
 * {@code wrap} calls in {@code StatementUseCasesCDIConfig} when the comparison is done.
 */
@ConfigMapping(prefix = "jbh.statement.compare")
public interface LlmCompareConfig {

  /**
   * When true, every import also runs the other providers in the background, and the OCR text is
   * cached per PDF ({@link OcrCache}).
   */
  boolean enabled();

  /** The folder for the result files. Relative paths start at the working folder of the app. */
  @WithName("output-dir")
  String outputDir();

  Ollama ollama();

  interface Ollama {
    @WithName("api-key")
    Optional<String> apiKey();

    @WithName("base-url")
    String baseUrl();

    String model();

    @WithName("timeout-seconds")
    int timeoutSeconds();
  }
}
