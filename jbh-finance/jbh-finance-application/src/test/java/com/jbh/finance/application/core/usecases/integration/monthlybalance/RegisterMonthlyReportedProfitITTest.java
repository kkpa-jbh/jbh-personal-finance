package com.jbh.finance.application.core.usecases.integration.monthlybalance;

import static com.jbh.commons.util.JbhMoneyUtils.JBH_ZERO;
import static com.jbh.commons.util.JbhMoneyUtils.withJBHDecimals;
import static com.jbh.finance.application.builders.CommandTestBuilder.createBasicAccountCommand;
import static com.jbh.finance.application.builders.TestDataFactory.getAddMonthlyBalanceCommandsWithProfit;
import static com.jbh.finance.application.builders.UseCaseBuilder.DEFAULT_ACCOUNT_NAME;
import static com.jbh.finance.application.core.usecases.utils.MonthlyBalanceITUtils.assertMonthlyBalance;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.builders.EntityTestBuilder;
import com.jbh.finance.application.builders.UseCaseBuilder;
import com.jbh.finance.application.builders.commands.AddMovementCommandTestBuilder;
import com.jbh.finance.application.core.ports.output.product.InMemoryProductRepository;
import com.jbh.finance.application.feature.monthlybalance.commands.AddMonthlyBalanceCommand;
import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.finance.application.feature.monthlybalance.services.MonthlyBalanceLifecycleService;
import com.jbh.finance.application.feature.monthlybalance.usecases.RegisterMonthlyBalanceUseCase;
import com.jbh.finance.application.feature.movement.commands.AddMovementCommand;
import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.application.feature.movement.ports.output.MovementWriterRepository;
import com.jbh.finance.application.feature.movement.services.ProcessMovementServiceImpl;
import com.jbh.finance.application.feature.movement.usecases.AddMovementUseCase;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.application.feature.product.usecases.CreateProductUseCase;
import com.jbh.finance.domain.movement.vo.CategoryType;
import com.jbh.finance.domain.movement.vo.ExpenseCategory;
import com.jbh.finance.domain.movement.vo.IncomeCategory;
import com.jbh.finance.domain.movement.vo.MovementCategoryVO;
import com.jbh.finance.domain.product.vo.ProductId;
import com.jbh.finance.domain.product.vo.ProductType;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
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
public class RegisterMonthlyReportedProfitITTest {
  static final UUID userId = UUID.randomUUID();
  private static final Logger LOG =
      LoggerFactory.getLogger(RegisterMonthlyReportedWithoutProfitITTest.class);
  private static final InMemoryProductRepository inMemoryAccountRepo =
      UseCaseBuilder.getAccountRepository();

  static CreateProductUseCase createAccountUseCase;
  static ProductDTO createdAccount;
  static ProductId accountId;
  static int commandIndex = -1;
  static MonthlyBalanceLifecycleService monthlyBalanceService;
  private static MonthlyBalanceDTO finalReported20249;
  private static ProductDTO finalAccountBalance;
  private static MonthlyBalanceDTO finalReported202410;
  @Mock private static MovementWriterRepository accountMovementRepository;
  RegisterMonthlyBalanceUseCase useCaseTest;
  YearMonth initialPeriod = YearMonth.of(2024, 7);
  LocalDate runningDate = LocalDate.now();
  BigDecimal FIRST_BALANCE_ZERO = withJBHDecimals(new BigDecimal("1000"));
  List<AddMonthlyBalanceCommand> monthlyCommands =
      getAddMonthlyBalanceCommandsWithProfit(initialPeriod, FIRST_BALANCE_ZERO);
  ProcessMovementServiceImpl accountMovementService;
  private AddMovementUseCase addMovementUseCase;

  @BeforeAll
  static void beforeAll() {
    UseCaseBuilder.getInMemoryMonthlyBalanceRepos().clearStorage();
    UseCaseBuilder.getAccountRepository().clearStorage();
  }

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    monthlyBalanceService = UseCaseBuilder.buildMonthlyBalanceService();

    accountMovementService =
        UseCaseBuilder.buildAccountMovementApplicationService(accountMovementRepository);

    useCaseTest = UseCaseBuilder.buildRegisterMonthlyBalanceUseCase(accountMovementRepository);

    createAccountUseCase = UseCaseBuilder.buildCreateAccountUseCase();

    addMovementUseCase = UseCaseBuilder.buildAddMovementUseCase(accountMovementRepository);

