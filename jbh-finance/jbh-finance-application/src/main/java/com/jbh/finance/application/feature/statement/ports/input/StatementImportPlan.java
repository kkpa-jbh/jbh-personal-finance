package com.jbh.finance.application.feature.statement.ports.input;

import com.jbh.finance.application.feature.movement.commands.AddMovementUploadedFileCommand;
import com.jbh.finance.application.feature.statement.dto.CategorizedMovementDTO;
import com.jbh.finance.application.feature.statement.dto.SkippedStatementMovementDTO;
import com.jbh.finance.application.feature.statement.dto.SkippedStatementMovementDTO.SkipReason;
import com.jbh.finance.application.feature.statement.dto.StatementClassificationDTO;
import com.jbh.finance.application.feature.statement.dto.StatementImportSummaryDTO;
import com.jbh.finance.application.feature.statement.dto.StatementMovementDirection;
import com.jbh.finance.application.feature.statement.services.StatementMainCurrencyPolicy;
import com.jbh.finance.domain.movement.vo.MovementReviewVO;
import com.jbh.finance.domain.movement.vo.MovementType;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The rows of a classified statement split into rows to save and rows reported as not saved, for
 * the main currency. It also builds the summary and the upload commands.
 *
 * @param classification the extracted and classified statement
 * @param mainCurrency the currency whose rows are saved
 * @param toSave rows to save, sorted by date (stable)
 * @param notSaved rows left out, in PDF order
 */
record StatementImportPlan(
    StatementClassificationDTO classification,
    String mainCurrency,
    List<CategorizedMovementDTO> toSave,
    List<SkippedStatementMovementDTO> notSaved) {

  private static final StatementMainCurrencyPolicy CURRENCY_POLICY =
      new StatementMainCurrencyPolicy();

  static StatementImportPlan of(
      final StatementClassificationDTO classification, final Clock clock) {
    final List<CategorizedMovementDTO> movements = classification.movements();
    final String mainCurrency = CURRENCY_POLICY.pick(movements, classification.balances());
    final LocalDate today = LocalDate.now(clock);

    final List<CategorizedMovementDTO> toSave =
        movements.stream()
            .filter(row -> skipReasonOf(row, mainCurrency, today).isEmpty())
            .sorted(Comparator.comparing(CategorizedMovementDTO::date))
            .toList();
    final List<SkippedStatementMovementDTO> notSaved =
        movements.stream()
            .flatMap(
                row -> skipReasonOf(row, mainCurrency, today).map(r -> skipped(row, r)).stream())
            .toList();
    return new StatementImportPlan(classification, mainCurrency, toSave, notSaved);
  }

  StatementImportSummaryDTO summary() {
    final int needsReview =
        (int) toSave.stream().filter(CategorizedMovementDTO::lowConfidence).count();
    return new StatementImportSummaryDTO(
        mainCurrency,
        toSave.size(),
        notSaved.size(),
        needsReview,
        classification.totalsChecks(),
        notSaved);
  }

  List<AddMovementUploadedFileCommand> commands(final UUID importId) {
    return toSave.stream().map(row -> toCommand(row, importId)).toList();
  }

  /** The currency check runs first, so a foreign row dated in the future is OTHER_CURRENCY. */
  private static Optional<SkipReason> skipReasonOf(
      final CategorizedMovementDTO row, final String mainCurrency, final LocalDate today) {
    if (!mainCurrency.equals(row.currency())) {
      return Optional.of(SkipReason.OTHER_CURRENCY);
    }
    if (row.date().isAfter(today)) {
      return Optional.of(SkipReason.FUTURE_DATE);
    }
    return Optional.empty();
  }

  private static SkippedStatementMovementDTO skipped(
      final CategorizedMovementDTO row, final SkipReason reason) {
    return new SkippedStatementMovementDTO(
        row.date(),
        row.description(),
        row.amount(),
        row.currency(),
        row.direction(),
        reason,
        row.categoryAlias(),
        row.categoryDisplayName(),
        row.confidence(),
        row.lowConfidence());
  }

  /** The domain wants withdrawals negative, so a DEBIT amount changes sign. */
  private static AddMovementUploadedFileCommand toCommand(
      final CategorizedMovementDTO row, final UUID importId) {
    final boolean credit = row.direction() == StatementMovementDirection.CREDIT;
    return new AddMovementUploadedFileCommand(
        row.date(),
        credit ? MovementType.DEPOSIT : MovementType.WITHDRAWAL,
        credit ? row.amount() : row.amount().negate(),
        null,
        row.category(),
        row.description(),
        importId,
        new MovementReviewVO(row.confidence(), row.lowConfidence()));
  }
}
