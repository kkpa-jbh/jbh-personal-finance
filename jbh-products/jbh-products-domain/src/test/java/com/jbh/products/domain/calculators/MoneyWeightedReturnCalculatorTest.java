package com.jbh.products.domain.calculators;

import static org.junit.jupiter.api.Assertions.*;

import com.jbh.products.domain.product.service.calculators.MoneyWeightedReturnCalculator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

public class MoneyWeightedReturnCalculatorTest {
  public static final List<BigDecimal> cashFlows =
      List.of(
          new BigDecimal("-12591000"),
          new BigDecimal("-22685312"),
          new BigDecimal("0"),
          new BigDecimal("0"),
          new BigDecimal("673605"),
          new BigDecimal("1271000"),
          new BigDecimal("9590134"),
          new BigDecimal("6780000"),
          new BigDecimal("14979690"),
          new BigDecimal("3768488"));

  public static final List<LocalDate> dates =
      List.of(
          LocalDate.of(2024, 7, 31),
          LocalDate.of(2024, 8, 31),
          LocalDate.of(2024, 9, 30),
          LocalDate.of(2024, 10, 30),
          LocalDate.of(2024, 11, 30),
          LocalDate.of(2024, 12, 30),
          LocalDate.of(2025, 1, 31),
          LocalDate.of(2025, 2, 1),
          LocalDate.of(2025, 2, 28),
          LocalDate.of(2025, 3, 31));

  @Test
  void testCalculateMWRR() {

    final BigDecimal annualRate = MoneyWeightedReturnCalculator.calculateXIRR(cashFlows, dates);
    final BigDecimal monthlyRate = MoneyWeightedReturnCalculator.toMonthlyRate(annualRate);

    System.out.printf("MWRR (Anualizado): %.2f%%\n", annualRate);
    System.out.printf("MWRR (Mensual): %.2f%%\n", monthlyRate);
    // rango lógico
  }

  @Test
  void testTrii() {
    final BigDecimal initialBalance = new BigDecimal("-5000000");
    final List<BigDecimal> cashFlows = List.of(initialBalance, new BigDecimal("5065484.00"));
    final List<LocalDate> dates =
        List.of(YearMonth.of(2025, 8).atEndOfMonth(), YearMonth.of(2025, 10).atEndOfMonth());

    final BigDecimal annualRate = MoneyWeightedReturnCalculator.calculateXIRR(cashFlows, dates);
    final BigDecimal monthlyRate = MoneyWeightedReturnCalculator.toMonthlyRate(annualRate);

    System.out.printf("MWRR (Anualizado): %.2f%%\n", annualRate);
    System.out.printf("MWRR (Mensual): %.2f%%\n", monthlyRate);
  }
}
