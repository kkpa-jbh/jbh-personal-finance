package com.jbh.finance.infra.adapters.out.ai.llmcompare;

import com.jbh.finance.application.feature.statement.dto.CategorizedMovementDTO;
import com.jbh.finance.application.feature.statement.dto.StatementClassificationDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Temporary: writes the result of the Jev classification to {@code <output-dir>/<time>-jev.txt},
 * one line per movement with the category Jev chose and its confidence.
 *
 * <p>Active only when {@code jbh.statement.compare.enabled} is true. The file holds real bank data.
 */
@ApplicationScoped
public class JevReport {

  private static final Logger LOGGER = LoggerFactory.getLogger(JevReport.class);
  private static final String LOG_PREFIX = "[StatementImport:LlmCompare]";
  private static final String NONE = "-";
  private static final String YES = "yes";
  private static final String NO = "no";
  private static final String LINE = "%-10s  %-6s %14s  %-28s %10s  %-6s  %s%n";

  private final LlmCompareConfig config;

  @Inject
  public JevReport(final LlmCompareConfig config) {
    this.config = config;
  }

  /**
   * Writes the file, when the comparison is on. Never throws: a failure is only logged.
   *
   * @param classification the result of the import
   */
  public void write(final StatementClassificationDTO classification) {
    if (!config.enabled()) {
      return;
    }
    final Path file =
        Path.of(config.outputDir())
            .resolve(LocalDateTime.now().format(LlmCompareReport.FOLDER_NAME) + "-jev.txt");
    try {
      Files.createDirectories(file.getParent());
      Files.writeString(file, content(classification), StandardCharsets.UTF_8);
      LOGGER.info(LOG_PREFIX + " Jev result in {}", file.toAbsolutePath());
    } catch (final IOException e) {
      LOGGER.warn(LOG_PREFIX + " Jev result not written: {}", e.getClass().getSimpleName());
    }
  }

  private static String content(final StatementClassificationDTO classification) {
    final StringBuilder text = new StringBuilder(4096);
    final long needReview =
        classification.movements().stream().filter(CategorizedMovementDTO::lowConfidence).count();
    text.append("Totals check: ")
        .append(classification.totalsCheck().status())
        .append("\nMovements:    ")
        .append(classification.movements().size())
        .append(", need review: ")
        .append(needReview)
        .append("\n\n")
        .append(
            String.format(
                LINE, "DATE", "DIR", "AMOUNT", "CATEGORY", "CONFIDENCE", "REVIEW", "DESCRIPTION"));
    for (final CategorizedMovementDTO movement : classification.movements()) {
      text.append(
          String.format(
              LINE,
              movement.date(),
              movement.direction(),
              movement.amount().toPlainString(),
              movement.categoryAlias(),
              plain(movement.confidence()),
              movement.lowConfidence() ? YES : NO,
              movement.description()));
    }
    return text.toString();
  }

  private static String plain(final BigDecimal value) {
    return value == null ? NONE : value.toPlainString();
  }
}
