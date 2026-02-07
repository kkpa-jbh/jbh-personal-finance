package com.jbh.products.application.core.dto;

import static com.jbh.commons.util.JbhMoneyUtils.JBH_ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jbh.products.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.products.domain.product.vo.ProductId;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class MonthlyBalanceDTOTest {

  @Test
  void shouldCreateDTOWithDefaultBuilder() {
    final ProductId accountId = ProductId.generate();
    final YearMonth period = YearMonth.of(2025, 1);

    final MonthlyBalanceDTO dto =
        MonthlyBalanceDTO.defaultBuilder()
            .accountId(accountId)
            .period(period)
            .year(period.getYear())
            .month(period.getMonthValue())
            .build();

    assertNotNull(dto);
    assertEquals(accountId, dto.accountId());
    assertEquals(period, dto.period());
    assertEquals(2025, dto.year());
    assertEquals(1, dto.month());
    assertEquals(JBH_ZERO, dto.movementBalance());
    assertEquals(JBH_ZERO, dto.closingBalance());
    assertEquals(JBH_ZERO, dto.totalDebits());
    assertEquals(JBH_ZERO, dto.totalCredits());
    assertEquals(JBH_ZERO, dto.monthlyNetProfit());
    assertEquals(0, dto.totalMovements());
    assertEquals(JBH_ZERO, dto.openingBalance());
  }

  @Test
  void shouldCreateDTOWithInitialDataForNextMonth() {
    final ProductId accountId = ProductId.generate();
    final YearMonth period = YearMonth.of(2025, 2);
    final BigDecimal closingBalance = new BigDecimal("5000.00");

    final MonthlyBalanceDTO dto =
        MonthlyBalanceDTO.withInitialDataForNextMonth(accountId, period, closingBalance, false);

    assertNotNull(dto);
    assertEquals(accountId, dto.accountId());
    assertEquals(period, dto.period());
    assertEquals(2025, dto.year());
    assertEquals(2, dto.month());
    assertEquals(closingBalance, dto.closingBalance());
    assertFalse(dto.gapPeriod());
  }

  @Test
  void shouldCreateDTOWithGapPeriod() {
    final ProductId accountId = ProductId.generate();
    final YearMonth period = YearMonth.of(2025, 3);
    final BigDecimal closingBalance = new BigDecimal("10000.00");

    final MonthlyBalanceDTO dto =
        MonthlyBalanceDTO.withInitialDataForNextMonth(accountId, period, closingBalance, true);

    assertNotNull(dto);
    assertTrue(dto.gapPeriod());
  }

  @Test
  void shouldReturnTrueForIsProfitableWhenProfitIsPositive() {
    final MonthlyBalanceDTO dto =
        MonthlyBalanceDTO.defaultBuilder()
            .accountId(ProductId.generate())
            .period(YearMonth.of(2025, 1))
            .year(2025)
            .month(1)
            .monthlyNetProfit(new BigDecimal("500.00"))
            .build();

    assertTrue(dto.isProfitable());
    assertFalse(dto.isLoss());
  }

  @Test
  void shouldReturnTrueForIsProfitableWhenProfitIsZero() {
    final MonthlyBalanceDTO dto =
        MonthlyBalanceDTO.defaultBuilder()
            .accountId(ProductId.generate())
            .period(YearMonth.of(2025, 1))
            .year(2025)
            .month(1)
            .monthlyNetProfit(JBH_ZERO)
            .build();

    assertTrue(dto.isProfitable());
    assertFalse(dto.isLoss());
  }

  @Test
  void shouldReturnTrueForIsLossWhenProfitIsNegative() {
    final MonthlyBalanceDTO dto =
        MonthlyBalanceDTO.defaultBuilder()
            .accountId(ProductId.generate())
            .period(YearMonth.of(2025, 1))
            .year(2025)
            .month(1)
            .monthlyNetProfit(new BigDecimal("-500.00"))
            .build();

    assertTrue(dto.isLoss());
    assertFalse(dto.isProfitable());
  }

  @Test
  void shouldCreateFullDTOWithAllFields() {
    final ProductId accountId = ProductId.generate();
    final YearMonth period = YearMonth.of(2025, 1);
    final LocalDateTime now = LocalDateTime.now();

    final MonthlyBalanceDTO dto =
        MonthlyBalanceDTO.defaultBuilder()
            .id(1L)
            .accountId(accountId)
            .year(2025)
            .month(1)
            .period(period)
            .netGrowthRate(new BigDecimal("2.50"))
            .totalDebits(new BigDecimal("1000.00"))
            .totalCredits(new BigDecimal("3000.00"))
            .movementBalance(new BigDecimal("2000.00"))
            .openingBalance(new BigDecimal("5000.00"))
            .closingBalance(new BigDecimal("7000.00"))
            .monthlyNetProfit(new BigDecimal("2000.00"))
            .totalMovements(15)
            .gapPeriod(false)
            .officialMonthlyReport(true)
            .monthlyReportedProfit(new BigDecimal("1900.00"))
            .incomeWithholdingTaxAmount(new BigDecimal("100.00"))
            .createdAt(now)
            .updatedAt(now)
            .build();

    assertNotNull(dto);
    assertEquals(1L, dto.id());
    assertEquals(accountId, dto.accountId());
    assertEquals(2025, dto.year());
    assertEquals(1, dto.month());
    assertEquals(period, dto.period());
    assertEquals(new BigDecimal("2.50"), dto.netGrowthRate());
    assertEquals(new BigDecimal("1000.00"), dto.totalDebits());
    assertEquals(new BigDecimal("3000.00"), dto.totalCredits());
    assertEquals(new BigDecimal("2000.00"), dto.movementBalance());
    assertEquals(new BigDecimal("5000.00"), dto.openingBalance());
    assertEquals(new BigDecimal("7000.00"), dto.closingBalance());
    assertEquals(new BigDecimal("2000.00"), dto.monthlyNetProfit());
    assertEquals(15, dto.totalMovements());
    assertFalse(dto.gapPeriod());
    assertTrue(dto.officialMonthlyReport());
    assertEquals(new BigDecimal("1900.00"), dto.monthlyReportedProfit());
    assertEquals(new BigDecimal("100.00"), dto.incomeWithholdingTaxAmount());
    assertEquals(now, dto.createdAt());
    assertEquals(now, dto.updatedAt());
  }

  @Test
  void shouldCreateDTOWithNullOptionalFields() {
    final MonthlyBalanceDTO dto =
        MonthlyBalanceDTO.defaultBuilder()
            .accountId(ProductId.generate())
            .period(YearMonth.of(2025, 1))
            .year(2025)
            .month(1)
            .id(null)
            .netGrowthRate(null)
            .incomeWithholdingTaxAmount(null)
            .monthlyReportedProfit(null)
            .createdAt(null)
            .updatedAt(null)
            .build();

    assertNotNull(dto);
    assertNull(dto.id());
    assertNull(dto.netGrowthRate());
    assertNull(dto.incomeWithholdingTaxAmount());
    assertNull(dto.monthlyReportedProfit());
    assertNull(dto.createdAt());
    assertNull(dto.updatedAt());
  }
}
