package com.jbh.products.application.core.dto.balancehistory;

import static com.jbh.commons.util.JbhMoneyUtils.JBH_ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.jbh.products.application.feature.monthlybalance.dto.balancehistory.BalanceHistorySummaryResponse;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class BalanceHistorySummaryTest {

  @Test
  void shouldCreateSummaryWithAllFields() {
    final BigDecimal totalBalance = new BigDecimal("10000.00");
    final BigDecimal periodChange = new BigDecimal("1000.00");
    final BigDecimal periodChangePercent = new BigDecimal("10.00");
    final BigDecimal avgGrowthRate = new BigDecimal("2.50");
    final int totalMovements = 25;

    final BalanceHistorySummaryResponse summary =
        new BalanceHistorySummaryResponse(
            totalBalance, periodChange, periodChangePercent, avgGrowthRate, totalMovements);

    assertNotNull(summary);
    assertEquals(totalBalance, summary.totalBalance());
    assertEquals(periodChange, summary.periodChange());
    assertEquals(periodChangePercent, summary.periodChangePercent());
    assertEquals(avgGrowthRate, summary.avgGrowthRate());
    assertEquals(totalMovements, summary.totalMovements());
  }

  @Test
  void shouldCreateEmptySummary() {
    final BalanceHistorySummaryResponse summary = BalanceHistorySummaryResponse.empty();

    assertNotNull(summary);
    assertEquals(JBH_ZERO, summary.totalBalance());
    assertEquals(JBH_ZERO, summary.periodChange());
    assertEquals(JBH_ZERO, summary.periodChangePercent());
    assertEquals(JBH_ZERO, summary.avgGrowthRate());
    assertEquals(0, summary.totalMovements());
  }

  @Test
  void shouldCreateSummaryWithNegativeValues() {
    final BigDecimal totalBalance = new BigDecimal("-5000.00");
    final BigDecimal periodChange = new BigDecimal("-1500.00");
    final BigDecimal periodChangePercent = new BigDecimal("-15.00");
    final BigDecimal avgGrowthRate = new BigDecimal("-3.00");
    final int totalMovements = 10;

    final BalanceHistorySummaryResponse summary =
        new BalanceHistorySummaryResponse(
            totalBalance, periodChange, periodChangePercent, avgGrowthRate, totalMovements);

    assertNotNull(summary);
    assertEquals(totalBalance, summary.totalBalance());
    assertEquals(periodChange, summary.periodChange());
    assertEquals(periodChangePercent, summary.periodChangePercent());
    assertEquals(avgGrowthRate, summary.avgGrowthRate());
    assertEquals(totalMovements, summary.totalMovements());
  }
}
