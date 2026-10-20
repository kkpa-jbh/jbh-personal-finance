package com.jbh.finance.test.infra.movement;

import static com.jbh.commons.util.JbhMoneyUtils.withJBHDecimals;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.util.JbhMoneyUtils;
import com.jbh.finance.application.acid.UnitOfWork;
import com.jbh.finance.application.feature.category.dto.CategoryDTO;
import com.jbh.finance.application.feature.category.services.CategoryService;
import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.finance.application.feature.monthlybalance.services.ProcessMonthlyBalanceService;
import com.jbh.finance.application.feature.movement.commands.AddMovementCommand;
import com.jbh.finance.application.feature.movement.dto.AddMovementResultDTO;
import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.application.feature.movement.services.MovementLifecycleService;
import com.jbh.finance.application.feature.movement.services.ProcessMovementService;
import com.jbh.finance.application.feature.product.commands.CreateProductCommand;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.application.feature.product.ports.input.CreateProductInputPort;
import com.jbh.finance.application.feature.product.services.ProductLifecycleService;
import com.jbh.finance.domain.movement.vo.MovementType;
import com.jbh.finance.domain.product.vo.ProductId;
import com.jbh.finance.domain.product.vo.ProductMetadata;
import com.jbh.finance.domain.product.vo.ProductPK;
import com.jbh.finance.domain.product.vo.ProductType;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;

/**
 * Proves the product row lock on a real PostgreSQL. The lock lives in the database, so two threads
 * of one JVM, each in its own transaction, behave like two instances of the app.
 *
 * <p>A race does not lose an update on every run when the lock is missing. The repetitions make a
 * miss very unlikely.
 */
@QuarkusTest
@TestProfile(FinanceDatabaseTestProfile.class)
@Tag("database")
class ProcessMovementServiceDatabaseLockTest {

  private static final int REPETITIONS = 5;
  private static final int RACING_CALLS = 2;
  private static final long READY_TIMEOUT_SECONDS = 10;
  private static final long CALL_TIMEOUT_SECONDS = 30;

  private static final String PRODUCT_NAME = "lock-test";
  private static final LocalDate SEED_DATE = LocalDate.of(2025, 2, 1);
  private static final YearMonth RACE_PERIOD = YearMonth.of(2025, 3);
  private static final LocalDate RACE_DATE = RACE_PERIOD.atDay(1);
  private static final BigDecimal SEED_AMOUNT = new BigDecimal("100");
  private static final BigDecimal DEPOSIT_AMOUNT = new BigDecimal("5");
  private static final BigDecimal WITHDRAWAL_AMOUNT = new BigDecimal("3");

  @Inject ProcessMovementService processMovementService;
  @Inject ProcessMonthlyBalanceService processMonthlyBalanceService;
  @Inject CreateProductInputPort createProductInputPort;
  @Inject ProductLifecycleService productLifecycleService;
  @Inject MovementLifecycleService movementLifecycleService;
  @Inject UnitOfWork unitOfWork;

  // Resolved inside a transaction: the service reads the categories when it is built, and its
  // repository has no transaction of its own. The REST request context covers that in production.
  @Inject Instance<CategoryService> categoryServices;

  private CategoryDTO incomeOther;
  private CategoryDTO expenseUnknown;

  @BeforeEach
  void loadCategories() throws BusinessException {
    incomeOther = unitOfWork.executeInTransaction(() -> categoryServices.get().findIncomeOther());
    expenseUnknown =
        unitOfWork.executeInTransaction(() -> categoryServices.get().findExpenseUnknown());
  }