    UseCaseBuilder.delayTests();
    UseCaseBuilder.delayTests();
  }

  @Test
  @Order(0)
  void settingInitialBalance() throws BusinessException {
    ++commandIndex;
    createdAccount =
        createAccountUseCase.execute(
            createBasicAccountCommand(userId, DEFAULT_ACCOUNT_NAME, ProductType.SAVINGS));
    accountId = createdAccount.id();
    LOG.info("Account created with id {}", accountId);
    assertNotNull(accountId);

    // Creating Initial Balance
    final AddMonthlyBalanceCommand command = getCurrentCommand();

    final MonthlyBalanceDTO savedMonthlyBalance =
        useCaseTest.registerOfficialMonthlyBalance(runningDate, userId, accountId, command);

    // Then
    final MonthlyBalanceDTO expectedInitialBalance =
        MonthlyBalanceDTO.defaultBuilder()
            .closingBalance(FIRST_BALANCE_ZERO)
            .movementBalance(FIRST_BALANCE_ZERO)
            .totalDebits(FIRST_BALANCE_ZERO)
            .productId(accountId)
            .period(initialPeriod)
            .year(initialPeriod.getYear())
            .month(initialPeriod.getMonthValue())
            .officialMonthlyReport(true)
            .totalMovements(1)
            .build();
    assertMonthlyBalance(expectedInitialBalance, savedMonthlyBalance);

    refreshActualAccountBalance();
    assertEquals(FIRST_BALANCE_ZERO, finalAccountBalance.currentBalance());
    assertEquals(FIRST_BALANCE_ZERO, finalAccountBalance.movementBalance());
  }

  private AddMonthlyBalanceCommand getCurrentCommand() {
    return monthlyCommands.get(commandIndex);
  }

  private static void refreshActualAccountBalance() {
    finalAccountBalance = inMemoryAccountRepo.findByProductId(accountId).get();
  }

  @Test
  @Order(1)
  public void month1ProfitReported202408() {
    ++commandIndex;
    // Given
    final AddMonthlyBalanceCommand command = getCurrentCommand();
    final AtomicReference<MonthlyBalanceDTO> savedMonthlyBalance = new AtomicReference<>();
    Assertions.assertDoesNotThrow(
        () ->
            savedMonthlyBalance.set(
                useCaseTest.registerOfficialMonthlyBalance(
                    runningDate, userId, accountId, command)));

    // Then Current Monthly Balance has closing balance - monthlyReportedProfit (50)
    final var actualMonthlyBalance = savedMonthlyBalance.get();
    final var monthlyProfitReported = command.monthlyProfitReported();
    final var expectedMonthlyBalance =
        EntityTestBuilder.withClosingBalance(
                accountId,
                command.monthlyPeriod(),
                command.closingBalance().subtract(monthlyProfitReported))
            .monthlyReportedProfit(monthlyProfitReported)
            .monthlyNetProfit(withJBHDecimals(monthlyProfitReported))
            .openingBalance(withJBHDecimals(getPreviousCommand().closingBalance()))
            .officialMonthlyReport(true)
            .build();
    assertMonthlyBalance(expectedMonthlyBalance, actualMonthlyBalance);
    assertEquals(new BigDecimal("5.00"), actualMonthlyBalance.netGrowthRate());

    // Verify that dividends movement was created
    verify(accountMovementRepository).save(any(MovementDTO.class));

    // Then Account Balance
    refreshActualAccountBalance();
    assertEquals(command.closingBalance(), finalAccountBalance.currentBalance(), "Account Balance");
    assertEquals(command.closingBalance(), finalAccountBalance.movementBalance());
    assertEquals(
        command.monthlyProfitReported(),
        finalAccountBalance.netProfitBalance(),
        "Net Profit Balance Account");
  }

  private AddMonthlyBalanceCommand getPreviousCommand() {
    return monthlyCommands.get(commandIndex - 1);
  }

  @Test
  @Order(2)
  public void month2ProfitReport20249() {
    ++commandIndex;
    final var previousAccountMovementBalance = finalAccountBalance.movementBalance();
    final var previousAccountNetProfitBalance = finalAccountBalance.netProfitBalance();

    final AddMonthlyBalanceCommand command = getCurrentCommand();
    final AddMonthlyBalanceCommand previousCommand = getPreviousCommand();

    final BigDecimal deposit2 = withJBHDecimals(new BigDecimal("100"));
    final BigDecimal snapshot2 = withJBHDecimals(new BigDecimal("1160"));
    final var period = command.monthlyPeriod();
    addMovement(period, IncomeCategory.SALARY, deposit2, snapshot2);

    LOG.info("Checking Current Balance Month2 after adding movement");
    final MonthlyBalanceDTO currentMonthlyBalance =
        monthlyBalanceService.findByAccountIdAndPeriod(accountId, period).get();

    final var previousDividends = previousCommand.monthlyProfitReported();
    final MonthlyBalanceDTO expectedMonthlyBalanceAfterMovement =
        EntityTestBuilder.withClosingBalance(accountId, period, snapshot2)
            .totalDebits(deposit2.add(previousDividends))
            .openingBalance(previousCommand.closingBalance().subtract(previousDividends))
            .movementBalance(deposit2.add(previousDividends))
            .totalMovements(1 + 1)
            .monthlyNetProfit(withJBHDecimals(new BigDecimal("10")))
            .netGrowthRate(withJBHDecimals(new BigDecimal("0.91")))
            .build();
    assertMonthlyBalance(expectedMonthlyBalanceAfterMovement, currentMonthlyBalance);

    LOG.warn("Testing the registration of the Monthly Balance for period {}", period);

    final AtomicReference<MonthlyBalanceDTO> savedMonthlyBalance = new AtomicReference<>();
    Assertions.assertDoesNotThrow(
        () ->
            savedMonthlyBalance.set(
                useCaseTest.registerOfficialMonthlyBalance(
                    runningDate, userId, accountId, command)));
    finalReported20249 = savedMonthlyBalance.get();

    verify(accountMovementRepository, times(2)).save((MovementDTO) any());

    LOG.info("Assertion for Monthly Balance for period {}", period);

    final var closingBalanceWithProfit =
        getCurrentCommand().closingBalance().subtract(command.monthlyProfitReported());
    final MonthlyBalanceDTO expectedReportedBalance =
        EntityTestBuilder.withClosingBalance(accountId, period, closingBalanceWithProfit)
            .officialMonthlyReport(true)
            .monthlyNetProfit(command.monthlyProfitReported())
            .monthlyReportedProfit(command.monthlyProfitReported())
            .totalDebits(expectedMonthlyBalanceAfterMovement.totalDebits())
            .openingBalance(expectedMonthlyBalanceAfterMovement.openingBalance())
            .movementBalance(expectedMonthlyBalanceAfterMovement.movementBalance())
            .totalMovements(expectedMonthlyBalanceAfterMovement.totalMovements())
            .netGrowthRate(withJBHDecimals(new BigDecimal("30.00")))
            .build();

    assertMonthlyBalance(expectedReportedBalance, finalReported20249);

    // Verifying the opening balance for the next month
    final MonthlyBalanceDTO nextMonthBalance =
        monthlyBalanceService.findByAccountIdAndPeriod(accountId, period.plusMonths(1)).get();
    assertEquals(withJBHDecimals(closingBalanceWithProfit), nextMonthBalance.openingBalance());
    // Verifying dividends movement as total debits for the next month
    assertEquals(command.monthlyProfitReported(), nextMonthBalance.totalDebits());
    assertEquals(command.monthlyProfitReported(), nextMonthBalance.movementBalance());
    assertFalse(nextMonthBalance.officialMonthlyReport());

    // Then Account Balance

    refreshActualAccountBalance();
    assertEquals(command.closingBalance(), finalAccountBalance.currentBalance(), "Account Balance");
    assertEquals(
        previousDividends.add(command.monthlyProfitReported()),
        finalAccountBalance.netProfitBalance(),
        "Net Profit Account Balance");
  }

  private void addMovement(
      final YearMonth period,
      final CategoryType categoryType,
      final BigDecimal amount,
      final BigDecimal balanceSnapshot) {
    final AddMovementCommand movement =
        AddMovementCommandTestBuilder.withBalanceSnapshot(
            LocalDate.of(period.getYear(), period.getMonthValue(), 15),
            balanceSnapshot,
            MovementCategoryVO.withType(categoryType),
            amount);
    try {
      addMovementUseCase.addMovement(userId, accountId, movement);
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
  @Order(3)
  void month3ProfitReport202410() {
    ++commandIndex;
    final var previousAccountMovementBalance = finalAccountBalance.movementBalance();
    final var previousAccountNetProfitBalance = finalAccountBalance.netProfitBalance();
    final AddMonthlyBalanceCommand command = getCurrentCommand();
    final AddMonthlyBalanceCommand previousCommand = getPreviousCommand();
    final var period = command.monthlyPeriod();

    final MonthlyBalanceDTO initialMonthlyBalance =
        monthlyBalanceService.findByAccountIdAndPeriod(accountId, period).get();

    final BigDecimal withdrawal = withJBHDecimals(new BigDecimal("300"));
    final BigDecimal snapshotWithdrawal = withJBHDecimals(new BigDecimal("900"));

    addMovement(period, ExpenseCategory.PERSONAL, withdrawal, snapshotWithdrawal);

    LOG.info("Checking Current Balance after withdrawing");
    final MonthlyBalanceDTO currentMonthlyBalance =
        monthlyBalanceService.findByAccountIdAndPeriod(accountId, period).get();

    final MonthlyBalanceDTO expectedMonthlyBalanceAfterMovement =
        EntityTestBuilder.withClosingBalance(accountId, period, snapshotWithdrawal)
            .totalDebits(initialMonthlyBalance.totalDebits())
            .totalCredits(initialMonthlyBalance.totalCredits().add(withdrawal))
            .openingBalance(initialMonthlyBalance.openingBalance())
            .movementBalance(initialMonthlyBalance.movementBalance().subtract(withdrawal))
            .totalMovements(initialMonthlyBalance.totalMovements() + 1)
            .monthlyNetProfit(withJBHDecimals(JBH_ZERO))
            .build();
    assertMonthlyBalance(expectedMonthlyBalanceAfterMovement, currentMonthlyBalance);

    final BigDecimal deposit = withJBHDecimals(new BigDecimal("200"));
    final BigDecimal snapshotDeposit = withJBHDecimals(new BigDecimal("1130"));
    addMovement(period, IncomeCategory.SALARY, deposit, snapshotDeposit);

    LOG.info("Checking Current Balance after depositing");
    final MonthlyBalanceDTO currentMonthlyBalance2 =
        monthlyBalanceService.findByAccountIdAndPeriod(accountId, period).get();

    final MonthlyBalanceDTO expectedMonthlyBalanceAfterMovement2 =
        EntityTestBuilder.withClosingBalance(accountId, period, snapshotDeposit)
            .totalDebits(expectedMonthlyBalanceAfterMovement.totalDebits().add(deposit))
            .totalCredits(expectedMonthlyBalanceAfterMovement.totalCredits())
            .openingBalance(expectedMonthlyBalanceAfterMovement.openingBalance())
            .movementBalance(expectedMonthlyBalanceAfterMovement.movementBalance().add(deposit))
            .totalMovements(expectedMonthlyBalanceAfterMovement.totalMovements() + 1)
            .monthlyNetProfit(withJBHDecimals(new BigDecimal("30.00")))
            .build();
    assertMonthlyBalance(expectedMonthlyBalanceAfterMovement2, currentMonthlyBalance2);

    LOG.warn("Testing the registration of the Monthly Balance for period {}", period);

    final AtomicReference<MonthlyBalanceDTO> savedMonthlyBalance = new AtomicReference<>();
    Assertions.assertDoesNotThrow(
        () ->
            savedMonthlyBalance.set(
                useCaseTest.registerOfficialMonthlyBalance(
                    runningDate, userId, accountId, command)));
    final var actualReportedBalance = savedMonthlyBalance.get();
    verify(accountMovementRepository, times(3)).save((MovementDTO) any());

    LOG.info("Assertion for Monthly Balance for period {}", period);
    finalReported202410 =
        EntityTestBuilder.withClosingBalance(
                accountId,
                period,
                command.closingBalance().subtract(command.monthlyProfitReported()))
            .totalDebits(new BigDecimal("230.00"))
            .monthlyNetProfit(command.monthlyProfitReported())
            .totalMovements(3)
            .openingBalance(new BigDecimal("1170.00"))
            .officialMonthlyReport(true)
            .monthlyReportedProfit(command.monthlyProfitReported())
            .movementBalance(new BigDecimal("-70.00"))
            .totalCredits(new BigDecimal("300.00"))
            .netGrowthRate(withJBHDecimals(new BigDecimal("0.00")))
            .build();
    assertMonthlyBalance(finalReported202410, actualReportedBalance);

    // Then Account Balance
    final var beforeMovementBalance = finalAccountBalance.movementBalance();
    refreshActualAccountBalance();
    assertEquals(
        previousAccountNetProfitBalance.add(command.monthlyProfitReported()),
        finalAccountBalance.netProfitBalance());
    assertEquals(command.closingBalance(), finalAccountBalance.currentBalance(), "Account Balance");
    assertEquals(
        beforeMovementBalance
            .add(deposit)
            .subtract(withdrawal)
            .add(command.monthlyProfitReported()),
        finalAccountBalance.movementBalance());
  }

  @Test
  @Order(4)
  void month4ProfitReport202411() throws BusinessException {
    final var beforeMovementBalance = finalAccountBalance.movementBalance();
    final var beforeNetProfitBalance = finalAccountBalance.netProfitBalance();
    ++commandIndex;
    final var currentCommand = getCurrentCommand();
    final var previousCommand = getPreviousCommand();
    final var period = currentCommand.monthlyPeriod();

    // Checking Opening Balance
    final var openingBalance = withJBHDecimals(new BigDecimal("950"));
    final var dividendsPreviousMonth = previousCommand.monthlyProfitReported();
    final var expectedOpeningBalance =
        MonthlyBalanceDTO.defaultBuilder()
            .productId(accountId)
            .period(period)
            .movementBalance(dividendsPreviousMonth)
            .openingBalance(openingBalance)
            .closingBalance(previousCommand.closingBalance())
            .totalDebits(dividendsPreviousMonth)
            .totalMovements(1)
            .build();
    final var currentMonthBalance =
        monthlyBalanceService.findByAccountIdAndPeriod(accountId, period).get();
    assertMonthlyBalance(expectedOpeningBalance, currentMonthBalance);

    final BigDecimal deposit1 = withJBHDecimals(new BigDecimal("200"));
    final BigDecimal snapshot1 = withJBHDecimals(new BigDecimal("1200"));
    addMovement(period, IncomeCategory.SALARY, deposit1, snapshot1);

    final BigDecimal withdrawal1 = withJBHDecimals(new BigDecimal("100"));
    final BigDecimal snapshot2 = withJBHDecimals(new BigDecimal("1050"));
    addMovement(period, ExpenseCategory.PERSONAL, withdrawal1, snapshot2);

    final BigDecimal withdrawal2 = withJBHDecimals(new BigDecimal("350"));
    final BigDecimal snapshot3 = withJBHDecimals(new BigDecimal("700"));
    addMovement(period, ExpenseCategory.PERSONAL, withdrawal2, snapshot3);

    LOG.info("Checking Current Balance after 3 movements");
    final MonthlyBalanceDTO currentBalanceAfter3Movements =
        monthlyBalanceService.findByAccountIdAndPeriod(accountId, period).get();
    final MonthlyBalanceDTO expectedBalanceAfter3Movements =
        EntityTestBuilder.withClosingBalance(accountId, period, snapshot3)
            .totalDebits(deposit1.add(dividendsPreviousMonth))
            .totalCredits(withdrawal1.add(withdrawal2))
            .openingBalance(openingBalance)
            .movementBalance(new BigDecimal("-200.00"))
            .totalMovements(4) // 3 movements and 1 dividend
            .monthlyNetProfit(withJBHDecimals(new BigDecimal("-50.00")))
            .build();
    assertMonthlyBalance(expectedBalanceAfter3Movements, currentBalanceAfter3Movements);

    // Then Account Balance
    refreshActualAccountBalance();
    assertEquals(snapshot3, finalAccountBalance.currentBalance(), "Account Balance");
    assertEquals(new BigDecimal("-180.00"), finalAccountBalance.netProfitBalance());

    // Final Reported Monthly Balance
    final MonthlyBalanceDTO reportedMonthlyBalance =
        useCaseTest.registerOfficialMonthlyBalance(
            runningDate, userId, accountId, getCurrentCommand());
    final MonthlyBalanceDTO expectedReportedBalance =
        EntityTestBuilder.withClosingBalance(
                accountId,
                period,
                currentCommand.closingBalance().subtract(currentCommand.monthlyProfitReported()))
            .totalDebits(expectedBalanceAfter3Movements.totalDebits())
            .totalCredits(expectedBalanceAfter3Movements.totalCredits())
            .openingBalance(openingBalance)
            .movementBalance(expectedBalanceAfter3Movements.movementBalance())
            .officialMonthlyReport(true)
            .totalMovements(
                expectedBalanceAfter3Movements.totalMovements()) // 3 movements and 1 dividend
            .monthlyNetProfit(new BigDecimal("100.00"))
            .build();
    assertMonthlyBalance(expectedReportedBalance, reportedMonthlyBalance);

    verify(accountMovementRepository, times(4)).save((MovementDTO) any());

    // Then Account Balance

    refreshActualAccountBalance();
    assertEquals(new BigDecimal("980.00"), finalAccountBalance.movementBalance());
    assertEquals(
        currentCommand.closingBalance(), finalAccountBalance.currentBalance(), "Account Balance");
    assertEquals(
        beforeMovementBalance
            .add(currentCommand.monthlyProfitReported())
            .add(deposit1)
            .subtract(withdrawal1)
            .subtract(withdrawal2),
        finalAccountBalance.movementBalance(),
        "Movement Balance");

    // Assertions for the next month
    final var nextPeriod = currentCommand.monthlyPeriod().plusMonths(1);
    final MonthlyBalanceDTO nextMonthBalance =
        monthlyBalanceService.findByAccountIdAndPeriod(accountId, nextPeriod).get();
    final MonthlyBalanceDTO expectedNextMonthBalance =
        EntityTestBuilder.withClosingBalance(accountId, nextPeriod, currentCommand.closingBalance())
            .totalDebits(currentCommand.monthlyProfitReported())
            .openingBalance(
                currentCommand.closingBalance().subtract(currentCommand.monthlyProfitReported()))
            .movementBalance(currentCommand.monthlyProfitReported())
            .totalMovements(1) // 1 dividend
            .build();
    assertMonthlyBalance(expectedNextMonthBalance, nextMonthBalance);
  }

  @Test
  @Order(5)
  void month5ProfitReport202412() throws BusinessException {
    ++commandIndex;
    final var previousAccountMovementBalance = finalAccountBalance.movementBalance();
    final var previousAccountNetProfitBalance = finalAccountBalance.netProfitBalance();
    final AddMonthlyBalanceCommand command = getCurrentCommand();
    final AddMonthlyBalanceCommand previousCommand = getPreviousCommand();
    final var period = command.monthlyPeriod();

    // Checking Opening Balance
    final var previousDividends = previousCommand.monthlyProfitReported();
    final var openingBalance = previousCommand.closingBalance().subtract(previousDividends);
    final var expectedOpeningBalance =
        MonthlyBalanceDTO.defaultBuilder()
            .productId(accountId)
            .period(period)
            .movementBalance(previousDividends)
            .openingBalance(openingBalance)
            .closingBalance(previousCommand.closingBalance())
            .totalDebits(previousDividends)
            .totalMovements(1)
            .build();
    final var currentMonthBalance =
        monthlyBalanceService.findByAccountIdAndPeriod(accountId, period).get();
    assertMonthlyBalance(expectedOpeningBalance, currentMonthBalance);

    final var withDrawal1 = withJBHDecimals(new BigDecimal("100"));
    final var snapshot1 = withJBHDecimals(new BigDecimal("410"));
    addMovement(period, ExpenseCategory.PERSONAL, withDrawal1, snapshot1);

    // Registering Monthly Balance
    final MonthlyBalanceDTO finalReportedMonthlyBalance =
        useCaseTest.registerOfficialMonthlyBalance(runningDate, userId, accountId, command);

    final var expectedMonthlyBalance =
        EntityTestBuilder.withClosingBalance(
                accountId,
                period,
                command.closingBalance().subtract(command.monthlyProfitReported()))
            .totalDebits((previousDividends))
            .totalCredits(withDrawal1)
            .openingBalance(openingBalance)
            .movementBalance(previousDividends.subtract(withDrawal1))
            .totalMovements(2)
            .monthlyNetProfit(withJBHDecimals(new BigDecimal("20.00")))
            .officialMonthlyReport(true)
            .build();
    assertMonthlyBalance(expectedMonthlyBalance, finalReportedMonthlyBalance);

    // Verify that dividends movement was created
    verify(accountMovementRepository, times(2)).save(any(MovementDTO.class));

    refreshActualAccountBalance();
    assertEquals(command.closingBalance(), finalAccountBalance.currentBalance(), "Account Balance");
    assertEquals(new BigDecimal("900.00"), finalAccountBalance.movementBalance());
    assertEquals(
        previousAccountNetProfitBalance.add(command.monthlyProfitReported()),
        finalAccountBalance.netProfitBalance());
  }

  @Test
  @Order(6)
  void month6ProfitReport202501() throws BusinessException {
    ++commandIndex;

    final AddMonthlyBalanceCommand command = getCurrentCommand();
    final AddMonthlyBalanceCommand previousCommand = getPreviousCommand();
    final var period = command.monthlyPeriod();

    // Checking Opening Balance
    final var previousDividends = previousCommand.monthlyProfitReported();
    final var openingBalance = previousCommand.closingBalance().subtract(previousDividends);
    final var expectedOpeningBalance =
        MonthlyBalanceDTO.defaultBuilder()
            .productId(accountId)
            .period(period)
            .movementBalance(previousDividends)
            .openingBalance(openingBalance)
            .closingBalance(previousCommand.closingBalance())
            .totalDebits(previousDividends)
            .totalMovements(1)
            .build();
    final var currentMonthBalance =
        monthlyBalanceService.findByAccountIdAndPeriod(accountId, period).get();
    assertMonthlyBalance(expectedOpeningBalance, currentMonthBalance);

    final var deposit1 = withJBHDecimals(new BigDecimal("120"));
    final var snapshot1 = withJBHDecimals(new BigDecimal("530"));
    addMovement(period, IncomeCategory.SALARY, deposit1, snapshot1);

    // Registering Monthly Balance
    final MonthlyBalanceDTO finalReportedMonthlyBalance =
        useCaseTest.registerOfficialMonthlyBalance(runningDate, userId, accountId, command);

    final var closingBalanceWithoutDividends =
        command.closingBalance().subtract(command.monthlyProfitReported());
    final var expectedMonthlyBalance =
        EntityTestBuilder.withClosingBalance(accountId, period, closingBalanceWithoutDividends)
            .totalDebits(withJBHDecimals(new BigDecimal("140")))
            .openingBalance(openingBalance)
            .movementBalance(deposit1.add(previousDividends))
            .totalMovements(2)
            .monthlyNetProfit(withJBHDecimals(new BigDecimal("10.00")))
            .officialMonthlyReport(true)
            .build();
    assertMonthlyBalance(expectedMonthlyBalance, finalReportedMonthlyBalance);

    // Verify that dividends movement was created
    verify(accountMovementRepository, times(2)).save(any(MovementDTO.class));

    // Then Account Balance
    refreshActualAccountBalance();
    assertEquals(command.closingBalance(), finalAccountBalance.currentBalance(), "Account Balance");
    assertEquals(new BigDecimal("1030.00"), finalAccountBalance.movementBalance());

    // Assertions for the next month 202502
    final var nextPeriod = command.monthlyPeriod().plusMonths(1);
    final MonthlyBalanceDTO nextMonthBalance =
        monthlyBalanceService.findByAccountIdAndPeriod(accountId, nextPeriod).get();
    final MonthlyBalanceDTO expectedNextMonthBalance =
        EntityTestBuilder.withClosingBalance(accountId, nextPeriod, command.closingBalance())
            .totalDebits(command.monthlyProfitReported())
            .openingBalance(closingBalanceWithoutDividends)
            .movementBalance(command.monthlyProfitReported())
            .totalMovements(1) // 1 dividend
            .build();
    assertMonthlyBalance(expectedNextMonthBalance, nextMonthBalance);

    LOG.info("month6ProfitReport202501 - END");
  }

  @Test
  @Order(7)
  void registerReportWithRetefuente20252() throws BusinessException {
    LOG.info("registerReportWithRetefuente20252 - START");
    ++commandIndex;
    final AddMonthlyBalanceCommand command = getCurrentCommand();
    final AddMonthlyBalanceCommand previousCommand = getPreviousCommand();
    final var period = command.monthlyPeriod();

    // Checking Opening Balance
    final var previousDividends = previousCommand.monthlyProfitReported();
    final var openingBalance = previousCommand.closingBalance().subtract(previousDividends);
    final var expectedOpeningBalance =
        MonthlyBalanceDTO.defaultBuilder()
            .productId(accountId)
            .period(period)
            .movementBalance(previousDividends)
            .openingBalance(openingBalance)
            .closingBalance(previousCommand.closingBalance())
            .totalDebits(previousDividends)
            .totalMovements(1)
            .build();
    final var currentMonthBalance =
        monthlyBalanceService.findByAccountIdAndPeriod(accountId, period).get();
    assertMonthlyBalance(expectedOpeningBalance, currentMonthBalance);

    // Registering Monthly Balance
    final MonthlyBalanceDTO finalReportedMonthlyBalance =
        useCaseTest.registerOfficialMonthlyBalance(runningDate, userId, accountId, command);
    assertNotNull(finalReportedMonthlyBalance);

    try {
      Thread.sleep(1000l);
    } catch (final InterruptedException e) {
      throw new RuntimeException(e);
    }

    verify(accountMovementRepository, times(2)).save(any(MovementDTO.class));

    // Then Monthly Balance Assertions
    final var expectedMonthlyBalance =
        MonthlyBalanceDTO.defaultBuilder()
            .productId(accountId)
            .period(period)
            .openingBalance(withJBHDecimals(expectedOpeningBalance.openingBalance()))
            .closingBalance(withJBHDecimals(new BigDecimal("580")))
            .totalDebits(withJBHDecimals(expectedOpeningBalance.totalDebits()))
            .totalCredits(withJBHDecimals(expectedOpeningBalance.totalCredits()))
            .movementBalance(withJBHDecimals(expectedOpeningBalance.movementBalance()))
            .totalMovements(expectedOpeningBalance.totalMovements())
            .monthlyReportedProfit(withJBHDecimals(command.monthlyProfitReported()))
            .incomeWithholdingTaxAmount(withJBHDecimals(command.incomeWithholdingTaxAmount()))
            .monthlyNetProfit(new BigDecimal("2.00"))
            .netGrowthRate(withJBHDecimals(new BigDecimal("1.75")))
            .officialMonthlyReport(true)
            .build();
    assertMonthlyBalance(expectedMonthlyBalance, finalReportedMonthlyBalance);
    assertEquals(
        expectedMonthlyBalance.netGrowthRate(), finalReportedMonthlyBalance.netGrowthRate());

    // Then Next Monthly Balance Assertions
    LOG.info("Searching Period Balance {} ", period.plusMonths(1));
    final var nextMonthBalance =
        monthlyBalanceService.findByAccountIdAndPeriod(accountId, period.plusMonths(1)).get();
    final var expectedNextMonthBalance =
        MonthlyBalanceDTO.defaultBuilder()
            .productId(accountId)
            .period(period.plusMonths(1))
            .openingBalance(withJBHDecimals(new BigDecimal("580")))
            .closingBalance(withJBHDecimals(new BigDecimal("582")))
            .totalDebits(withJBHDecimals(new BigDecimal("5")))
            .totalCredits(withJBHDecimals(new BigDecimal("3")))
            .movementBalance(withJBHDecimals(new BigDecimal("2")))
            .totalMovements(2)
            .officialMonthlyReport(false)
            .build();
    assertMonthlyBalance(expectedNextMonthBalance, nextMonthBalance);

    // Account Balance Assertions
    final var previousAccountBalance = finalAccountBalance.movementBalance();
    refreshActualAccountBalance();

    assertEquals(command.closingBalance(), finalAccountBalance.currentBalance());
    assertEquals(
        previousAccountBalance
            .add(command.monthlyProfitReported())
            .subtract(command.incomeWithholdingTaxAmount()),
        finalAccountBalance.movementBalance());
    assertEquals(new BigDecimal("262.00"), finalAccountBalance.netProfitBalance());

    LOG.info("registerReportWithRetefuente20252 - END");
  }

  @Test
  @Order(17)
  void shouldThrowExceptionWhenAddingSnapshotAfterMonthlyReported() {
    LOG.info("shouldThrowExceptionWhenAddingSnapshotAfterMonthlyReported - START");

    final var deposit = withJBHDecimals(new BigDecimal("100"));
    final var snapshot = withJBHDecimals(new BigDecimal("1100"));
    final String message =
        "Cannot add a snapshot after the monthly balance was officially reported";

    assertThrows(
        RuntimeException.class,
        () -> {
          addMovement(YearMonth.of(2024, 9), ExpenseCategory.PERSONAL, deposit, snapshot);
        },
        message);

    LOG.info("shouldThrowExceptionWhenAddingSnapshotAfterMonthlyReported - END");
  }

  @Test
  @Order(18)
  void shouldAddDebitsToReportedMonthlyBalance() {
    LOG.info("shouldAddDebitsToReportedMonthlyBalance - START");
    // Given
    final YearMonth monthlyPeriod = YearMonth.of(2024, 9);
    final BigDecimal deposit = withJBHDecimals(new BigDecimal("10"));
    final BigDecimal snapshot = null;

    addMovement(monthlyPeriod, IncomeCategory.SALARY, deposit, snapshot);

    final MonthlyBalanceDTO actualReported20249 =
        monthlyBalanceService.findByAccountIdAndPeriod(accountId, monthlyPeriod).get();

    final MonthlyBalanceDTO expectedReported20249 =
        MonthlyBalanceDTO.defaultBuilder()
            .productId(accountId)
            .period(monthlyPeriod)
            .openingBalance(finalReported20249.openingBalance())
            .closingBalance(finalReported20249.closingBalance())
            .totalDebits(finalReported20249.totalDebits().add(deposit))
            .totalCredits(finalReported20249.totalCredits())
            .movementBalance(finalReported20249.movementBalance().add(deposit))
            .totalMovements(finalReported20249.totalMovements() + 1)
            .officialMonthlyReport(finalReported20249.officialMonthlyReport())
            .monthlyNetProfit(finalReported20249.monthlyNetProfit())
            .build();

    // Check that closing balances are not affected
    assertMonthlyBalance(expectedReported20249, actualReported20249);

    // Check that next month balance is not affected
    final var nextMonthBalance =
        monthlyBalanceService
            .findByAccountIdAndPeriod(accountId, monthlyPeriod.plusMonths(1))
            .get();
    assertMonthlyBalance(nextMonthBalance, finalReported202410);

    // Check that productDTO balance is not updated
    final var currentAccountBalance = inMemoryAccountRepo.findByProductId(accountId).get();
    assertEquals(
        currentAccountBalance.currentBalance(),
        finalAccountBalance.currentBalance(),
        "Account Balance");
    assertEquals(
        currentAccountBalance.movementBalance(),
        finalAccountBalance.movementBalance().add(deposit));
    assertEquals(currentAccountBalance.netProfitBalance(), finalAccountBalance.netProfitBalance());

    LOG.info("shouldAddDebitsToReportedMonthlyBalance - END");
  }
}
