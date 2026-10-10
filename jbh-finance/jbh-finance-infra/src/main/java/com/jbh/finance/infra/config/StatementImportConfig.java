package com.jbh.finance.infra.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Settings of the bank statement import: a text source (Azure OCR or PDFBox) turns the PDF into
 * text, an LLM (OpenRouter or Gemini) finds the movements, Jev classifies each movement.
 *
 * <p>An empty API key env var makes {@code apiKey()} empty, so the service still starts without
 * keys. The import endpoint then answers with a clear error.
 */
@ConfigMapping(prefix = "jbh.statement")
public interface StatementImportConfig {

  @WithName("max-file-size-bytes")
  long maxFileSizeBytes();

  /** Step 1: what turns the PDF into text. */
  @WithName("text-source")
  TextSource textSource();

  /** Step 2: which LLM finds the movements in the text. */
  Llm llm();

  Azure azure();

  Gemini gemini();

  @WithName("openrouter")
  OpenRouter openRouter();

  Jev jev();

  /** The {@code StatementTextSource} implementations. One is active at a time. */
  enum TextSource {
    AZURE,
    PDFBOX
  }

  /** The {@code StatementLlm} implementations. One is active at a time. */
  enum Llm {
    GEMINI,
    OPENROUTER
  }

  interface Azure {
    /** The resource endpoint, for example https://my-resource.cognitiveservices.azure.com. */
    Optional<String> endpoint();

    @WithName("api-key")
    Optional<String> apiKey();

    @WithName("model-id")
    String modelId();

    @WithName("api-version")
    String apiVersion();

    /** Pages sent in each request; zero or less sends the whole PDF. The free tier reads 2. */
    @WithName("pages-per-request")
    int pagesPerRequest();

    /** Maximum wait for one request, polling included. */
    @WithName("timeout-seconds")
    int timeoutSeconds();
  }

  interface Gemini {
    @WithName("api-key")
    Optional<String> apiKey();

    @WithName("base-url")
    String baseUrl();

    String model();

    @WithName("timeout-seconds")
    int timeoutSeconds();
  }

  interface OpenRouter {
    @WithName("api-key")
    Optional<String> apiKey();

    @WithName("base-url")
    String baseUrl();

    /** Tried in order; the next model answers when one is busy or fails. */
    List<String> models();

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