  @RepeatedTest(REPETITIONS)
  void shouldKeepBothMovementsWhenTwoTransactionsAddToTheSameMonthAtTheSameTime()
      throws BusinessException, InterruptedException, ExecutionException, TimeoutException {
    // Arrange
    final ProductPK productPK = createSavingsProduct();
    seedProduct(productPK);

    // Act
    raceTwoMovements(
        productPK,
        command(MovementType.DEPOSIT, DEPOSIT_AMOUNT),
        command(MovementType.WITHDRAWAL, WITHDRAWAL_AMOUNT));
    processMonthlyBalanceService.awaitPendingSync();

    // Assert
    final ProductId productId = productPK.productId();
    final MonthlyBalanceDTO month =
        unitOfWork.executeInTransaction(
            () ->
                processMonthlyBalanceService
                    .findByAccountIdAndPeriod(productId, RACE_PERIOD)
                    .orElseThrow());
    assertEquals(withJBHDecimals("5"), month.totalDebits());
    assertEquals(withJBHDecimals("3"), month.totalCredits());
    assertEquals(withJBHDecimals("2"), month.movementBalance());
    assertEquals(RACING_CALLS, month.totalMovements());

    final ProductDTO product =
        unitOfWork.executeInTransaction(
            () -> productLifecycleService.findOrThrowByIdProductId(productId));
    assertEquals(withJBHDecimals("102"), product.currentBalance());
    assertEquals(withJBHDecimals("102"), product.movementBalance());

    final List<BigDecimal> movementAmounts =
        unitOfWork.executeInTransaction(
            () ->
                movementLifecycleService.findByAccountId(productId).stream()
                    .map(MovementDTO::movementAmount)
                    .map(JbhMoneyUtils::withJBHDecimals)
                    .sorted()
                    .toList());
    assertEquals(
        List.of(withJBHDecimals("-3"), withJBHDecimals("5"), withJBHDecimals("100")),
        movementAmounts);
  }

  private ProductPK createSavingsProduct() throws BusinessException {
    final UUID userId = UUID.randomUUID();
    final ProductDTO product =
        createProductInputPort.execute(
            new CreateProductCommand(
                userId, PRODUCT_NAME, ProductType.SAVINGS, ProductMetadata.empty()));
    return new ProductPK(userId, product.id());
  }

  /**
   * Puts money in the product before the race. Without it, a withdrawal that runs first on a zero
   * balance marks the product fully withdrawn, and the product values would depend on thread order.
   */
  private void seedProduct(final ProductPK productPK) throws BusinessException {
    final AddMovementCommand seed =
        AddMovementCommand.builder()
            .entryDate(SEED_DATE)
            .totalAmount(SEED_AMOUNT)
            .movementType(MovementType.DEPOSIT)
            .categoryDTO(incomeOther)
            .build();
    // In a transaction, so the lazy movement service is built with one open (it reads categories).
    unitOfWork.executeInTransaction(
        () -> processMovementService.addMovementToProduct(productPK, seed));
    processMonthlyBalanceService.awaitPendingSync();
  }

  private AddMovementCommand command(final MovementType movementType, final BigDecimal amount) {
    final CategoryDTO category =
        movementType == MovementType.DEPOSIT ? incomeOther : expenseUnknown;
    return AddMovementCommand.builder()
        .entryDate(RACE_DATE)
        .totalAmount(amount)
        .movementType(movementType)
        .categoryDTO(category)
        .build();
  }

  /** Releases both calls at the same moment. Each one opens its own transaction on its thread. */
  private void raceTwoMovements(
      final ProductPK productPK, final AddMovementCommand first, final AddMovementCommand second)
      throws InterruptedException, ExecutionException, TimeoutException {
    final StartGate gate = new StartGate(new CountDownLatch(RACING_CALLS), new CountDownLatch(1));
    final ExecutorService threads = Executors.newFixedThreadPool(RACING_CALLS);
    try {
      final Future<AddMovementResultDTO> firstCall =
          threads.submit(gatedCall(productPK, first, gate));
      final Future<AddMovementResultDTO> secondCall =
          threads.submit(gatedCall(productPK, second, gate));

      assertTrue(
          gate.ready().await(READY_TIMEOUT_SECONDS, SECONDS), "Both threads must reach the gate");
      gate.start().countDown();

      firstCall.get(CALL_TIMEOUT_SECONDS, SECONDS);
      secondCall.get(CALL_TIMEOUT_SECONDS, SECONDS);
    } finally {
      threads.shutdownNow();
    }
  }

  private Callable<AddMovementResultDTO> gatedCall(
      final ProductPK productPK, final AddMovementCommand command, final StartGate gate) {
    return () -> {
      gate.ready().countDown();
      gate.start().await();
      return processMovementService.addMovementToProduct(productPK, command);
    };
  }

  private record StartGate(CountDownLatch ready, CountDownLatch start) {}
}
