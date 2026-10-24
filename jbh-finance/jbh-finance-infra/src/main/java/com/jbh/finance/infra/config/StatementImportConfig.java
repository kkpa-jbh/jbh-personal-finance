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
 *
 * <p>Property names follow the default kebab-case mapping ({@code apiKey()} reads {@code
 * api-key}). {@code @WithName} is only for a name that differs.
 */
@ConfigMapping(prefix = "jbh.statement")
public interface StatementImportConfig {

  long maxFileSizeBytes();

  /** Step 1: what turns the PDF into text. */
  TextSource textSource();

  /** Step 2: which LLM finds the movements in the text. */
  Llm llm();

  Azure azure();

  Gemini gemini();

  @WithName("openrouter")
  OpenRouter openRouter();

  JevApi jev();

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

    Optional<String> apiKey();

    String modelId();

    String apiVersion();

    /** Pages sent in each request; zero or less sends the whole PDF. The free tier reads 2. */
    int pagesPerRequest();

    /** Maximum wait for one request, polling included. */
    int timeoutSeconds();
  }

  interface Gemini {
    Optional<String> apiKey();

    String baseUrl();

    String model();

    int timeoutSeconds();
  }

  interface OpenRouter {
    Optional<String> apiKey();

    String baseUrl();

    /** Tried in order; the next model answers when one is busy or fails. */
    List<String> models();

    int timeoutSeconds();
  }

  interface JevApi {
    Optional<String> apiKey();

    String baseUrl();

    String model();

    int batchSize();

    BigDecimal minConfidence();

    int timeoutSeconds();
  }
}
