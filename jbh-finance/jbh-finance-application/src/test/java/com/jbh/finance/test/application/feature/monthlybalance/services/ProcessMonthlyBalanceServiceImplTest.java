package com.jbh.finance.test.application.feature.monthlybalance.services;

import static com.jbh.commons.util.JbhMoneyUtils.withJBHDecimals;
import static com.jbh.finance.domain.movement.vo.MovementType.DEPOSIT;
import static com.jbh.finance.domain.movement.vo.MovementType.WITHDRAWAL;
import static com.jbh.finance.test.testfixtures.CategoryFixturesTestApp.EXPENSE_UNKNOWN;
import static com.jbh.finance.test.testfixtures.CategoryFixturesTestApp.OTHER_INCOME_MOVEMENT;
import static com.jbh.finance.test.testfixtures.builders.commands.AddMovementCommandFixtureBuilder.createMovementWithType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.acid.UnitOfWork;
import com.jbh.finance.application.acid.UnitOfWork.TransactionalWork;
import com.jbh.finance.application.async.AsyncTaskExecutor;
import com.jbh.finance.application.async.AsyncTaskExecutorImpl;
import com.jbh.finance.application.async.vo.AsyncTask;
import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.finance.application.feature.monthlybalance.services.MonthlyBalanceLifecycleService;
import com.jbh.finance.application.feature.monthlybalance.services.MonthlyBalanceLifecycleServiceImpl;
import com.jbh.finance.application.feature.monthlybalance.services.ProcessMonthlyBalanceServiceImpl;
import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.application.feature.movement.mappers.MovementMapper;
import com.jbh.finance.application.feature.product.services.ProductLifecycleService;
import com.jbh.finance.domain.movement.vo.MovementType;
import com.jbh.finance.domain.product.vo.ProductId;
import com.jbh.finance.test.testfixtures.fakes.monthlybalance.InMemoryMonthlyBalanceRepositories;
import com.jbh.finance.test.testfixtures.utils.UnitOfWorkTest;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

@Tag("integration")
class ProcessMonthlyBalanceServiceImplTest {

  private static final YearMonth PERIOD = YearMonth.of(2025, 3);
  private static final LocalDate MOVEMENT_DATE = PERIOD.atDay(1);

  private ProductId productId;
  private InMemoryMonthlyBalanceRepositories repos;
  private MonthlyBalanceLifecycleService lifecycleService;
  private ProductLifecycleService productService;
  private final UnitOfWork unitOfWork = new UnitOfWorkTest();

  @BeforeEach
  void setUp() {
    productId = ProductId.generate();
    repos = new InMemoryMonthlyBalanceRepositories();
    lifecycleService =
        new MonthlyBalanceLifecycleServiceImpl(repos.getQueryRepo(), repos.getWriterRepo());
    productService = mock(ProductLifecycleService.class);
  }

  @Test
  void shouldKeepBothMovementsWhenTwoMovementsAreSyncedInTheSameMonth() throws BusinessException {
    // Arrange
    final ProcessMonthlyBalanceServiceImpl service =
        new ProcessMonthlyBalanceServiceImpl(
            lifecycleService, new AsyncTaskExecutorImpl(), productService, unitOfWork);
    final MovementDTO deposit = movement(DEPOSIT, "5");
    final MovementDTO withdrawal = movement(WITHDRAWAL, "3");

    // Act
    unitOfWork.executeInTransaction(() -> service.syncForNewMovement(deposit));
    unitOfWork.executeInTransaction(() -> service.syncForNewMovement(withdrawal));
    service.awaitPendingSync();

    // Assert
    final MonthlyBalanceDTO stored = storedBalance();
    assertEquals(withJBHDecimals(new BigDecimal("5")), stored.totalDebits());
    assertEquals(withJBHDecimals(new BigDecimal("3")), stored.totalCredits());
    assertEquals(withJBHDecimals(new BigDecimal("2")), stored.movementBalance());
    assertEquals(2, stored.totalMovements());
  }

  @Test
  void shouldSaveMonthlyBalanceBeforeReturningWhenExecutorNeverRuns() throws BusinessException {
    // Arrange
    final RecordingExecutor executor = RecordingExecutor.dropTasks();
    final ProcessMonthlyBalanceServiceImpl service =
        new ProcessMonthlyBalanceServiceImpl(lifecycleService, executor, productService, unitOfWork);

    // Act
    unitOfWork.executeInTransaction(() -> service.syncForNewMovement(movement(DEPOSIT, "5")));

    // Assert
    final MonthlyBalanceDTO stored = storedBalance();
    assertEquals(withJBHDecimals(new BigDecimal("5")), stored.totalDebits());
    assertEquals(1, stored.totalMovements());
    assertEquals(1, executor.submittedTasks());
  }

