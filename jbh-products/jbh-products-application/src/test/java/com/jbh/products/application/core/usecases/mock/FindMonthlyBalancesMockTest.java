package com.jbh.products.application.core.usecases.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jbh.commons.exception.BusinessException;
import com.jbh.products.application.core.dto.MonthlyBalanceDTO;
import com.jbh.products.application.core.dto.ProductDTO;
import com.jbh.products.application.core.ports.input.FindMonthlyBalanceInputPort;
import com.jbh.products.application.core.services.account.ProductsService;
import com.jbh.products.application.core.services.monthlybalance.MonthlyBalanceService;
import com.jbh.products.application.core.usecases.FindMonthlyBalanceUseCase;
import com.jbh.products.domain.vo.ProductId;
import com.jbh.products.domain.vo.ProductType;
import java.time.YearMonth;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class FindMonthlyBalancesMockTest {

  private static final UUID TEST_USER_ID = UUID.randomUUID();
  private static final YearMonth START_PERIOD = YearMonth.of(2024, 1);
  private static final YearMonth END_PERIOD = YearMonth.of(2024, 6);

  private FindMonthlyBalanceUseCase useCase;

  @Mock private MonthlyBalanceService monthlyBalanceService;
  @Mock private ProductsService productsService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    useCase = new FindMonthlyBalanceInputPort(monthlyBalanceService, productsService);
  }

  @Test
  void shouldReturnMonthlyBalancesForAllActiveProducts() throws BusinessException {
    final ProductId productId1 = ProductId.generate();
    final ProductId productId2 = ProductId.generate();

    final ProductDTO product1 =
        ProductDTO.defaultBuilder(TEST_USER_ID, productId1, "Savings", ProductType.SAVINGS)
            .isActive(true)
            .build();
    final ProductDTO product2 =
        ProductDTO.defaultBuilder(TEST_USER_ID, productId2, "Investment", ProductType.INVESTMENT)
            .isActive(true)
            .build();

    final List<ProductDTO> activeProducts = List.of(product1, product2);

    final MonthlyBalanceDTO balance1 =
        MonthlyBalanceDTO.defaultBuilder()
            .accountId(productId1)
            .period(YearMonth.of(2024, 1))
            .year(2024)
            .month(1)
            .build();
    final MonthlyBalanceDTO balance2 =
        MonthlyBalanceDTO.defaultBuilder()
            .accountId(productId2)
            .period(YearMonth.of(2024, 1))
            .year(2024)
            .month(1)
            .build();

    final Map<ProductId, List<MonthlyBalanceDTO>> expectedBalances = new HashMap<>();
    expectedBalances.put(productId1, List.of(balance1));
    expectedBalances.put(productId2, List.of(balance2));

    when(productsService.findActiveByUserId(TEST_USER_ID)).thenReturn(activeProducts);
    when(monthlyBalanceService.findByProductIdsAndPeriods(
            List.of(productId1, productId2), START_PERIOD, END_PERIOD))
        .thenReturn(expectedBalances);

    final Map<ProductId, List<MonthlyBalanceDTO>> result =
        useCase.findByActiveProductsAndPeriods(TEST_USER_ID, START_PERIOD, END_PERIOD);

    assertNotNull(result);
    assertEquals(2, result.size());
    assertTrue(result.containsKey(productId1));
    assertTrue(result.containsKey(productId2));

    verify(productsService).findActiveByUserId(TEST_USER_ID);
    verify(monthlyBalanceService)
        .findByProductIdsAndPeriods(List.of(productId1, productId2), START_PERIOD, END_PERIOD);
  }

  @Test
  void shouldReturnEmptyMapWhenNoActiveProducts() throws BusinessException {
    when(productsService.findActiveByUserId(TEST_USER_ID)).thenReturn(Collections.emptyList());

    final Map<ProductId, List<MonthlyBalanceDTO>> result =
        useCase.findByActiveProductsAndPeriods(TEST_USER_ID, START_PERIOD, END_PERIOD);

    assertNotNull(result);
    assertTrue(result.isEmpty());

    verify(productsService).findActiveByUserId(TEST_USER_ID);
    verify(monthlyBalanceService, never())
        .findByProductIdsAndPeriods(any(), eq(START_PERIOD), eq(END_PERIOD));
  }

  @Test
  void shouldThrowExceptionWhenStartPeriodIsNull() {
    assertThrows(
        BusinessException.class,
        () -> useCase.findByActiveProductsAndPeriods(TEST_USER_ID, null, END_PERIOD));
  }

  @Test
  void shouldThrowExceptionWhenEndPeriodIsNull() {
    assertThrows(
        BusinessException.class,
        () -> useCase.findByActiveProductsAndPeriods(TEST_USER_ID, START_PERIOD, null));
  }

  @Test
  void shouldThrowExceptionWhenStartPeriodIsAfterEndPeriod() {
    final YearMonth invalidStart = YearMonth.of(2024, 6);
    final YearMonth invalidEnd = YearMonth.of(2024, 1);

    assertThrows(
        BusinessException.class,
        () -> useCase.findByActiveProductsAndPeriods(TEST_USER_ID, invalidStart, invalidEnd));
  }

  @Test
  void shouldThrowExceptionWhenEndPeriodIsInFuture() {
    final YearMonth futureEnd = YearMonth.now().plusMonths(1);

    assertThrows(
        BusinessException.class,
        () -> useCase.findByActiveProductsAndPeriods(TEST_USER_ID, START_PERIOD, futureEnd));
  }

  @Test
  void shouldReturnBalancesOnlyForProductsWithData() throws BusinessException {
    final ProductId productId1 = ProductId.generate();
    final ProductId productId2 = ProductId.generate();

    final ProductDTO product1 =
        ProductDTO.defaultBuilder(TEST_USER_ID, productId1, "Savings", ProductType.SAVINGS)
            .isActive(true)
            .build();
    final ProductDTO product2 =
        ProductDTO.defaultBuilder(TEST_USER_ID, productId2, "Investment", ProductType.INVESTMENT)
            .isActive(true)
            .build();

    final List<ProductDTO> activeProducts = List.of(product1, product2);

    final MonthlyBalanceDTO balance1 =
        MonthlyBalanceDTO.defaultBuilder()
            .accountId(productId1)
            .period(YearMonth.of(2024, 1))
            .year(2024)
            .month(1)
            .build();

    final Map<ProductId, List<MonthlyBalanceDTO>> expectedBalances = new HashMap<>();
    expectedBalances.put(productId1, List.of(balance1));

    when(productsService.findActiveByUserId(TEST_USER_ID)).thenReturn(activeProducts);
    when(monthlyBalanceService.findByProductIdsAndPeriods(
            List.of(productId1, productId2), START_PERIOD, END_PERIOD))
        .thenReturn(expectedBalances);

    final Map<ProductId, List<MonthlyBalanceDTO>> result =
        useCase.findByActiveProductsAndPeriods(TEST_USER_ID, START_PERIOD, END_PERIOD);

    assertNotNull(result);
    assertEquals(1, result.size());
    assertTrue(result.containsKey(productId1));

    verify(productsService).findActiveByUserId(TEST_USER_ID);
  }
}
