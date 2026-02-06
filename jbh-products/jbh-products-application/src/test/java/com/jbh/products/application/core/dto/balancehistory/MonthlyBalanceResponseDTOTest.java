package com.jbh.products.application.core.dto.balancehistory;

import static com.jbh.commons.util.JbhMoneyUtils.JBH_ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jbh.products.application.core.dto.MonthlyBalanceDTO;
import com.jbh.products.application.core.dto.ProductDTO;
import com.jbh.products.domain.vo.ProductId;
import com.jbh.products.domain.vo.ProductType;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MonthlyBalanceResponseDTOTest {

  @Test
  void shouldCreateResponseFromDTOWithAllFields() {
    final ProductId productId = ProductId.generate();
    final UUID userId = UUID.randomUUID();
    final YearMonth period = YearMonth.of(2025, 1);

    final MonthlyBalanceDTO monthlyBalance =
        MonthlyBalanceDTO.defaultBuilder()
            .accountId(productId)
            .period(period)
            .year(2025)
            .month(1)
            .openingBalance(new BigDecimal("10000.00"))
            .closingBalance(new BigDecimal("12000.00"))
            .monthlyNetProfit(new BigDecimal("2000.00"))
            .netGrowthRate(new BigDecimal("5.00"))
            .totalMovements(15)
            .gapPeriod(false)
            .officialMonthlyReport(false)
            .build();

    final ProductDTO product =
        ProductDTO.defaultBuilder(userId, productId, "Test Product", ProductType.SAVINGS).build();

    final BalanceHistoryEntryResponse response =
        BalanceHistoryEntryResponse.fromDTO(monthlyBalance, product);

    assertNotNull(response);
    assertEquals(period, response.period());
    assertEquals(new BigDecimal("10000.00"), response.openingBalance());
    assertEquals(new BigDecimal("12000.00"), response.closingBalance());
    assertEquals(new BigDecimal("2000.00"), response.profit());
    assertEquals(new BigDecimal("5.00"), response.growthRate());
    assertEquals(15, response.movementCount());
    assertFalse(response.isGapPeriod());
    assertFalse(response.isOfficialReport());
    assertEquals(productId, response.productId());
    assertEquals("Test Product", response.productName());
    assertTrue(response.isProfitable());
    assertFalse(response.isLoss());
  }

  @Test
  void shouldCreateResponseFromDTOWithOfficialReport() {
    final ProductId productId = ProductId.generate();
    final UUID userId = UUID.randomUUID();
    final YearMonth period = YearMonth.of(2025, 2);

    final MonthlyBalanceDTO monthlyBalance =
        MonthlyBalanceDTO.defaultBuilder()
            .accountId(productId)
            .period(period)
            .year(2025)
            .month(2)
            .openingBalance(new BigDecimal("5000.00"))
            .closingBalance(new BigDecimal("8000.00"))
            .monthlyNetProfit(new BigDecimal("2500.00"))
            .monthlyReportedProfit(new BigDecimal("3000.00"))
            .netGrowthRate(new BigDecimal("10.00"))
            .totalMovements(20)
            .gapPeriod(false)
            .officialMonthlyReport(true)
            .build();

    final ProductDTO product =
        ProductDTO.defaultBuilder(userId, productId, "Investment Account", ProductType.INVESTMENT)
            .build();

    final BalanceHistoryEntryResponse response =
        BalanceHistoryEntryResponse.fromDTO(monthlyBalance, product);

    assertNotNull(response);
    assertEquals(new BigDecimal("3000.00"), response.profit());
    assertTrue(response.isOfficialReport());
    assertEquals("Investment Account", response.productName());
  }

  @Test
  void shouldCreateResponseFromDTOWithGapPeriod() {
    final ProductId productId = ProductId.generate();
    final UUID userId = UUID.randomUUID();
    final YearMonth period = YearMonth.of(2025, 3);

    final MonthlyBalanceDTO monthlyBalance =
        MonthlyBalanceDTO.defaultBuilder()
            .accountId(productId)
            .period(period)
            .year(2025)
            .month(3)
            .openingBalance(JBH_ZERO)
            .closingBalance(JBH_ZERO)
            .monthlyNetProfit(JBH_ZERO)
            .netGrowthRate(JBH_ZERO)
            .totalMovements(0)
            .gapPeriod(true)
            .officialMonthlyReport(false)
            .build();

    final ProductDTO product =
        ProductDTO.defaultBuilder(userId, productId, "Savings Account", ProductType.SAVINGS)
            .build();

    final BalanceHistoryEntryResponse response =
        BalanceHistoryEntryResponse.fromDTO(monthlyBalance, product);

    assertNotNull(response);
    assertTrue(response.isGapPeriod());
    assertEquals(0, response.movementCount());
  }

  @Test
  void shouldCreateResponseFromDTOWithNegativeProfit() {
    final ProductId productId = ProductId.generate();
    final UUID userId = UUID.randomUUID();
    final YearMonth period = YearMonth.of(2025, 4);

    final MonthlyBalanceDTO monthlyBalance =
        MonthlyBalanceDTO.defaultBuilder()
            .accountId(productId)
            .period(period)
            .year(2025)
            .month(4)
            .openingBalance(new BigDecimal("10000.00"))
            .closingBalance(new BigDecimal("9000.00"))
            .monthlyNetProfit(new BigDecimal("-1000.00"))
            .netGrowthRate(new BigDecimal("-5.00"))
            .totalMovements(8)
            .gapPeriod(false)
            .officialMonthlyReport(false)
            .build();

    final ProductDTO product =
        ProductDTO.defaultBuilder(userId, productId, "Trading Account", ProductType.INVESTMENT)
            .build();

    final BalanceHistoryEntryResponse response =
        BalanceHistoryEntryResponse.fromDTO(monthlyBalance, product);

    assertNotNull(response);
    assertEquals(new BigDecimal("-1000.00"), response.profit());
    assertEquals(new BigDecimal("-5.00"), response.growthRate());
    assertFalse(response.isProfitable());
    assertTrue(response.isLoss());
  }

  @Test
  void shouldCreateResponseDirectlyUsingConstructor() {
    final ProductId productId = ProductId.generate();
    final YearMonth period = YearMonth.of(2025, 5);

    final BalanceHistoryEntryResponse response =
        new BalanceHistoryEntryResponse(
            period,
            new BigDecimal("15000.00"),
            new BigDecimal("16000.00"),
            new BigDecimal("1000.00"),
            new BigDecimal("3.00"),
            12,
            false,
            true,
            productId,
            "Credit Card",
            true,
            false,
            JBH_ZERO,
            JBH_ZERO);

    assertNotNull(response);
    assertEquals(period, response.period());
    assertEquals(new BigDecimal("15000.00"), response.openingBalance());
    assertEquals(new BigDecimal("16000.00"), response.closingBalance());
    assertEquals(new BigDecimal("1000.00"), response.profit());
    assertEquals(new BigDecimal("3.00"), response.growthRate());
    assertEquals(12, response.movementCount());
    assertFalse(response.isGapPeriod());
    assertTrue(response.isOfficialReport());
    assertEquals(productId, response.productId());
    assertEquals("Credit Card", response.productName());
    assertTrue(response.isProfitable());
    assertFalse(response.isLoss());
  }
}
