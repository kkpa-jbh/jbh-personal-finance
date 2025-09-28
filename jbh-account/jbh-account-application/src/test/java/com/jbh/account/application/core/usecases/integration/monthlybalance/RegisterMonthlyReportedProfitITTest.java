package com.jbh.account.application.core.usecases.integration.monthlybalance;

import static com.jbh.account.application.core.ports.output.account.InMemoryAccountRepository.DEFAULT_ACCOUNT_NAME;
import static com.jbh.account.application.core.usecases.utils.MonthlyBalanceITUtils.assertMonthlyBalance;
import static com.jbh.account.application.core.usecases.utils.TestDataFactory.getAddMonthlyBalanceCommandsWithProfit;
import static com.jbh.account.domain.utils.MoneyUtils.JBH_ZERO;
import static com.jbh.account.domain.utils.MoneyUtils.withJBHDecimals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.jbh.account.application.async.AsyncTaskExecutorImpl;
import com.jbh.account.application.core.dto.AccountDTO;
import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.application.core.dto.MovementDTO;
import com.jbh.account.application.core.ports.input.AddMovementInputPort;
import com.jbh.account.application.core.ports.input.CreateAccountInputPort;
import com.jbh.account.application.core.ports.input.RegisterMonthlyBalanceInputPort;
import com.jbh.account.application.core.ports.output.account.InMemoryAccountRepository;
import com.jbh.account.application.core.ports.output.monthlybalance.AccountMonthlyBalanceQueryRepo;
import com.jbh.account.application.core.ports.output.monthlybalance.AccountMonthlyBalanceWriterRepository;
import com.jbh.account.application.core.ports.output.monthlybalance.InMemoryMonthlyBalanceRepositories;
import com.jbh.account.application.core.services.MonthlyBalanceSyncForUploadedMovements;
import com.jbh.account.application.core.services.account.AccountService;
import com.jbh.account.application.core.services.account.AccountServiceImpl;
import com.jbh.account.application.core.services.monthlybalance.MonthlyBalanceService;
import com.jbh.account.application.core.services.monthlybalance.MonthlyBalanceServiceImpl;
import com.jbh.account.application.core.services.movements.AccountMovementServiceImpl;
import com.jbh.account.application.core.usecases.AddMovementUseCase;
import com.jbh.account.application.core.usecases.CreateAccountUseCase;
import com.jbh.account.application.core.usecases.RegisterMonthlyBalanceUseCase;
import com.jbh.account.application.core.usecases.utils.UnitOfWorkTest;
import com.jbh.account.application.core.vo.commands.AddMonthlyBalanceCommand;
import com.jbh.account.application.core.vo.commands.AddMovementCommand;
import com.jbh.account.application.core.vo.commands.CreateBasicAccountCommand;
import com.jbh.account.application.exceptions.JbhSpecificationApplication;
import com.jbh.account.application.movements.ports.output.AccountMovementRepository;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountType;
import com.jbh.account.domain.vo.CategoryType;
import com.jbh.account.domain.vo.ExpenseCategory;
import com.jbh.account.domain.vo.IncomeCategory;
import com.jbh.account.domain.vo.MovementCategoryDTO;
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
  private static final InMemoryAccountRepository inMemoryAccountRepo =
      new InMemoryAccountRepository();
  static InMemoryMonthlyBalanceRepositories inMemoryMonthlyBalanceRepos =
      new InMemoryMonthlyBalanceRepositories();
  static CreateAccountUseCase createAccountUseCase;
  static AccountDTO createdAccount;
  static AccountId accountId;
  static int commandIndex = -1;
  static MonthlyBalanceService monthlyBalanceService;
  private static MonthlyBalanceDTO finalReported20249;
  private static AccountDTO finalExpectedAccountBalance;
  private static MonthlyBalanceDTO finalReported202410;
  AddMovementUseCase addMovementUseCase;
  RegisterMonthlyBalanceUseCase useCaseTest;
  YearMonth initialPeriod = YearMonth.of(2024, 7);
  LocalDate runningDate = LocalDate.now();
  BigDecimal FIRST_BALANCE_ZERO = withJBHDecimals(new BigDecimal("1000"));
  List<AddMonthlyBalanceCommand> monthlyCommands =
      getAddMonthlyBalanceCommandsWithProfit(initialPeriod, FIRST_BALANCE_ZERO);
  MonthlyBalanceSyncForUploadedMovements monthlyBalanceSyncer;
  AccountMovementServiceImpl accountMovementService;
  @Mock private AccountMovementRepository accountMovementRepository;

  @BeforeAll
  static void beforeAll() {
    inMemoryMonthlyBalanceRepos.clearStorage();
    inMemoryAccountRepo.clearStorage();
  }

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    final AccountMonthlyBalanceWriterRepository monthlyBalanceInMemoWriter =
        inMemoryMonthlyBalanceRepos.getWriterRepo();
    final AccountMonthlyBalanceQueryRepo monthlyBalanceInMemoQuery =
        inMemoryMonthlyBalanceRepos.getQueryRepo();
    monthlyBalanceService =
        new MonthlyBalanceServiceImpl(
            monthlyBalanceInMemoQuery, monthlyBalanceInMemoWriter, new AsyncTaskExecutorImpl());

    final AccountService accountService = new AccountServiceImpl(inMemoryAccountRepo);
    accountMovementService =
        new AccountMovementServiceImpl(
            accountMovementRepository, accountService, monthlyBalanceService, new UnitOfWorkTest());

    useCaseTest =
        new RegisterMonthlyBalanceInputPort(
            monthlyBalanceService, accountService, accountMovementService);

    createAccountUseCase = new CreateAccountInputPort(accountService);

    monthlyBalanceSyncer = new MonthlyBalanceSyncForUploadedMovements(monthlyBalanceService);
    addMovementUseCase = new AddMovementInputPort(accountMovementService);
  }

  @Test
  @Order(0)
  void settingInitialBalance() throws JbhSpecificationApplication {
    ++commandIndex;
    createdAccount =
        createAccountUseCase.execute(
            new CreateBasicAccountCommand(userId, DEFAULT_ACCOUNT_NAME, AccountType.SAVINGS));
    accountId = createdAccount.id();
    LOG.info("Account created with id {}", accountId);
    assertNotNull(accountId);

    // Creating Initial Balance
    final AddMonthlyBalanceCommand command = getCurrentCommand();
    final AtomicReference<MonthlyBalanceDTO> savedInitialMonthlyBalance = new AtomicReference<>();

    savedInitialMonthlyBalance.set(
        useCaseTest.registerOfficialMonthlyBalance(runningDate, userId, accountId, command));

    // Then
    final MonthlyBalanceDTO expectedInitialBalance =
        MonthlyBalanceDTO.defaultBuilder()
            .closingBalance(FIRST_BALANCE_ZERO)
            .movementBalance(FIRST_BALANCE_ZERO)
            .totalDebits(FIRST_BALANCE_ZERO)
            .accountId(accountId)
            .period(initialPeriod)
            .year(initialPeriod.getYear())
            .month(initialPeriod.getMonthValue())
            .officialMonthlyReport(true)
            .totalMovements(1)
            .build();
    assertMonthlyBalance(expectedInitialBalance, savedInitialMonthlyBalance.get());
  }

  private AddMonthlyBalanceCommand getCurrentCommand() {
    return monthlyCommands.get(commandIndex);
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

    // Then Current Monthly Balance has closing balance - monthlyProfitReported (50)
    final var actualMonthlyBalance = savedMonthlyBalance.get();
    final var monthlyProfitReported = command.monthlyProfitReported();
    final var expectedMonthlyBalance =
        MonthlyBalanceDTO.withClosingBalance(
                accountId,
                command.monthlyPeriod(),
                command.closingBalance().subtract(monthlyProfitReported))
            .monthlyProfitReported(monthlyProfitReported)
            .monthlyNetProfit(withJBHDecimals(monthlyProfitReported))
            .openingBalance(withJBHDecimals(getPreviousCommand().closingBalance()))
            .officialMonthlyReport(true)
            .build();
    assertMonthlyBalance(expectedMonthlyBalance, actualMonthlyBalance);
    assertEquals(new BigDecimal("5.00"), actualMonthlyBalance.netGrowthRate());

    // Verify that dividends movement was created
    verify(accountMovementRepository).save(any(MovementDTO.class));

    // Then Account Balance
    final var accountBalance = inMemoryAccountRepo.findByAccountId(accountId).get();
    assertEquals(command.closingBalance(), accountBalance.currentBalance(), "Account Balance");
  }

  private AddMonthlyBalanceCommand getPreviousCommand() {
    return monthlyCommands.get(commandIndex - 1);
  }

  @Test
  @Order(2)
  public void month2ProfitReport20249() {
    ++commandIndex;
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
        MonthlyBalanceDTO.withClosingBalance(accountId, period, snapshot2)
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
        MonthlyBalanceDTO.withClosingBalance(accountId, period, closingBalanceWithProfit)
            .officialMonthlyReport(true)
            .monthlyNetProfit(command.monthlyProfitReported())
            .monthlyProfitReported(command.monthlyProfitReported())
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
    final var accountBalance = inMemoryAccountRepo.findByAccountId(accountId).get();
    assertEquals(command.closingBalance(), accountBalance.currentBalance(), "Account Balance");
  }

  private void addMovement(
      final YearMonth period,
      final CategoryType categoryType,
      final BigDecimal amount,
      final BigDecimal balanceSnapshot) {
    final AddMovementCommand movement =
        new AddMovementCommand(
            LocalDate.of(period.getYear(), period.getMonthValue(), 15),
            amount,
            balanceSnapshot,
            MovementCategoryDTO.withType(categoryType));
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
        MonthlyBalanceDTO.withClosingBalance(accountId, period, snapshotWithdrawal)
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
        MonthlyBalanceDTO.withClosingBalance(accountId, period, snapshotDeposit)
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
        MonthlyBalanceDTO.withClosingBalance(
                accountId,
                period,
                command.closingBalance().subtract(command.monthlyProfitReported()))
            .totalDebits(new BigDecimal("230.00"))
            .monthlyNetProfit(command.monthlyProfitReported())
            .totalMovements(3)
            .openingBalance(new BigDecimal("1170.00"))
            .officialMonthlyReport(true)
            .monthlyProfitReported(command.monthlyProfitReported())
            .movementBalance(new BigDecimal("-70.00"))
            .totalCredits(new BigDecimal("300.00"))
            .netGrowthRate(withJBHDecimals(new BigDecimal("0.00")))
            .build();
    assertMonthlyBalance(finalReported202410, actualReportedBalance);

    // Then Account Balance
    finalExpectedAccountBalance = inMemoryAccountRepo.findByAccountId(accountId).get();
    assertEquals(
        command.closingBalance(), finalExpectedAccountBalance.currentBalance(), "Account Balance");
  }

  @Test
  @Order(4)
  void month4ProfitReport202411() throws JbhSpecificationApplication {
    ++commandIndex;
    final var currentCommand = getCurrentCommand();
    final var previousCommand = getPreviousCommand();
    final var period = currentCommand.monthlyPeriod();

    // Checking Opening Balance
    final var openingBalance = withJBHDecimals(new BigDecimal("950"));
    final var dividendsPreviousMonth = previousCommand.monthlyProfitReported();
    final var expectedOpeningBalance =
        MonthlyBalanceDTO.defaultBuilder()
            .accountId(accountId)
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
        MonthlyBalanceDTO.withClosingBalance(accountId, period, snapshot3)
            .totalDebits(deposit1.add(dividendsPreviousMonth))
            .totalCredits(withdrawal1.add(withdrawal2))
            .openingBalance(openingBalance)
            .movementBalance(new BigDecimal("-200.00"))
            .totalMovements(4) // 3 movements and 1 dividend
            .monthlyNetProfit(withJBHDecimals(new BigDecimal("-50.00")))
            .build();
    assertMonthlyBalance(expectedBalanceAfter3Movements, currentBalanceAfter3Movements);

    // Then Account Balance
    finalExpectedAccountBalance = inMemoryAccountRepo.findByAccountId(accountId).get();
    assertEquals(snapshot3, finalExpectedAccountBalance.currentBalance(), "Account Balance");

    // Final Reported Monthly Balance
    final MonthlyBalanceDTO reportedMonthlyBalance =
        useCaseTest.registerOfficialMonthlyBalance(
            runningDate, userId, accountId, getCurrentCommand());
    final MonthlyBalanceDTO expectedReportedBalance =
        MonthlyBalanceDTO.withClosingBalance(
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
    finalExpectedAccountBalance = inMemoryAccountRepo.findByAccountId(accountId).get();
    assertEquals(
        currentCommand.closingBalance(),
        finalExpectedAccountBalance.currentBalance(),
        "Account Balance");

    // Assertions for the next month
    final var nextPeriod = currentCommand.monthlyPeriod().plusMonths(1);
    final MonthlyBalanceDTO nextMonthBalance =
        monthlyBalanceService.findByAccountIdAndPeriod(accountId, nextPeriod).get();
    final MonthlyBalanceDTO expectedNextMonthBalance =
        MonthlyBalanceDTO.withClosingBalance(accountId, nextPeriod, currentCommand.closingBalance())
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
  void month5ProfitReport202412() throws JbhSpecificationApplication {
    ++commandIndex;
    final AddMonthlyBalanceCommand command = getCurrentCommand();
    final AddMonthlyBalanceCommand previousCommand = getPreviousCommand();
    final var period = command.monthlyPeriod();

    // Checking Opening Balance
    final var previousDividends = previousCommand.monthlyProfitReported();
    final var openingBalance = previousCommand.closingBalance().subtract(previousDividends);
    final var expectedOpeningBalance =
        MonthlyBalanceDTO.defaultBuilder()
            .accountId(accountId)
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

    final var deposit1 = withJBHDecimals(new BigDecimal("100"));
    final var snapshot1 = withJBHDecimals(new BigDecimal("420"));
    addMovement(period, IncomeCategory.SALARY, deposit1, snapshot1);

    // Registering Monthly Balance
    final MonthlyBalanceDTO finalReportedMonthlyBalance =
        useCaseTest.registerOfficialMonthlyBalance(runningDate, userId, accountId, command);

    final var expectedMonthlyBalance =
        MonthlyBalanceDTO.withClosingBalance(
                accountId,
                period,
                command.closingBalance().subtract(command.monthlyProfitReported()))
            .totalDebits(deposit1.add(previousDividends))
            .openingBalance(openingBalance)
            .movementBalance(deposit1.add(previousDividends))
            .totalMovements(2)
            .monthlyNetProfit(withJBHDecimals(new BigDecimal("20.00")))
            .officialMonthlyReport(true)
            .build();
    assertMonthlyBalance(expectedMonthlyBalance, finalReportedMonthlyBalance);

    // Verify that dividends movement was created
    verify(accountMovementRepository, times(2)).save(any(MovementDTO.class));
  }

  @Test
  @Order(6)
  void month6ProfitReport202501() throws JbhSpecificationApplication {
    ++commandIndex;
    final AddMonthlyBalanceCommand command = getCurrentCommand();
    final AddMonthlyBalanceCommand previousCommand = getPreviousCommand();
    final var period = command.monthlyPeriod();

    // Checking Opening Balance
    final var previousDividends = previousCommand.monthlyProfitReported();
    final var openingBalance = previousCommand.closingBalance().subtract(previousDividends);
    final var expectedOpeningBalance =
        MonthlyBalanceDTO.defaultBuilder()
            .accountId(accountId)
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
        MonthlyBalanceDTO.withClosingBalance(accountId, period, closingBalanceWithoutDividends)
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
    finalExpectedAccountBalance = inMemoryAccountRepo.findByAccountId(accountId).get();
    assertEquals(
        command.closingBalance(), finalExpectedAccountBalance.currentBalance(), "Account Balance");

    // Assertions for the next month 202502
    final var nextPeriod = command.monthlyPeriod().plusMonths(1);
    final MonthlyBalanceDTO nextMonthBalance =
        monthlyBalanceService.findByAccountIdAndPeriod(accountId, nextPeriod).get();
    final MonthlyBalanceDTO expectedNextMonthBalance =
        MonthlyBalanceDTO.withClosingBalance(accountId, nextPeriod, command.closingBalance())
            .totalDebits(command.monthlyProfitReported())
            .openingBalance(closingBalanceWithoutDividends)
            .movementBalance(command.monthlyProfitReported())
            .totalMovements(1) // 1 dividend
            .build();
    assertMonthlyBalance(expectedNextMonthBalance, nextMonthBalance);
  }

  @Test
  @Order(7)
  void shouldThrowExceptionWhenAddingSnapshotAfterMonthlyReported() {

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
  }

  @Test
  @Order(8)
  void shouldAddDebitsToReportedMonthlyBalance() {
    // Given
    final YearMonth monthlyPeriod = YearMonth.of(2024, 9);
    final BigDecimal deposit = withJBHDecimals(new BigDecimal("10"));
    final BigDecimal snapshot = null;

    addMovement(monthlyPeriod, IncomeCategory.SALARY, deposit, snapshot);

    final MonthlyBalanceDTO actualReported20249 =
        monthlyBalanceService.findByAccountIdAndPeriod(accountId, monthlyPeriod).get();

    final MonthlyBalanceDTO expectedReported20249 =
        MonthlyBalanceDTO.defaultBuilder()
            .accountId(accountId)
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

    // Check that account balance is not updated
    final var currentAccountBalance = inMemoryAccountRepo.findByAccountId(accountId).get();
    assertEquals(
        currentAccountBalance.currentBalance(), finalExpectedAccountBalance.currentBalance());
    assertEquals(
        currentAccountBalance.movementBalance(),
        finalExpectedAccountBalance.movementBalance().add(deposit));
    assertEquals(
        currentAccountBalance.profitBalance(), finalExpectedAccountBalance.profitBalance());
  }
}
