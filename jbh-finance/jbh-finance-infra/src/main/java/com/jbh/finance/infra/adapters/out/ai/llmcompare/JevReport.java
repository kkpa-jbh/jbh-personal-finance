package com.jbh.finance.infra.adapters.out.ai.llmcompare;

import com.jbh.finance.application.feature.statement.dto.CategorizedMovementDTO;
import com.jbh.finance.application.feature.statement.dto.StatementClassificationDTO;
import com.jbh.finance.application.feature.statement.dto.TotalsCheckDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Temporary: writes the result of the Jev classification to {@code <output-dir>/<time>-jev.txt}:
 * one line per movement with the category Jev chose and its confidence, then the request and the
 * raw response of each Jev batch (from {@link JevTrace}).
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
  private static final String LINE = "%-10s  %-6s %14s  %-3s  %-28s %10s  %-6s  %s%n";

  private final LlmCompareConfig config;
  private final JevTrace trace;

  @Inject
  public JevReport(final LlmCompareConfig config, final JevTrace trace) {
    this.config = config;
    this.trace = trace;
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
      Files.writeString(
          file, content(classification) + batches(trace.batches()), StandardCharsets.UTF_8);
      LOGGER.info(LOG_PREFIX + " Jev result in {}", file.toAbsolutePath());
    } catch (final IOException e) {
      LOGGER.warn(LOG_PREFIX + " Jev result not written: {}", e.getClass().getSimpleName());
    }
  }

  private static String content(final StatementClassificationDTO classification) {
    final StringBuilder text = new StringBuilder(4096);
    final long needReview =
        classification.movements().stream().filter(CategorizedMovementDTO::lowConfidence).count();
    for (final TotalsCheckDTO check : classification.totalsChecks()) {
      text.append("Totals check: ")
          .append(check.currency())
          .append(' ')
          .append(check.status())
          .append(" (expected change ")
          .append(plain(check.expectedChange()))
          .append(", movements change ")
          .append(plain(check.movementsChange()))
          .append(")\n");
    }
    text.append("Movements:    ")
        .append(classification.movements().size())
        .append(", need review: ")
        .append(needReview)
        .append("\n\n")
        .append(
            String.format(
                LINE,
                "DATE",
                "DIR",
                "AMOUNT",
                "CUR",
                "CATEGORY",
                "CONFIDENCE",
                "REVIEW",
                "DESCRIPTION"));
    for (final CategorizedMovementDTO movement : classification.movements()) {
      text.append(
          String.format(
              LINE,
              movement.date(),
              movement.direction(),
              movement.amount().toPlainString(),
              movement.currency(),
              movement.categoryAlias(),
              plain(movement.confidence()),
              movement.lowConfidence() ? YES : NO,
              movement.description()));
    }
    return text.toString();
  }

  private static String batches(final List<JevTrace.Batch> batches) {
    final StringBuilder text = new StringBuilder(8192);
    for (int i = 0; i < batches.size(); i++) {
      final int number = i + 1;
      text.append("\n\n===== JEV REQUEST, batch ")
          .append(number)
          .append('/')
          .append(batches.size())
          .append(" =====\n")
          .append(batches.get(i).request())
          .append("\n\n===== JEV RESPONSE, batch ")
          .append(number)
          .append('/')
          .append(batches.size())
          .append(" =====\n")
          .append(batches.get(i).response());
    }
    return text.toString();
  }

  private static String plain(final BigDecimal value) {
    return value == null ? NONE : value.toPlainString();
  }
}