  @Test
  void shouldRemoveReversedMovementFromStoredMonthWhenReversalIsSynced() throws BusinessException {
    // Arrange
    final RecordingExecutor executor = RecordingExecutor.dropTasks();
    final ProcessMonthlyBalanceServiceImpl service =
        new ProcessMonthlyBalanceServiceImpl(lifecycleService, executor, productService, unitOfWork);
    final MovementDTO withdrawal = movement(WITHDRAWAL, "4");

    // Act
    unitOfWork.executeInTransaction(() -> service.syncForNewMovement(movement(DEPOSIT, "10")));
    unitOfWork.executeInTransaction(() -> service.syncForNewMovement(withdrawal));
    unitOfWork.executeInTransaction(() -> service.syncForReversedMovement(withdrawal));

    // Assert
    final MonthlyBalanceDTO stored = storedBalance();
    assertEquals(withJBHDecimals(new BigDecimal("10")), stored.totalDebits());
    assertEquals(withJBHDecimals(BigDecimal.ZERO), stored.totalCredits());
    assertEquals(withJBHDecimals(new BigDecimal("10")), stored.movementBalance());
    assertEquals(1, stored.totalMovements());
  }

  @Test
  void shouldPropagateExceptionWhenSaveFails() {
    // Arrange
    final MonthlyBalanceLifecycleService failingLifecycle =
        mock(MonthlyBalanceLifecycleService.class);
    doThrow(new IllegalStateException("save failed")).when(failingLifecycle).saveMultiBalances(any());
    final RecordingExecutor executor = RecordingExecutor.dropTasks();
    final ProcessMonthlyBalanceServiceImpl service =
        new ProcessMonthlyBalanceServiceImpl(failingLifecycle, executor, productService, unitOfWork);
    final MovementDTO deposit = movement(DEPOSIT, "5");

    // Act and Assert
    assertThrows(IllegalStateException.class, () -> service.syncForNewMovement(deposit));
    assertEquals(0, executor.submittedTasks());
  }

  @Test
  void shouldLockProductBeforeReadingWhenAdjustRuns() throws BusinessException {
    // Arrange
    final UnitOfWork mockUnitOfWork = mock(UnitOfWork.class);
    when(mockUnitOfWork.executeInTransaction(any()))
        .thenAnswer(invocation -> invocation.<TransactionalWork<?>>getArgument(0).run());
    final MonthlyBalanceLifecycleService mockLifecycle = mock(MonthlyBalanceLifecycleService.class);
    final ProductLifecycleService mockProductService = mock(ProductLifecycleService.class);
    final ProcessMonthlyBalanceServiceImpl service =
        new ProcessMonthlyBalanceServiceImpl(
            mockLifecycle, RecordingExecutor.runOnCallingThread(), mockProductService, mockUnitOfWork);

    // Act
    service.syncForNewMovement(movement(DEPOSIT, "5"));

    // Assert
    final InOrder inOrder = inOrder(mockUnitOfWork, mockProductService, mockLifecycle);
    inOrder.verify(mockUnitOfWork).executeInTransaction(any());
    inOrder.verify(mockProductService).lockForUpdate(productId);
    inOrder.verify(mockLifecycle).findNextBalancesFromPeriodInclusive(productId, PERIOD);
  }

  private MonthlyBalanceDTO storedBalance() {
    return repos.getQueryRepo().findByAccountIdAndPeriod(productId, PERIOD).orElseThrow();
  }

  private MovementDTO movement(final MovementType type, final String amount) {
    final var category = type == DEPOSIT ? OTHER_INCOME_MOVEMENT : EXPENSE_UNKNOWN;
    return MovementMapper.fromCommand(
        productId, createMovementWithType(MOVEMENT_DATE, new BigDecimal(amount), type, category));
  }

  /** Executor double: counts submitted tasks, and either drops them or runs them at once. */
  private static final class RecordingExecutor implements AsyncTaskExecutor {

    private final boolean runTasks;
    private final List<AsyncTask> submitted = new ArrayList<>();

    private RecordingExecutor(final boolean runTasks) {
      this.runTasks = runTasks;
    }

    static RecordingExecutor dropTasks() {
      return new RecordingExecutor(false);
    }

    static RecordingExecutor runOnCallingThread() {
      return new RecordingExecutor(true);
    }

    int submittedTasks() {
      return submitted.size();
    }

    @Override
    public <T> CompletableFuture<T> submitTask(final AsyncTask asyncTask, final Callable<T> task) {
      submitted.add(asyncTask);
      final CompletableFuture<T> future = new CompletableFuture<>();
      if (runTasks) {
        try {
          future.complete(task.call());
        } catch (final Exception e) {
          future.completeExceptionally(e);
        }
      }
      return future;
    }

    @Override
    public void awaitIdle(final Duration timeout) {
      // Nothing is pending in this double.
    }
  }
}
