package com.jbh.finance.application.core.ports.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.finance.application.feature.monthlybalance.dto.balancehistory.BalanceHistoryResponse;
import com.jbh.finance.application.feature.monthlybalance.ports.input.FindMonthlyBalanceInputPort;
import com.jbh.finance.application.feature.monthlybalance.services.MonthlyBalanceLifecycleService;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.application.feature.product.services.ProductLifecycleService;
import com.jbh.finance.domain.product.vo.ProductId;
import com.jbh.finance.domain.product.vo.ProductPK;
import com.jbh.finance.domain.product.vo.ProductType;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class FindMonthlyBalanceInputPortTest {

  private static final UUID TEST_USER_ID = UUID.randomUUID();
  private static final ProductId TEST_PRODUCT_ID = ProductId.generate();
  private static final YearMonth START_PERIOD = YearMonth.of(2025, 1);
  private static final YearMonth END_PERIOD = YearMonth.of(2025, 3);
  private static final YearMonth TODAY = YearMonth.of(2025, 6);

  private FindMonthlyBalanceInputPort inputPort;

  @Mock private MonthlyBalanceLifecycleService monthlyBalanceService;

  @Mock private ProductLifecycleService productService;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    inputPort = new FindMonthlyBalanceInputPort(monthlyBalanceService, productService);
  }

  @Test
  public void shouldFindMonthlyBalancesByProduct() throws BusinessException {
    final ProductPK accountPK = new ProductPK(TEST_USER_ID, TEST_PRODUCT_ID);
    final ProductDTO product = createTestProduct();

    final MonthlyBalanceDTO balance1 = createTestMonthlyBalance(YearMonth.of(2025, 1));
    final MonthlyBalanceDTO balance2 = createTestMonthlyBalance(YearMonth.of(2025, 2));
    final List<MonthlyBalanceDTO> expectedBalances = Arrays.asList(balance1, balance2);

    when(productService.findByUserAndProductId(TEST_USER_ID, TEST_PRODUCT_ID)).thenReturn(product);
    when(monthlyBalanceService.findByAccountAndPeriods(accountPK, START_PERIOD, END_PERIOD))
        .thenReturn(expectedBalances);

    final List<MonthlyBalanceDTO> result =
        inputPort.findMonthlyBalancesByProduct(accountPK, START_PERIOD, END_PERIOD);

    assertNotNull(result);
    assertEquals(2, result.size());
    verify(productService).findByUserAndProductId(TEST_USER_ID, TEST_PRODUCT_ID);
    verify(monthlyBalanceService).findByAccountAndPeriods(accountPK, START_PERIOD, END_PERIOD);
  }

  private ProductDTO createTestProduct() {
    return ProductDTO.defaultBuilder(
            TEST_USER_ID, TEST_PRODUCT_ID, "Test Product", ProductType.SAVINGS)
        .build();
  }

  private MonthlyBalanceDTO createTestMonthlyBalance(final YearMonth period) {
    return MonthlyBalanceDTO.defaultBuilder()
        .accountId(TEST_PRODUCT_ID)
        .period(period)
        .year(period.getYear())
        .month(period.getMonthValue())
        .openingBalance(new BigDecimal("5000.00"))
        .closingBalance(new BigDecimal("6000.00"))
        .monthlyNetProfit(new BigDecimal("1000.00"))
        .netGrowthRate(new BigDecimal("3.00"))
        .totalMovements(5)
        .build();
  }

  @Test
  public void shouldThrowExceptionWhenStartPeriodIsNull() {
    final ProductPK accountPK = new ProductPK(TEST_USER_ID, TEST_PRODUCT_ID);

    final BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> inputPort.findMonthlyBalancesByProduct(accountPK, null, END_PERIOD));

    assertNotNull(exception);
  }

  @Test
  public void shouldThrowExceptionWhenEndPeriodIsNull() {
    final ProductPK accountPK = new ProductPK(TEST_USER_ID, TEST_PRODUCT_ID);

    final BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> inputPort.findMonthlyBalancesByProduct(accountPK, START_PERIOD, null));

    assertNotNull(exception);
  }

  @Test
  public void shouldThrowExceptionWhenStartPeriodIsAfterEndPeriod() {
    final ProductPK accountPK = new ProductPK(TEST_USER_ID, TEST_PRODUCT_ID);
    final YearMonth invalidStartPeriod = YearMonth.of(2025, 6);
    final YearMonth invalidEndPeriod = YearMonth.of(2025, 1);

    final BusinessException exception =
        assertThrows(
            BusinessException.class,
            () ->
                inputPort.findMonthlyBalancesByProduct(
                    accountPK, invalidStartPeriod, invalidEndPeriod));

    assertNotNull(exception);
  }

  @Test
  public void shouldThrowExceptionWhenEndPeriodIsAfterNow() {
    final ProductPK accountPK = new ProductPK(TEST_USER_ID, TEST_PRODUCT_ID);
    final YearMonth futureEndPeriod = YearMonth.now().plusMonths(2);

    final BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> inputPort.findMonthlyBalancesByProduct(accountPK, START_PERIOD, futureEndPeriod));

    assertNotNull(exception);
  }

  @Test
  public void shouldFindBalanceHistoryByProduct() throws BusinessException {
    final ProductPK accountPK = new ProductPK(TEST_USER_ID, TEST_PRODUCT_ID);
    final ProductDTO product = createTestProduct();

    final MonthlyBalanceDTO balance1 = createTestMonthlyBalance(START_PERIOD);
    final List<MonthlyBalanceDTO> balances = Collections.singletonList(balance1);

    when(productService.findByUserAndProductId(TEST_USER_ID, TEST_PRODUCT_ID)).thenReturn(product);
    when(monthlyBalanceService.findByProductIdsAndPeriods(any(), any())).thenReturn(balances);

    final BalanceHistoryResponse result =
        inputPort.findBalanceHistoryByProduct(accountPK, START_PERIOD, END_PERIOD, TODAY);

    assertNotNull(result);
    assertNotNull(result.summary());
    assertNotNull(result.balances());
    verify(productService).findByUserAndProductId(TEST_USER_ID, TEST_PRODUCT_ID);
  }

  @Test
  public void shouldReturnEmptyHistoryWhenNoBalancesFound() throws BusinessException {
    final ProductPK accountPK = new ProductPK(TEST_USER_ID, TEST_PRODUCT_ID);
    final ProductDTO product = createTestProduct();

    when(productService.findByUserAndProductId(TEST_USER_ID, TEST_PRODUCT_ID)).thenReturn(product);
    when(monthlyBalanceService.findByProductIdsAndPeriods(any(), any()))
        .thenReturn(Collections.emptyList());

    final BalanceHistoryResponse result =
        inputPort.findBalanceHistoryByProduct(accountPK, START_PERIOD, END_PERIOD, TODAY);

    assertNotNull(result);
    assertNotNull(result.summary());
    assertTrue(result.balances().isEmpty());
  }

  @Test
  public void shouldFindBalanceHistoryByUser() throws BusinessException {
    final ProductDTO product = createTestProduct();
    final List<ProductDTO> products = Collections.singletonList(product);

    final MonthlyBalanceDTO balance1 = createTestMonthlyBalance(START_PERIOD);
    final List<MonthlyBalanceDTO> balances = Collections.singletonList(balance1);

    when(productService.findActiveByUserId(TEST_USER_ID)).thenReturn(products);
    when(monthlyBalanceService.findByProductIdsAndPeriods(any(), any())).thenReturn(balances);

    final BalanceHistoryResponse result =
        inputPort.findBalanceHistoryByUser(TEST_USER_ID, START_PERIOD, END_PERIOD, TODAY);

    assertNotNull(result);
    assertNotNull(result.summary());
    assertNotNull(result.balances());
    verify(productService).findActiveByUserId(TEST_USER_ID);
  }

  @Test
  public void shouldReturnEmptyHistoryWhenUserHasNoProducts() throws BusinessException {
    when(productService.findActiveByUserId(TEST_USER_ID)).thenReturn(Collections.emptyList());

    final BalanceHistoryResponse result =
        inputPort.findBalanceHistoryByUser(TEST_USER_ID, START_PERIOD, END_PERIOD, TODAY);

    assertNotNull(result);
    assertNotNull(result.summary());
    assertTrue(result.balances().isEmpty());
    verify(productService).findActiveByUserId(TEST_USER_ID);
  }

  @Test
  public void shouldCalculateSummaryCorrectly() throws BusinessException {
    final ProductDTO product = createTestProduct();
    final List<ProductDTO> products = Collections.singletonList(product);

    final MonthlyBalanceDTO balance1 =
        MonthlyBalanceDTO.defaultBuilder()
            .accountId(TEST_PRODUCT_ID)
            .period(START_PERIOD)
            .year(START_PERIOD.getYear())
            .month(START_PERIOD.getMonthValue())
            .openingBalance(new BigDecimal("10000.00"))
            .closingBalance(new BigDecimal("12000.00"))
            .monthlyNetProfit(new BigDecimal("2000.00"))
            .netGrowthRate(new BigDecimal("5.00"))
            .totalMovements(10)
            .build();

    final MonthlyBalanceDTO balance2 =
        MonthlyBalanceDTO.defaultBuilder()
            .accountId(TEST_PRODUCT_ID)
            .period(END_PERIOD)
            .year(END_PERIOD.getYear())
            .month(END_PERIOD.getMonthValue())
            .openingBalance(new BigDecimal("12000.00"))
            .closingBalance(new BigDecimal("15000.00"))
            .monthlyNetProfit(new BigDecimal("3000.00"))
            .netGrowthRate(new BigDecimal("7.00"))
            .totalMovements(15)
            .build();

    final List<MonthlyBalanceDTO> balances = Arrays.asList(balance1, balance2);

    when(productService.findActiveByUserId(TEST_USER_ID)).thenReturn(products);
    when(monthlyBalanceService.findByProductIdsAndPeriods(any(), any())).thenReturn(balances);

    final BalanceHistoryResponse result =
        inputPort.findBalanceHistoryByUser(TEST_USER_ID, START_PERIOD, END_PERIOD, TODAY);

    assertNotNull(result);
    assertNotNull(result.summary());
    assertEquals(25, result.summary().totalMovements());
  }

  @Test
  public void shouldThrowExceptionWhenPeriodRangeInvalidForHistoryByProduct() {
    final ProductPK accountPK = new ProductPK(TEST_USER_ID, TEST_PRODUCT_ID);

    final BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> inputPort.findBalanceHistoryByProduct(accountPK, null, END_PERIOD, TODAY));

    assertNotNull(exception);
  }

  @Test
  public void shouldThrowExceptionWhenPeriodRangeInvalidForHistoryByUser() {
    final BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> inputPort.findBalanceHistoryByUser(TEST_USER_ID, START_PERIOD, null, TODAY));

    assertNotNull(exception);
  }
}
