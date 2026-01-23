package com.jbh.account.application.core.usecases.integration.monthlybalance;

import static org.junit.jupiter.api.Assertions.*;

import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.application.core.ports.output.monthlybalance.InMemoryMonthlyBalanceRepositories;
import com.jbh.account.application.core.usecases.utils.MonthlyBalanceITUtils;
import com.jbh.account.domain.vo.ProductId;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MonthlyBalanceInMemoryRepositoryTest {

  private InMemoryMonthlyBalanceRepositories repositories;
  private ProductId testAccountId;

  @BeforeEach
  void setUp() {
    repositories = new InMemoryMonthlyBalanceRepositories();
    testAccountId = ProductId.generate();
  }

  @Test
  void shouldSaveAndRetrieveMonthlyBalance() {
    // Given
    final MonthlyBalanceDTO balance =
        MonthlyBalanceDTO.defaultBuilder()
            .accountId(testAccountId)
            .year(2024)
            .month(3)
            .period(YearMonth.of(2024, 3))
            .openingBalance(BigDecimal.valueOf(1000))
            .closingBalance(BigDecimal.valueOf(1200))
            .monthlyNetProfit(BigDecimal.valueOf(200))
            .totalMovements(5)
            .build();

    // When
    repositories.getWriterRepo().saveBalance(balance);

    // Then
    final Optional<MonthlyBalanceDTO> retrieved =
        repositories.getQueryRepo().findByAccountIdYearAndMonth(testAccountId, 2024, 3);

    assertTrue(retrieved.isPresent());
    MonthlyBalanceITUtils.assertMonthlyBalance(balance, retrieved.get());
  }

  @Test
  void shouldSaveMultipleBalancesAndQueryByPeriodRange() {
    // Given
    final List<MonthlyBalanceDTO> balances =
        List.of(
            createBalance(testAccountId, 2024, 1, BigDecimal.valueOf(1000)),
            createBalance(testAccountId, 2024, 2, BigDecimal.valueOf(1100)),
            createBalance(testAccountId, 2024, 3, BigDecimal.valueOf(1200)),
            createBalance(testAccountId, 2024, 4, BigDecimal.valueOf(1300)));

    // When
    repositories.getWriterRepo().saveMultiBalances(balances);

    // Then
    final List<MonthlyBalanceDTO> retrieved =
        repositories
            .getQueryRepo()
            .findNextBalancesFromPeriodInclusive(testAccountId, YearMonth.of(2024, 2));

    assertEquals(3, retrieved.size());
    assertEquals(YearMonth.of(2024, 2), retrieved.get(0).period());
    assertEquals(YearMonth.of(2024, 3), retrieved.get(1).period());
    assertEquals(YearMonth.of(2024, 4), retrieved.get(2).period());
  }

  private MonthlyBalanceDTO createBalance(
      final ProductId accountId, final int year, final int month, final BigDecimal closingBalance) {
    return MonthlyBalanceDTO.defaultBuilder()
        .accountId(accountId)
        .year(year)
        .month(month)
        .period(YearMonth.of(year, month))
        .openingBalance(BigDecimal.ZERO)
        .closingBalance(closingBalance)
        .monthlyNetProfit(BigDecimal.ZERO)
        .totalMovements(0)
        .build();
  }

  @Test
  void shouldHandleMultipleAccountsSeparately() {
    // Given
    final ProductId accountId1 = ProductId.generate();
    final ProductId accountId2 = ProductId.generate();

    repositories
        .getWriterRepo()
        .saveBalance(createBalance(accountId1, 2024, 3, BigDecimal.valueOf(1000)));
    repositories
        .getWriterRepo()
        .saveBalance(createBalance(accountId2, 2024, 3, BigDecimal.valueOf(2000)));

    // When & Then
    final Optional<MonthlyBalanceDTO> account1Balance =
        repositories.getQueryRepo().findByAccountIdYearAndMonth(accountId1, 2024, 3);
    final Optional<MonthlyBalanceDTO> account2Balance =
        repositories.getQueryRepo().findByAccountIdYearAndMonth(accountId2, 2024, 3);

    assertTrue(account1Balance.isPresent());
    assertTrue(account2Balance.isPresent());
    assertEquals(BigDecimal.valueOf(1000), account1Balance.get().closingBalance());
    assertEquals(BigDecimal.valueOf(2000), account2Balance.get().closingBalance());
  }

  @Test
  void shouldReturnEmptyWhenBalanceNotFound() {
    // When
    final Optional<MonthlyBalanceDTO> result =
        repositories.getQueryRepo().findByAccountIdYearAndMonth(testAccountId, 2024, 12);

    // Then
    assertFalse(result.isPresent());
  }

  @Test
  void shouldClearAllData() {
    // Given
    repositories
        .getWriterRepo()
        .saveBalance(createBalance(testAccountId, 2024, 3, BigDecimal.valueOf(1000)));
    assertEquals(1, repositories.getQueryRepo().size());

    // When
    repositories.clearStorage();

    // Then
    assertEquals(0, repositories.getQueryRepo().size());
    final Optional<MonthlyBalanceDTO> result =
        repositories.getQueryRepo().findByAccountIdYearAndMonth(testAccountId, 2024, 3);
    assertFalse(result.isPresent());
  }
}
