package com.jbh.finance.test.application.feature.statement.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jbh.finance.application.feature.statement.dto.ExtractedMovementDTO;
import com.jbh.finance.application.feature.statement.dto.ExtractedStatementDTO;
import com.jbh.finance.application.feature.statement.dto.StatementBalanceDTO;
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
  private static final String COP = "COP";
  private static final String USD = "USD";

  private final StatementTotalsChecker checker = new StatementTotalsChecker();

  @Test
  void shouldMatchWhenMovementsEqualBalanceChange() {
    // Arrange
    final ExtractedStatementDTO statement =
        statement(new BigDecimal("1000"), new BigDecimal("1300"));

    // Act
    final TotalsCheckDTO result = onlyCheck(statement);

    // Assert
    assertEquals(COP, result.currency());
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
                movement("50", COP, StatementMovementDirection.CREDIT),
                movement("250", COP, StatementMovementDirection.DEBIT)),
            List.of(new StatementBalanceDTO(COP, new BigDecimal("500"), new BigDecimal("700"))));

    // Act
    final TotalsCheckDTO result = onlyCheck(statement);

    // Assert
    assertEquals(TotalsCheckStatus.MATCH, result.status());
  }

  @Test
  void shouldMismatchWhenMovementsDoNotAddUp() {
    // Arrange
    final ExtractedStatementDTO statement =
        statement(new BigDecimal("1000"), new BigDecimal("1250"));

    // Act
    final TotalsCheckDTO result = onlyCheck(statement);

    // Assert
    assertEquals(TotalsCheckStatus.MISMATCH, result.status());
  }

  @Test
  void shouldBeNotAvailableWhenOpeningIsNull() {
    // Arrange
    final ExtractedStatementDTO statement = statement(null, new BigDecimal("1300"));

    // Act
    final TotalsCheckDTO result = onlyCheck(statement);

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
    final TotalsCheckDTO result = onlyCheck(statement);

    // Assert
    assertEquals(TotalsCheckStatus.NOT_AVAILABLE, result.status());
    assertNull(result.expectedChange());
    assertEquals(0, new BigDecimal("300").compareTo(result.movementsChange()));
  }

  @Test
  void shouldBeNotAvailableWhenCurrencyHasNoBalances() {
    // Arrange
    final ExtractedStatementDTO statement =
        new ExtractedStatementDTO(
            List.of(movement("100", COP, StatementMovementDirection.DEBIT)), List.of());

    // Act
    final TotalsCheckDTO result = onlyCheck(statement);

    // Assert
    assertEquals(COP, result.currency());
    assertEquals(TotalsCheckStatus.NOT_AVAILABLE, result.status());
    assertEquals(0, new BigDecimal("-100").compareTo(result.movementsChange()));
  }

  @Test
  void shouldMatchWhenScaleDiffers() {
    // Arrange
    final ExtractedStatementDTO statement =
        new ExtractedStatementDTO(
            List.of(movement("100", COP, StatementMovementDirection.CREDIT)),
            List.of(new StatementBalanceDTO(COP, new BigDecimal("0.00"), new BigDecimal("100.00"))));

    // Act
    final TotalsCheckDTO result = onlyCheck(statement);

    // Assert
    assertEquals(TotalsCheckStatus.MATCH, result.status());
  }

  @Test
  void shouldCheckEachCurrencyApartWhenStatementHasTwoCurrencies() {
    // Arrange: the USD movement must not change the COP check.
    final ExtractedStatementDTO statement =
        new ExtractedStatementDTO(
            List.of(
                movement("300", COP, StatementMovementDirection.DEBIT),
                movement("7", USD, StatementMovementDirection.DEBIT)),
            List.of(
                new StatementBalanceDTO(COP, new BigDecimal("1000"), new BigDecimal("1300")),
                new StatementBalanceDTO(USD, new BigDecimal("10"), null)));

    // Act
    final List<TotalsCheckDTO> result = checker.check(statement);

    // Assert
    assertEquals(2, result.size());
    assertEquals(COP, result.get(0).currency());
    assertEquals(TotalsCheckStatus.MATCH, result.get(0).status());
    assertEquals(USD, result.get(1).currency());
    assertEquals(TotalsCheckStatus.NOT_AVAILABLE, result.get(1).status());
    assertEquals(0, new BigDecimal("-7").compareTo(result.get(1).movementsChange()));
  }

  @Test
  void shouldAddCheckWhenBalanceCurrencyHasNoMovements() {
    // Arrange
    final ExtractedStatementDTO statement =
        new ExtractedStatementDTO(
            List.of(movement("300", COP, StatementMovementDirection.DEBIT)),
            List.of(new StatementBalanceDTO(USD, new BigDecimal("10"), new BigDecimal("10"))));

    // Act
    final List<TotalsCheckDTO> result = checker.check(statement);

    // Assert
    assertEquals(2, result.size());
    assertEquals(USD, result.get(1).currency());
    assertEquals(TotalsCheckStatus.MATCH, result.get(1).status());
  }

  @Test
  void shouldReturnNoChecksWhenStatementIsEmpty() {
    // Arrange
    final ExtractedStatementDTO statement = new ExtractedStatementDTO(List.of(), List.of());

    // Act
    final List<TotalsCheckDTO> result = checker.check(statement);

    // Assert
    assertTrue(result.isEmpty());
  }

  private TotalsCheckDTO onlyCheck(final ExtractedStatementDTO statement) {
    final List<TotalsCheckDTO> checks = checker.check(statement);
    assertEquals(1, checks.size());
    return checks.get(0);
  }

  /** COP only. Credits 500, debits 200: the movements change is +300. */
  private static ExtractedStatementDTO statement(
      final BigDecimal opening, final BigDecimal closing) {
    return new ExtractedStatementDTO(
        List.of(
            movement("500", COP, StatementMovementDirection.CREDIT),
            movement("200", COP, StatementMovementDirection.DEBIT)),
        List.of(new StatementBalanceDTO(COP, opening, closing)));
  }

  private static ExtractedMovementDTO movement(
      final String amount, final String currency, final StatementMovementDirection direction) {
    return new ExtractedMovementDTO(DATE, "PAGO PSE", new BigDecimal(amount), currency, direction);
  }
}
