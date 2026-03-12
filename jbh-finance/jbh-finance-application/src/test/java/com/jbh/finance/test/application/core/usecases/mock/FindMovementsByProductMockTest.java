package com.jbh.finance.test.application.core.usecases.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.application.feature.movement.ports.input.FindMovementsInputPort;
import com.jbh.finance.application.feature.movement.ports.output.MovementQueryRepository;
import com.jbh.finance.application.feature.movement.ports.output.MovementWriterRepository;
import com.jbh.finance.application.feature.movement.services.MovementLifecycleService;
import com.jbh.finance.application.feature.movement.services.MovementLifecycleServiceImpl;
import com.jbh.finance.application.feature.movement.usecases.FindMovementsUseCase;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.application.feature.product.services.ProductLifecycleService;
import com.jbh.finance.application.shared.exceptions.BusinessApplicationExceptionType;
import com.jbh.finance.domain.movement.vo.MovementId;
import com.jbh.finance.domain.movement.vo.MovementType;
import com.jbh.finance.domain.product.vo.ProductId;
import com.jbh.finance.domain.product.vo.ProductType;
import com.jbh.finance.test.testfixtures.CategoryFixturesTestApp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class FindMovementsByProductMockTest {

  private static final UUID TEST_USER_ID = UUID.randomUUID();
  private static final ProductId TEST_PRODUCT_ID = ProductId.generate();

  private FindMovementsUseCase useCase;

  @Mock private MovementQueryRepository movementQueryRepoMock;
  @Mock private MovementWriterRepository movementWriterRepositoryMock;
  @Mock private ProductLifecycleService productLifecycleService;

  private MovementLifecycleService movementLifecycleService;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    movementLifecycleService =
        new MovementLifecycleServiceImpl(movementWriterRepositoryMock, movementQueryRepoMock);

    useCase = new FindMovementsInputPort(movementLifecycleService, productLifecycleService);
  }

  @Test
  public void shouldReturnMovementsSortedDescending() throws BusinessException {
    final ProductDTO product =
        ProductDTO.defaultBuilder(
                TEST_USER_ID, TEST_PRODUCT_ID, "Test Account", ProductType.SAVINGS)
            .build();

    final LocalDate today = LocalDate.now();
    final MovementDTO movement1 =
        createMovement(MovementId.generate(), today.minusDays(5), new BigDecimal("100.00"));
    final MovementDTO movement2 =
        createMovement(MovementId.generate(), today.minusDays(10), new BigDecimal("200.00"));
    final MovementDTO movement3 =
        createMovement(MovementId.generate(), today.minusDays(1), new BigDecimal("50.00"));

    when(productLifecycleService.findOrThrowByUserAndProductId(TEST_USER_ID, TEST_PRODUCT_ID))
        .thenReturn(product);
    when(movementQueryRepoMock.getByUserAndProductIdWithinPeriod(
            eq(TEST_USER_ID), eq(TEST_PRODUCT_ID), any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(List.of(movement1, movement2, movement3));

    final List<MovementDTO> result =
        useCase.findMovementsByProduct(TEST_USER_ID, TEST_PRODUCT_ID, 3);

    assertNotNull(result);
    assertEquals(3, result.size());

    // Verify sorted descending by date (newest first)
    assertEquals(movement3.id(), result.get(0).id()); // today - 1
    assertEquals(movement1.id(), result.get(1).id()); // today - 5
    assertEquals(movement2.id(), result.get(2).id()); // today - 10

    verify(productLifecycleService).findOrThrowByUserAndProductId(TEST_USER_ID, TEST_PRODUCT_ID);
    verify(movementQueryRepoMock)
        .getByUserAndProductIdWithinPeriod(
            eq(TEST_USER_ID), eq(TEST_PRODUCT_ID), any(LocalDate.class), any(LocalDate.class));
  }

  private MovementDTO createMovement(
      final MovementId id, final LocalDate date, final BigDecimal amount) {
    return MovementDTO.builder()
        .id(id)
        .productId(TEST_PRODUCT_ID)
        .movementType(MovementType.DEPOSIT)
        .category(CategoryFixturesTestApp.INCOME_DEPOSIT)
        .movementAmount(amount)
        .movementDate(date)
        .balanceSnapshot(amount)
        .createdAt(LocalDateTime.now())
        .build();
  }

  @Test
  public void shouldUseDefaultMonthsBackWhenNull() throws BusinessException {
    final ProductDTO product =
        ProductDTO.defaultBuilder(
                TEST_USER_ID, TEST_PRODUCT_ID, "Test Account", ProductType.SAVINGS)
            .build();

    when(productLifecycleService.findOrThrowByUserAndProductId(TEST_USER_ID, TEST_PRODUCT_ID))
        .thenReturn(product);
    when(movementQueryRepoMock.getByUserAndProductIdWithinPeriod(
            eq(TEST_USER_ID), eq(TEST_PRODUCT_ID), any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(Collections.emptyList());

    final List<MovementDTO> result =
        useCase.findMovementsByProduct(TEST_USER_ID, TEST_PRODUCT_ID, null);

    assertNotNull(result);
    verify(movementQueryRepoMock)
        .getByUserAndProductIdWithinPeriod(
            eq(TEST_USER_ID), eq(TEST_PRODUCT_ID), any(LocalDate.class), any(LocalDate.class));
  }

  @Test
  public void shouldUseDefaultMonthsBackWhenZero() throws BusinessException {
    final ProductDTO product =
        ProductDTO.defaultBuilder(
                TEST_USER_ID, TEST_PRODUCT_ID, "Test Account", ProductType.SAVINGS)
            .build();

    when(productLifecycleService.findOrThrowByUserAndProductId(TEST_USER_ID, TEST_PRODUCT_ID))
        .thenReturn(product);
    when(movementQueryRepoMock.getByUserAndProductIdWithinPeriod(
            eq(TEST_USER_ID), eq(TEST_PRODUCT_ID), any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(Collections.emptyList());

    final List<MovementDTO> result =
        useCase.findMovementsByProduct(TEST_USER_ID, TEST_PRODUCT_ID, 0);

    assertNotNull(result);
    verify(movementQueryRepoMock)
        .getByUserAndProductIdWithinPeriod(
            eq(TEST_USER_ID), eq(TEST_PRODUCT_ID), any(LocalDate.class), any(LocalDate.class));
  }

  @Test
  public void shouldReturnEmptyListWhenNoMovements() throws BusinessException {
    final ProductDTO product =
        ProductDTO.defaultBuilder(
                TEST_USER_ID, TEST_PRODUCT_ID, "Test Account", ProductType.SAVINGS)
            .build();

    when(productLifecycleService.findOrThrowByUserAndProductId(TEST_USER_ID, TEST_PRODUCT_ID))
        .thenReturn(product);
    when(movementQueryRepoMock.getByUserAndProductIdWithinPeriod(
            eq(TEST_USER_ID), eq(TEST_PRODUCT_ID), any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(Collections.emptyList());

    final List<MovementDTO> result =
        useCase.findMovementsByProduct(TEST_USER_ID, TEST_PRODUCT_ID, 3);

    assertNotNull(result);
    assertEquals(0, result.size());

    verify(productLifecycleService).findOrThrowByUserAndProductId(TEST_USER_ID, TEST_PRODUCT_ID);
    verify(movementQueryRepoMock)
        .getByUserAndProductIdWithinPeriod(
            eq(TEST_USER_ID), eq(TEST_PRODUCT_ID), any(LocalDate.class), any(LocalDate.class));
  }

  @Test
  public void shouldThrowExceptionWhenUserIdIsNull() {
    final GenericSpecificationException exception =
        assertThrows(
            GenericSpecificationException.class,
            () -> useCase.findMovementsByProduct(null, TEST_PRODUCT_ID, 3));

    assertEquals("User ID cannot be null", exception.getMessage());
  }

  @Test
  public void shouldThrowExceptionWhenProductIdIsNull() {
    final GenericSpecificationException exception =
        assertThrows(
            GenericSpecificationException.class,
            () -> useCase.findMovementsByProduct(TEST_USER_ID, null, 3));

    assertEquals("Product ID cannot be null", exception.getMessage());
  }

  @Test
  public void shouldThrowExceptionWhenProductDoesNotExist() throws BusinessException {
    when(productLifecycleService.findOrThrowByUserAndProductId(TEST_USER_ID, TEST_PRODUCT_ID))
        .thenThrow(new BusinessException(BusinessApplicationExceptionType.PRODUCT_NOT_FOUND));

    assertThrows(
        BusinessException.class,
        () -> useCase.findMovementsByProduct(TEST_USER_ID, TEST_PRODUCT_ID, 3));

    verify(productLifecycleService).findOrThrowByUserAndProductId(TEST_USER_ID, TEST_PRODUCT_ID);
  }

  @Test
  public void shouldCalculateDateRangeCorrectly() throws BusinessException {
    final ProductDTO product =
        ProductDTO.defaultBuilder(
                TEST_USER_ID, TEST_PRODUCT_ID, "Test Account", ProductType.SAVINGS)
            .build();

    when(productLifecycleService.findOrThrowByUserAndProductId(TEST_USER_ID, TEST_PRODUCT_ID))
        .thenReturn(product);
    when(movementQueryRepoMock.getByUserAndProductIdWithinPeriod(
            eq(TEST_USER_ID), eq(TEST_PRODUCT_ID), any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(Collections.emptyList());

    useCase.findMovementsByProduct(TEST_USER_ID, TEST_PRODUCT_ID, 3);

    // Verify date range calculation
    final YearMonth currentMonth = YearMonth.now();
    final LocalDate expectedEndDate = currentMonth.atEndOfMonth();
    final LocalDate expectedStartDate = currentMonth.minusMonths(2).atDay(1);

    verify(movementQueryRepoMock)
        .getByUserAndProductIdWithinPeriod(
            TEST_USER_ID, TEST_PRODUCT_ID, expectedStartDate, expectedEndDate);
  }

  @Test
  public void shouldHandleCustomMonthsBackValue() throws BusinessException {
    final ProductDTO product =
        ProductDTO.defaultBuilder(
                TEST_USER_ID, TEST_PRODUCT_ID, "Test Account", ProductType.SAVINGS)
            .build();

    when(productLifecycleService.findOrThrowByUserAndProductId(TEST_USER_ID, TEST_PRODUCT_ID))
        .thenReturn(product);
    when(movementQueryRepoMock.getByUserAndProductIdWithinPeriod(
            eq(TEST_USER_ID), eq(TEST_PRODUCT_ID), any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(Collections.emptyList());

    useCase.findMovementsByProduct(TEST_USER_ID, TEST_PRODUCT_ID, 6);

    // Verify date range calculation for 6 months
    final YearMonth currentMonth = YearMonth.now();
    final LocalDate expectedEndDate = currentMonth.atEndOfMonth();
    final LocalDate expectedStartDate = currentMonth.minusMonths(5).atDay(1);

    verify(movementQueryRepoMock)
        .getByUserAndProductIdWithinPeriod(
            TEST_USER_ID, TEST_PRODUCT_ID, expectedStartDate, expectedEndDate);
  }
}
