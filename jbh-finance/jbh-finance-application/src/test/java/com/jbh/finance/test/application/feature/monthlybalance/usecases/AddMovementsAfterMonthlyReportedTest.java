package com.jbh.finance.test.application.feature.monthlybalance.usecases;

import static com.jbh.commons.util.JbhMoneyUtils.withJBHDecimals;
import static com.jbh.finance.test.testfixtures.utils.MonthlyBalanceITUtils.assertMonthlyBalance;
import static com.jbh.finance.test.testfixtures.utils.MonthlyBalanceITUtils.createMonthlyBalanceCommand;
import static com.jbh.finance.test.testfixtures.builders.CommandTestBuilder.createBasicAccountCommand;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.category.dto.CategoryDTO;
import com.jbh.finance.application.feature.monthlybalance.commands.AddMonthlyBalanceCommand;
import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.finance.application.feature.monthlybalance.services.MonthlyBalanceLifecycleService;
import com.jbh.finance.application.feature.monthlybalance.usecases.RegisterMonthlyBalanceUseCase;
import com.jbh.finance.application.feature.movement.commands.AddMovementCommand;
import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.application.feature.movement.ports.output.MovementWriterRepository;
import com.jbh.finance.application.feature.movement.usecases.AddMovementUseCase;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.application.feature.product.usecases.CreateProductUseCase;
import com.jbh.finance.domain.category.vo.CategoryTypeVO;
import com.jbh.finance.domain.product.vo.ProductId;
import com.jbh.finance.domain.product.vo.ProductType;
import com.jbh.finance.test.testfixtures.fakes.product.InMemoryProductRepository;
import com.jbh.finance.test.testfixtures.CategoryFixturesTestApp;
import com.jbh.finance.test.testfixtures.builders.EntityTestBuilder;
import com.jbh.finance.test.testfixtures.builders.UseCaseBuilder;
import com.jbh.finance.test.testfixtures.builders.commands.AddMovementCommandTestBuilder;
import com.jbh.finance.test.testfixtures.builders.commands.MonthlyBalanceCommandFixture;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TestMethodOrder(OrderAnnotation.class)
public class AddMovementsAfterMonthlyReportedTest {
  static final UUID userId = UUID.randomUUID();
  private static final Logger LOG =
      LoggerFactory.getLogger(AddMovementsAfterMonthlyReportedTest.class);

  private static final String PRODUCT_REPORTED_NAME = "Reported";
  private static final List<BigDecimal> withdrawalMovements =
      List.of(withJBHDecimals(new BigDecimal("5")));
  private static final List<BigDecimal> depositMovements =
      List.of(withJBHDecimals(new BigDecimal("10")));

  static CreateProductUseCase createAccountUseCase;
  static ProductDTO createdAccount;
  static ProductId productId;
  static MonthlyBalanceLifecycleService monthlyBalanceService;
  static YearMonth reportedPeriod = YearMonth.of(2024, 7);
  static MonthlyBalanceCommandFixture reportedPeriodAmounts =
      new MonthlyBalanceCommandFixture(
          withJBHDecimals(new BigDecimal("1010")), withJBHDecimals(new BigDecimal("10")));
  private static ProductDTO finalExpectedAccountBalance;
  private static MonthlyBalanceDTO officialReportedBalance;
  private static InMemoryProductRepository inMemoryAccountRepo;
  RegisterMonthlyBalanceUseCase useCaseTest;
  LocalDate runningDate = LocalDate.now();
  @Mock private MovementWriterRepository accountMovementRepository;
  private AddMovementUseCase addMovementUseCase;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    monthlyBalanceService = UseCaseBuilder.buildMonthlyBalanceService();
    inMemoryAccountRepo = UseCaseBuilder.getProductRepoInMemory();

    useCaseTest = UseCaseBuilder.buildRegisterMonthlyBalanceUseCase(accountMovementRepository);

    createAccountUseCase = UseCaseBuilder.buildCreateAccountUseCase();

