package com.jbh.finance.test.application.feature.monthlybalance.ports.input;

import static com.jbh.commons.util.JbhMoneyUtils.JBH_ZERO;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.category.services.CategoryService;
import com.jbh.finance.application.feature.monthlybalance.commands.AddMonthlyBalanceCommand;
import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.finance.application.feature.monthlybalance.ports.input.RegisterMonthlyBalanceInputPort;
import com.jbh.finance.application.feature.monthlybalance.services.MonthlyBalanceLifecycleService;
import com.jbh.finance.application.feature.monthlybalance.services.ProcessMonthlyBalanceService;
import com.jbh.finance.application.feature.movement.services.ProcessMovementService;
import com.jbh.finance.application.feature.product.services.ProductLifecycleService;
import com.jbh.finance.domain.product.vo.ProductId;
import com.jbh.finance.test.testfixtures.CategoryFixturesTestApp;
import com.jbh.finance.test.testfixtures.fakes.acid.RecordingUnitOfWork;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

@Tag("unit")
class RegisterMonthlyBalanceInputPortTest {

  private static final YearMonth PERIOD = YearMonth.of(2025, 3);
  private static final LocalDate RUNNING_DATE = LocalDate.of(2025, 5, 1);

  private MonthlyBalanceLifecycleService monthlyBalanceService;
  private ProductLifecycleService productService;
  private ProcessMovementService movementService;
  private ProcessMonthlyBalanceService processMonthlyBalanceService;
  private RecordingUnitOfWork unitOfWork;
  private RegisterMonthlyBalanceInputPort inputPort;

  private final UUID userId = UUID.randomUUID();
  private final ProductId accountId = ProductId.generate();
  private final AddMonthlyBalanceCommand command =
      new AddMonthlyBalanceCommand(PERIOD, new BigDecimal("100.00"), null, null);

  @BeforeEach
  void setUp() {
    monthlyBalanceService = mock(MonthlyBalanceLifecycleService.class);
    productService = mock(ProductLifecycleService.class);
    movementService = mock(ProcessMovementService.class);
    processMonthlyBalanceService = mock(ProcessMonthlyBalanceService.class);
    unitOfWork = new RecordingUnitOfWork();

    final CategoryService categoryService = mock(CategoryService.class);
    when(categoryService.findIncomeInitialBalance())
        .thenReturn(CategoryFixturesTestApp.INCOME_INITIAL_BALANCE);

    inputPort =
        new RegisterMonthlyBalanceInputPort(
            monthlyBalanceService,
            productService,
            movementService,
            categoryService,
            unitOfWork,
            processMonthlyBalanceService);
  }

  @Test
  void shouldLockProductBeforeReadingMonthWhenRegistering() throws BusinessException {
    // Arrange
    when(monthlyBalanceService.findByAccountIdYearAndMonth(accountId, 2025, 3))
        .thenReturn(Optional.of(existingMonth()));

    // Act
    inputPort.registerOfficialMonthlyBalance(RUNNING_DATE, userId, accountId, command);

    // Assert
    final InOrder inOrder = inOrder(productService, monthlyBalanceService);
    inOrder.verify(productService).lockForUpdate(accountId);
    inOrder.verify(monthlyBalanceService).findByAccountIdYearAndMonth(accountId, 2025, 3);
  }

  @Test
  void shouldNotAwaitPendingSyncWhenMonthExists() throws BusinessException {
    // Arrange
    when(monthlyBalanceService.findByAccountIdYearAndMonth(accountId, 2025, 3))
        .thenReturn(Optional.of(existingMonth()));

    // Act
    inputPort.registerOfficialMonthlyBalance(RUNNING_DATE, userId, accountId, command);

    // Assert
    verify(processMonthlyBalanceService, never()).awaitPendingSync();
  }

  @Test
  void shouldAwaitPendingSyncBetweenTransactionsWhenFirstReport() throws BusinessException {
    // Arrange
    final AtomicBoolean inTransactionOnAwait = new AtomicBoolean(true);
    stubFirstReport(inTransactionOnAwait);

    // Act
    inputPort.registerOfficialMonthlyBalance(RUNNING_DATE, userId, accountId, command);

    // Assert
    final InOrder inOrder =
        inOrder(movementService, processMonthlyBalanceService, productService, monthlyBalanceService);
    inOrder.verify(movementService).addMovementToProduct(any(), any());
    inOrder.verify(processMonthlyBalanceService).awaitPendingSync();
    inOrder.verify(productService).lockForUpdate(accountId);
    inOrder.verify(monthlyBalanceService).findByAccountIdYearAndMonth(accountId, 2025, 3);
    verify(processMonthlyBalanceService, times(1)).awaitPendingSync();
    assertFalse(inTransactionOnAwait.get());
  }

  @Test
  void shouldNotSleepWhenFirstReport() {
    // Arrange
    stubFirstReport(new AtomicBoolean());

    // Act and Assert: the old code slept 2 seconds here
    assertTimeout(
        Duration.ofMillis(500),
        () -> {
          inputPort.registerOfficialMonthlyBalance(RUNNING_DATE, userId, accountId, command);
        });
  }

  private void stubFirstReport(final AtomicBoolean inTransactionOnAwait) {
    when(monthlyBalanceService.findByAccountIdYearAndMonth(eq(accountId), eq(2025), eq(3)))
        .thenReturn(Optional.empty(), Optional.of(existingMonth()));
    doAnswer(
            invocation -> {
              inTransactionOnAwait.set(unitOfWork.isInTransaction());
              return null;
            })
        .when(processMonthlyBalanceService)
        .awaitPendingSync();
  }

  private MonthlyBalanceDTO existingMonth() {
    return MonthlyBalanceDTO.withInitialDataForNextMonth(accountId, PERIOD, JBH_ZERO, false);
  }
}
