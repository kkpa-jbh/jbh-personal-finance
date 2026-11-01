package com.jbh.finance.test.application.feature.statement.services;

import static com.jbh.finance.test.testfixtures.builders.StatementFixtures.COP;
import static com.jbh.finance.test.testfixtures.builders.StatementFixtures.USD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jbh.finance.application.feature.category.dto.CategoryDTO;
import com.jbh.finance.application.feature.statement.dto.CategorizedMovementDTO;
import com.jbh.finance.application.feature.statement.dto.StatementBalanceDTO;
import com.jbh.finance.application.feature.statement.dto.StatementMovementDirection;
import com.jbh.finance.application.feature.statement.services.StatementMainCurrencyPolicy;
import com.jbh.finance.domain.category.vo.CategorySourceVO;
import com.jbh.finance.test.testfixtures.builders.StatementFixtures;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class StatementMainCurrencyPolicyTest {

  private static final String EUR = "EUR";
  private static final CategoryDTO UNKNOWN =
      StatementFixtures.category(CategorySourceVO.EXPENSE, "UNK", 1L, "Unknown");

  private final StatementMainCurrencyPolicy policy = new StatementMainCurrencyPolicy();

  @Test
  void shouldPickCurrencyWithMostRowsWhenNoTie() {
    // Arrange
    final List<CategorizedMovementDTO> rows = rows(COP, COP, COP, USD);

    // Act & Assert
    assertEquals(COP, policy.pick(rows, List.of()));
  }

  @Test
  void shouldPickFirstBalanceCurrencyWhenTieAndUsdSectionIsFirst() {
    // Arrange
    final List<CategorizedMovementDTO> rows = rows(COP, COP, USD, USD);

    // Act & Assert
    assertEquals(USD, policy.pick(rows, List.of(balance(USD), balance(COP))));
  }

  @Test
  void shouldPickFirstBalanceCurrencyWhenTieAndCopSectionIsFirst() {
    // Arrange
    final List<CategorizedMovementDTO> rows = rows(COP, COP, USD, USD);

    // Act & Assert
    assertEquals(COP, policy.pick(rows, List.of(balance(COP), balance(USD))));
  }

  @Test
  void shouldPickFirstSeenCurrencyWhenTieAndNoBalances() {
    // Arrange
    final List<CategorizedMovementDTO> rows = rows(USD, COP, COP, USD);

    // Act & Assert
    assertEquals(USD, policy.pick(rows, List.of()));
  }

  @Test
  void shouldPickFirstBalanceAmongTiedCurrenciesWhenBalanceListHasOtherCurrencies() {
    // Arrange
    final List<CategorizedMovementDTO> rows = rows(COP, USD);

    // Act & Assert
    assertEquals(USD, policy.pick(rows, List.of(balance(EUR), balance(USD))));
  }

  @Test
  void shouldThrowWhenThereAreNoMovements() {
    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> policy.pick(List.of(), List.of()));
  }

  private static StatementBalanceDTO balance(final String currency) {
    return new StatementBalanceDTO(currency, null, null);
  }

  private static List<CategorizedMovementDTO> rows(final String... currencies) {
    final List<CategorizedMovementDTO> rows = new ArrayList<>();
    for (final String currency : currencies) {
      rows.add(
          StatementFixtures.row(
              LocalDate.of(2026, 9, 1),
              "ROW",
              StatementMovementDirection.DEBIT,
              BigDecimal.TEN.toPlainString(),
              currency,
              UNKNOWN));
    }
    return rows;
  }
}
