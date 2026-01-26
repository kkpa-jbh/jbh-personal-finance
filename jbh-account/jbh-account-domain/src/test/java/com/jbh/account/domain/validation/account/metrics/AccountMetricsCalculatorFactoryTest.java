package com.jbh.account.domain.validation.account.metrics;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jbh.account.domain.vo.ProductType;
import com.jbh.commons.exception.BusinessException;
import org.junit.jupiter.api.Test;

class AccountMetricsCalculatorFactoryTest {

  @Test
  void shouldGetSavingsAccountMetricsCalculator() throws BusinessException {
    // When
    final ProductMetricsCalculator calculator =
        AccountMetricsCalculatorFactory.getCalculator(ProductType.SAVINGS);

    // Then
    assertNotNull(calculator);
    assertInstanceOf(SavingsAccountMetricsCalculator.class, calculator);
  }

  @Test
  void shouldGetCreditCardAccountMetricsCalculator() throws BusinessException {
    // When
    final ProductMetricsCalculator calculator =
        AccountMetricsCalculatorFactory.getCalculator(ProductType.CREDIT_CARD);

    // Then
    assertNotNull(calculator);
    assertInstanceOf(CreditCardAccountMetricsCalculator.class, calculator);
  }

  @Test
  void shouldGetInvestmentAccountMetricsCalculator() throws BusinessException {
    // When
    final ProductMetricsCalculator calculator =
        AccountMetricsCalculatorFactory.getCalculator(ProductType.INVESTMENT);

    // Then
    assertNotNull(calculator);
    assertInstanceOf(InvestmentAccountMetricsCalculator.class, calculator);
  }

  @Test
  void shouldGetCdtAccountMetricsCalculator() throws BusinessException {
    // When
    final ProductMetricsCalculator calculator =
        AccountMetricsCalculatorFactory.getCalculator(ProductType.CDT);

    // Then
    assertNotNull(calculator);
    assertInstanceOf(CdtAccountMetricsCalculator.class, calculator);
  }

  @Test
  void shouldThrowExceptionWhenTryingToInstantiateFactory() throws Exception {
    // Given
    final var constructor = AccountMetricsCalculatorFactory.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    // When/Then
    final var exception =
        assertThrows(
            java.lang.reflect.InvocationTargetException.class, () -> constructor.newInstance());

    assertNotNull(exception.getCause());
    assertInstanceOf(UnsupportedOperationException.class, exception.getCause());
  }
}