    addMovementUseCase = UseCaseBuilder.buildAddMovementUseCase(accountMovementRepository);
  }

  @Test
  @Order(0)
  void creatingAccount() throws BusinessException {
    createdAccount =
        createAccountUseCase.execute(
            createBasicAccountCommand(userId, PRODUCT_REPORTED_NAME, ProductType.SAVINGS));
    productId = createdAccount.id();
    LOG.info("Account created with id {}", productId);
    assert productId != null;

    // Creating Initial Balance
    final var initialBalance = withJBHDecimals(new BigDecimal("900"));
    final AddMonthlyBalanceCommand previousCommand =
        createMonthlyBalanceCommand(
            reportedPeriod.plusMonths(-1),
            new MonthlyBalanceCommandFixture(withJBHDecimals(initialBalance), null));

    useCaseTest.registerOfficialMonthlyBalance(runningDate, userId, productId, previousCommand);

    verify(accountMovementRepository).save(any(MovementDTO.class));

    finalExpectedAccountBalance = inMemoryAccountRepo.findByProductId(productId).get();
    assertEquals(initialBalance, finalExpectedAccountBalance.currentBalance(), "Account Balance");
    assertEquals(initialBalance, finalExpectedAccountBalance.movementBalance(), "Movement Balance");

    final AddMonthlyBalanceCommand command =
        createMonthlyBalanceCommand(reportedPeriod, reportedPeriodAmounts);
    final AtomicReference<MonthlyBalanceDTO> savedInitialMonthlyBalance = new AtomicReference<>();
    Assertions.assertDoesNotThrow(
        () ->
            savedInitialMonthlyBalance.set(
                useCaseTest.registerOfficialMonthlyBalance(
                    runningDate, userId, productId, command)));

    officialReportedBalance = savedInitialMonthlyBalance.get();
    assertEquals(new BigDecimal("12.22"), officialReportedBalance.netGrowthRate());
    assertEquals(initialBalance, officialReportedBalance.openingBalance());
    assertEquals(
        command.closingBalance().subtract(command.monthlyProfitReported()),
        officialReportedBalance.closingBalance());
    assertEquals(command.monthlyProfitReported(), officialReportedBalance.monthlyReportedProfit());
    assertEquals(command.monthlyProfitReported(), officialReportedBalance.monthlyNetProfit());

    finalExpectedAccountBalance = inMemoryAccountRepo.findByProductId(productId).get();
    assertEquals(
        command.closingBalance(), finalExpectedAccountBalance.currentBalance(), "Account Balance");
    assertEquals(
        command.monthlyProfitReported().add(initialBalance),
        finalExpectedAccountBalance.movementBalance(),
        "Account Balance");
    assertEquals(new BigDecimal("10.00"), finalExpectedAccountBalance.netProfitBalance());
  }

  @Test
  @Order(1)
  void addingWithdrawal1() {
    final BigDecimal withDrawal1 = withdrawalMovements.get(0);
    addMovement(
        reportedPeriod, CategoryFixturesTestApp.UNKNOWN_EXPENSE.getType(), withDrawal1, null);

    final MonthlyBalanceDTO currentMonthlyBalance =
        monthlyBalanceService.findByAccountIdAndPeriod(productId, reportedPeriod).get();
    final var expectedMonthlyBalance =
        EntityTestBuilder.withClosingBalance(
                productId, reportedPeriod, officialReportedBalance.closingBalance())
            .totalDebits(officialReportedBalance.totalDebits())
            .totalCredits(officialReportedBalance.totalCredits().add(withDrawal1))
            .openingBalance(officialReportedBalance.openingBalance())
            .monthlyReportedProfit(officialReportedBalance.monthlyReportedProfit())
            .monthlyNetProfit(officialReportedBalance.monthlyNetProfit())
            .movementBalance(withJBHDecimals(new BigDecimal("-5.00")))
            .totalMovements(1)
            .officialMonthlyReport(true)
            .build();

    assertMonthlyBalance(expectedMonthlyBalance, currentMonthlyBalance);
    assertTrue(
        currentMonthlyBalance.netGrowthRate().compareTo(officialReportedBalance.netGrowthRate())
            > 0);

    final ProductDTO currentAccountBalance = inMemoryAccountRepo.findByProductId(productId).get();
    assertEquals(
        finalExpectedAccountBalance.currentBalance(),
        currentAccountBalance.currentBalance(),
        "Account Balance");
    assertEquals(
        finalExpectedAccountBalance.netProfitBalance(), currentAccountBalance.netProfitBalance());
    assertEquals(
        finalExpectedAccountBalance.movementBalance().subtract(withDrawal1),
        currentAccountBalance.movementBalance());
  }

  private void addMovement(
      final YearMonth period,
      final CategoryTypeVO categoryType,
      final BigDecimal amount,
      final BigDecimal balanceSnapshot) {
    final AddMovementCommand movement =
        AddMovementCommandTestBuilder.withBalanceSnapshot(
            LocalDate.of(period.getYear(), period.getMonthValue(), 15),
            balanceSnapshot,
            CategoryDTO.withInternalPurpose(categoryType, null),
            amount);
    try {
      addMovementUseCase.addMovement(userId, productId, movement);
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
    try {
      Thread.sleep(Duration.ofSeconds(2).toMillis());
    } catch (final InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  @Order(2)
  void addingDeposit1() {

    final BigDecimal deposit = depositMovements.get(0);
    final BigDecimal withDrawal1 = withdrawalMovements.get(0);

    addMovement(reportedPeriod, CategoryFixturesTestApp.SALARY.getCategoryType(), deposit, null);

    final MonthlyBalanceDTO currentMonthlyBalance =
        monthlyBalanceService.findByAccountIdAndPeriod(productId, reportedPeriod).get();
    final var expectedMonthlyBalance =
        EntityTestBuilder.withClosingBalance(
                productId, reportedPeriod, officialReportedBalance.closingBalance())
            .totalDebits(officialReportedBalance.totalDebits().add(deposit))
            .totalCredits(officialReportedBalance.totalCredits().add(withDrawal1))
            .openingBalance(officialReportedBalance.openingBalance())
            .monthlyReportedProfit(officialReportedBalance.monthlyReportedProfit())
            .monthlyNetProfit(officialReportedBalance.monthlyNetProfit())
            .movementBalance(deposit.subtract(withDrawal1))
            .totalMovements(2)
            .officialMonthlyReport(true)
            .build();

    assertMonthlyBalance(expectedMonthlyBalance, currentMonthlyBalance);
    assertTrue(
        currentMonthlyBalance.netGrowthRate().compareTo(officialReportedBalance.netGrowthRate())
            < 0);

    final ProductDTO currentAccountBalance = inMemoryAccountRepo.findByProductId(productId).get();
    assertEquals(
        finalExpectedAccountBalance.currentBalance(),
        currentAccountBalance.currentBalance(),
        "Account Balance");
    assertEquals(
        finalExpectedAccountBalance.netProfitBalance(), currentAccountBalance.netProfitBalance());
    assertEquals(
        finalExpectedAccountBalance.movementBalance().add(deposit).subtract(withDrawal1),
        currentAccountBalance.movementBalance());
  }

  @Test
  @Order(2)
  public void addExcceededDepositsShouldThrowException() {
    final BigDecimal deposit = new BigDecimal("100");

    Assertions.assertThrows(
        RuntimeException.class,
        () ->
            addMovement(
                reportedPeriod, CategoryFixturesTestApp.SALARY.getCategoryType(), deposit, null));
  }

  @Test
  @Order(3)
  public void addExcceededWithdrawalsShouldThrowException() {
    final BigDecimal withdrawal1 = new BigDecimal("900");
    final BigDecimal withdrawal2 = new BigDecimal("10");

    // Nothing happens, the balance has enought still (5 more)
    addMovement(
        reportedPeriod, CategoryFixturesTestApp.UNKNOWN_EXPENSE.getType(), withdrawal1, null);

    Assertions.assertThrows(
        RuntimeException.class,
        () ->
            addMovement(
                reportedPeriod,
                CategoryFixturesTestApp.UNKNOWN_EXPENSE.getType(),
                withdrawal2,
                null));
  }
}
