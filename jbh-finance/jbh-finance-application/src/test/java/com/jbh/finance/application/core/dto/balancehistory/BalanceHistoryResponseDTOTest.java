package com.jbh.finance.application.core.dto.balancehistory;

import static com.jbh.commons.util.JbhMoneyUtils.JBH_ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jbh.finance.application.feature.monthlybalance.dto.balancehistory.BalanceHistoryDTO;
import com.jbh.finance.application.feature.monthlybalance.dto.balancehistory.BalanceHistoryEntryDTO;
import com.jbh.finance.application.feature.monthlybalance.dto.balancehistory.BalanceHistorySummaryDTO;
import com.jbh.finance.domain.product.vo.ProductId;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class BalanceHistoryResponseDTOTest {

  @Test
  void shouldCreateResponseWithAllFields() {
    final BalanceHistorySummaryDTO summary =
        new BalanceHistorySummaryDTO(
            new BigDecimal("10000.00"),
            new BigDecimal("1000.00"),
            new BigDecimal("10.00"),
            new BigDecimal("2.50"),
            25);

    final BalanceHistoryEntryDTO balance1 =
        new BalanceHistoryEntryDTO(
            YearMonth.of(2025, 1),
            new BigDecimal("9000.00"),
            new BigDecimal("10000.00"),
            new BigDecimal("1000.00"),
            new BigDecimal("2.50"),
            10,
            false,
            false,
            ProductId.generate(),
            "Test Product",
            true,
            false,
            JBH_ZERO,
            JBH_ZERO);

    final List<BalanceHistoryEntryDTO> balances = Arrays.asList(balance1);

    final BalanceHistoryDTO response = new BalanceHistoryDTO(summary, balances);

    assertNotNull(response);
    assertEquals(summary, response.summary());
    assertEquals(balances, response.balances());
    assertEquals(1, response.balances().size());
  }

  @Test
  void shouldCreateEmptyResponse() {
    final BalanceHistoryDTO response = BalanceHistoryDTO.empty();

    assertNotNull(response);
    assertNotNull(response.summary());
    assertEquals(JBH_ZERO, response.summary().totalBalance());
    assertEquals(JBH_ZERO, response.summary().periodChange());
    assertEquals(JBH_ZERO, response.summary().periodChangePercent());
    assertEquals(JBH_ZERO, response.summary().avgGrowthRate());
    assertEquals(0, response.summary().totalMovements());
    assertTrue(response.balances().isEmpty());
  }

  @Test
  void shouldCreateResponseWithEmptyBalancesList() {
    final BalanceHistorySummaryDTO summary = BalanceHistorySummaryDTO.empty();
    final BalanceHistoryDTO response = new BalanceHistoryDTO(summary, Collections.emptyList());

    assertNotNull(response);
    assertEquals(summary, response.summary());
    assertTrue(response.balances().isEmpty());
  }

  @Test
  void shouldCreateResponseWithMultipleBalances() {
    final BalanceHistorySummaryDTO summary =
        new BalanceHistorySummaryDTO(
            new BigDecimal("15000.00"),
            new BigDecimal("2000.00"),
            new BigDecimal("15.00"),
            new BigDecimal("3.00"),
            50);

    final BalanceHistoryEntryDTO balance1 =
        new BalanceHistoryEntryDTO(
            YearMonth.of(2025, 1),
            new BigDecimal("10000.00"),
            new BigDecimal("12000.00"),
            new BigDecimal("1000.00"),
            new BigDecimal("2.50"),
            20,
            false,
            false,
            ProductId.generate(),
            "Product 1",
            true,
            false,
            JBH_ZERO,
            JBH_ZERO);

    final BalanceHistoryEntryDTO balance2 =
        new BalanceHistoryEntryDTO(
            YearMonth.of(2025, 2),
            new BigDecimal("12000.00"),
            new BigDecimal("15000.00"),
            new BigDecimal("1000.00"),
            new BigDecimal("3.50"),
            30,
            false,
            true,
            ProductId.generate(),
            "Product 2",
            true,
            false,
            JBH_ZERO,
            JBH_ZERO);

    final List<BalanceHistoryEntryDTO> balances = Arrays.asList(balance1, balance2);

    final BalanceHistoryDTO response = new BalanceHistoryDTO(summary, balances);

    assertNotNull(response);
    assertEquals(summary, response.summary());
    assertEquals(2, response.balances().size());
  }
}
