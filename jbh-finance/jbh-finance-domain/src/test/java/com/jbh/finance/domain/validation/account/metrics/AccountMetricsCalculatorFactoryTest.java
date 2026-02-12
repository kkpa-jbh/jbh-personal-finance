package com.jbh.finance.domain.validation.account.metrics;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.domain.product.service.metrics.CdtAccountMetricsCalculator;
import com.jbh.finance.domain.product.service.metrics.CreditCardAccountMetricsCalculator;
import com.jbh.finance.domain.product.service.metrics.InvestmentAccountMetricsCalculator;
import com.jbh.finance.domain.product.service.metrics.ProductMetricsCalculator;
import com.jbh.finance.domain.product.service.metrics.ProductMetricsCalculatorFactory;
import com.jbh.finance.domain.product.service.metrics.SavingsAccountMetricsCalculator;
import com.jbh.finance.domain.product.vo.ProductType;
import org.junit.jupiter.api.Test;

class ProductMetricsCalculatorFactoryTest {

  @Test
  void shouldGetSavingsAccountMetricsCalculator() throws BusinessException {
    // When
    final ProductMetricsCalculator calculator =
        ProductMetricsCalculatorFactory.getCalculator(ProductType.SAVINGS);

    // Then
    assertNotNull(calculator);
    assertInstanceOf(SavingsAccountMetricsCalculator.class, calculator);
  }

  @Test
  void shouldGetCreditCardAccountMetricsCalculator() throws BusinessException {
    // When
    final ProductMetricsCalculator calculator =
        ProductMetricsCalculatorFactory.getCalculator(ProductType.CREDIT_CARD);

    // Then
    assertNotNull(calculator);
    assertInstanceOf(CreditCardAccountMetricsCalculator.class, calculator);
  }

  @Test
  void shouldGetInvestmentAccountMetricsCalculator() throws BusinessException {
    // When
    final ProductMetricsCalculator calculator =
        ProductMetricsCalculatorFactory.getCalculator(ProductType.INVESTMENT);

    // Then
    assertNotNull(calculator);
    assertInstanceOf(InvestmentAccountMetricsCalculator.class, calculator);
  }

  @Test
  void shouldGetCdtAccountMetricsCalculator() throws BusinessException {
    // When
    final ProductMetricsCalculator calculator =
        ProductMetricsCalculatorFactory.getCalculator(ProductType.CDT);

    // Then
    assertNotNull(calculator);
    assertInstanceOf(CdtAccountMetricsCalculator.class, calculator);
  }

  @Test
  void shouldThrowExceptionWhenTryingToInstantiateFactory() throws Exception {
    // Given
    final var constructor = ProductMetricsCalculatorFactory.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    // When/Then
    final var exception =
        assertThrows(
            java.lang.reflect.InvocationTargetException.class, () -> constructor.newInstance());

    assertNotNull(exception.getCause());
    assertInstanceOf(UnsupportedOperationException.class, exception.getCause());
  }
}
