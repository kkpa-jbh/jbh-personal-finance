package com.jbh.finance.test.application.feature.statement.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.jbh.finance.application.feature.statement.dto.ExtractedMovementDTO;
import com.jbh.finance.application.feature.statement.dto.ExtractedStatementDTO;
import com.jbh.finance.application.feature.statement.dto.StatementMovementDirection;
import com.jbh.finance.application.feature.statement.dto.TotalsCheckDTO;
import com.jbh.finance.application.feature.statement.dto.TotalsCheckStatus;
import com.jbh.finance.application.feature.statement.services.StatementTotalsChecker;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class StatementTotalsCheckerTest {

  private static final LocalDate DATE = LocalDate.of(2026, 9, 1);

  private final StatementTotalsChecker checker = new StatementTotalsChecker();

  @Test
  void shouldMatchWhenMovementsEqualBalanceChange() {
    // Arrange
    final ExtractedStatementDTO statement =
        statement(new BigDecimal("1000"), new BigDecimal("1300"));

    // Act
    final TotalsCheckDTO result = checker.check(statement);

    // Assert
    assertEquals(TotalsCheckStatus.MATCH, result.status());
    assertEquals(0, new BigDecimal("300").compareTo(result.expectedChange()));
    assertEquals(0, new BigDecimal("300").compareTo(result.movementsChange()));
  }

  @Test
  void shouldMatchWhenSignIsInvertedOnCreditCard() {
    // Arrange: a credit card debt grows when there are more purchases than payments.
    final ExtractedStatementDTO statement =
        new ExtractedStatementDTO(
            List.of(
                movement("50", StatementMovementDirection.CREDIT),
                movement("250", StatementMovementDirection.DEBIT)),
            new BigDecimal("500"),
            new BigDecimal("700"));

    // Act
    final TotalsCheckDTO result = checker.check(statement);

    // Assert
    assertEquals(TotalsCheckStatus.MATCH, result.status());
  }

  @Test
  void shouldMismatchWhenMovementsDoNotAddUp() {
    // Arrange
    final ExtractedStatementDTO statement =
        statement(new BigDecimal("1000"), new BigDecimal("1250"));

    // Act
    final TotalsCheckDTO result = checker.check(statement);

    // Assert
    assertEquals(TotalsCheckStatus.MISMATCH, result.status());
  }

  @Test
  void shouldBeNotAvailableWhenOpeningIsNull() {
    // Arrange
    final ExtractedStatementDTO statement = statement(null, new BigDecimal("1300"));

    // Act
    final TotalsCheckDTO result = checker.check(statement);

    // Assert
    assertEquals(TotalsCheckStatus.NOT_AVAILABLE, result.status());
    assertNull(result.expectedChange());
    assertEquals(0, new BigDecimal("300").compareTo(result.movementsChange()));
  }

  @Test
  void shouldBeNotAvailableWhenClosingIsNull() {
    // Arrange
    final ExtractedStatementDTO statement = statement(new BigDecimal("1000"), null);

    // Act
    final TotalsCheckDTO result = checker.check(statement);

    // Assert
    assertEquals(TotalsCheckStatus.NOT_AVAILABLE, result.status());
    assertNull(result.expectedChange());
    assertEquals(0, new BigDecimal("300").compareTo(result.movementsChange()));
  }

  @Test
  void shouldMatchWhenScaleDiffers() {
    // Arrange
    final ExtractedStatementDTO statement =
        new ExtractedStatementDTO(
            List.of(movement("100", StatementMovementDirection.CREDIT)),
            new BigDecimal("0.00"),
            new BigDecimal("100.00"));

    // Act
    final TotalsCheckDTO result = checker.check(statement);

    // Assert
    assertEquals(TotalsCheckStatus.MATCH, result.status());
  }

  /** Credits 500, debits 200: the movements change is +300. */
  private static ExtractedStatementDTO statement(
      final BigDecimal opening, final BigDecimal closing) {
    return new ExtractedStatementDTO(
        List.of(
            movement("500", StatementMovementDirection.CREDIT),
            movement("200", StatementMovementDirection.DEBIT)),
        opening,
        closing);
  }

  private static ExtractedMovementDTO movement(
      final String amount, final StatementMovementDirection direction) {
    return new ExtractedMovementDTO(DATE, "PAGO PSE", new BigDecimal(amount), direction);
  }
}
