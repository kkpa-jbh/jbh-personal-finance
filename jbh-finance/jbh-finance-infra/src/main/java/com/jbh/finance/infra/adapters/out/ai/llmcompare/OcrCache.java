package com.jbh.finance.infra.adapters.out.ai.llmcompare;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.infra.adapters.out.ai.StatementTextSource;
import com.jbh.finance.infra.config.StatementImportConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Temporary: caches the OCR text of each PDF, so testing LLMs on the same PDF does not call Azure
 * (time and free-tier pages) on every import.
 *
 * <p>The cache file is {@code <output-dir>/ocr-cache/<sha-256>-<TEXT_SOURCE>.txt}, keyed by the
 * SHA-256 of the unlocked PDF and the text source (for example {@code AZURE}). Active only when
 * {@code jbh.statement.compare.enabled} is true. Delete the folder after a change to the text
 * source settings (for example {@code azure.pages-per-request}) to read fresh text. The files hold
 * real bank data.
 */
@ApplicationScoped
public class OcrCache {

  private static final Logger LOGGER = LoggerFactory.getLogger(OcrCache.class);
  private static final String LOG_PREFIX = "[StatementImport:OcrCache]";
  private static final String CACHE_FOLDER = "ocr-cache";

  private final LlmCompareConfig compareConfig;
  private final StatementImportConfig importConfig;

  @Inject
  public OcrCache(final LlmCompareConfig compareConfig, final StatementImportConfig importConfig) {
    this.compareConfig = compareConfig;
    this.importConfig = importConfig;
  }

  /**
   * Wraps the active text source with the cache.
   *
   * @param active the text source chosen by {@code jbh.statement.text-source}
   * @return {@code active} itself when the comparison is off, else a caching wrapper around it
   */
  public StatementTextSource wrap(final StatementTextSource active) {
    if (!compareConfig.enabled()) {
      return active;
    }
    LOGGER.info(LOG_PREFIX + " OCR cache is on. Files go to {}", folder());
    return pdfContent -> cachedText(active, pdfContent);
  }

  private String cachedText(final StatementTextSource active, final byte[] pdfContent)
      throws BusinessException {
    final Path file =
        folder().resolve(sha256(pdfContent) + "-" + importConfig.textSource().name() + ".txt");
    if (Files.exists(file)) {
      try {
        final String text = Files.readString(file, StandardCharsets.UTF_8);
        LOGGER.info(
            LOG_PREFIX + " Cache hit, the text source is not called: {} characters",
            text.length());
        return text;
      } catch (final IOException e) {
        LOGGER.warn(LOG_PREFIX + " Cache file not readable: {}", e.getClass().getSimpleName());
      }
    }

    final String text = active.extractText(pdfContent);
    try {
      Files.createDirectories(file.getParent());
      Files.writeString(file, text, StandardCharsets.UTF_8);
      LOGGER.info(LOG_PREFIX + " Cache miss, text saved in {}", file.toAbsolutePath());
    } catch (final IOException e) {
      LOGGER.warn(LOG_PREFIX + " Text not saved: {}", e.getClass().getSimpleName());
    }
    return text;
  }

  private Path folder() {
    return Path.of(compareConfig.outputDir()).resolve(CACHE_FOLDER);
  }

  private static String sha256(final byte[] content) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    } catch (final NoSuchAlgorithmException e) {
      // Every Java runtime must support SHA-256.
      throw new IllegalStateException("SHA-256 is not available", e);
    }
  }
}
