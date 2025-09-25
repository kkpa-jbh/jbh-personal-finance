package com.jbh.account.application.core.usecases.integration.monthlybalance;

import static com.jbh.account.application.core.ports.output.account.InMemoryAccountRepository.DEFAULT_ACCOUNT_NAME;
import static com.jbh.account.application.core.usecases.utils.MonthlyBalanceITUtils.assertMonthlyBalance;
import static com.jbh.account.application.core.usecases.utils.TestDataFactory.getAddMonthlyBalanceCommandsWithProfit;
import static com.jbh.account.domain.utils.MoneyUtils.withJBHDecimals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.jbh.account.application.async.AsyncTaskExecutorImpl;
import com.jbh.account.application.core.dto.AccountDTO;
import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.application.core.ports.input.AddMovementInputPort;
import com.jbh.account.application.core.ports.input.CreateAccountInputPort;
import com.jbh.account.application.core.ports.input.RegisterMonthlyBalanceInputPort;
import com.jbh.account.application.core.ports.output.account.InMemoryAccountRepository;
import com.jbh.account.application.core.ports.output.monthlybalance.AccountMonthlyBalanceQueryRepo;
import com.jbh.account.application.core.ports.output.monthlybalance.AccountMonthlyBalanceWriterRepository;
import com.jbh.account.application.core.ports.output.monthlybalance.InMemoryMonthlyBalanceRepositories;
import com.jbh.account.application.core.services.MonthlyBalanceAsyncTask;
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
  AddMovementUseCase addMovementUseCase;
  RegisterMonthlyBalanceUseCase useCase;
  YearMonth initialPeriod = YearMonth.of(2024, 7);
  LocalDate runningDate = LocalDate.now();
  BigDecimal initialBalance = withJBHDecimals(new BigDecimal("1000"));
  List<AddMonthlyBalanceCommand> monthlyCommands =
      getAddMonthlyBalanceCommandsWithProfit(initialPeriod, initialBalance);

  MonthlyBalanceAsyncTask monthlyBalanceSyncer;
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
        new MonthlyBalanceServiceImpl(monthlyBalanceInMemoQuery, monthlyBalanceInMemoWriter);

    final AccountService accountService = new AccountServiceImpl(inMemoryAccountRepo);
    accountMovementService =
        new AccountMovementServiceImpl(
            accountMovementRepository, accountService, monthlyBalanceService, new UnitOfWorkTest());

    useCase =
        new RegisterMonthlyBalanceInputPort(
            monthlyBalanceService, accountService, accountMovementService);

    createAccountUseCase = new CreateAccountInputPort(accountService);

    monthlyBalanceSyncer =
        new MonthlyBalanceAsyncTask(monthlyBalanceService, new AsyncTaskExecutorImpl());
    addMovementUseCase = new AddMovementInputPort(accountMovementService, monthlyBalanceSyncer);
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
        useCase.registerOfficialMonthlyBalance(runningDate, userId, accountId, command));

    // Then
    final MonthlyBalanceDTO expectedInitialBalance =
        MonthlyBalanceDTO.defaultBuilder()
            .closingBalance(initialBalance)
            .accountId(accountId)
            .period(initialPeriod)
            .year(initialPeriod.getYear())
            .month(initialPeriod.getMonthValue())
            .officialMonthlyReport(true)
            .build();
    assertMonthlyBalance(expectedInitialBalance, savedInitialMonthlyBalance.get());
  }

  private AddMonthlyBalanceCommand getCurrentCommand() {
    return monthlyCommands.get(commandIndex);
  }

  @Test
  @Order(1)
  public void month1ProfitReported50() {
    ++commandIndex;
    // Given
    final AddMonthlyBalanceCommand command = getCurrentCommand();
    final AtomicReference<MonthlyBalanceDTO> savedMonthlyBalance = new AtomicReference<>();
    Assertions.assertDoesNotThrow(
        () ->
            savedMonthlyBalance.set(
                useCase.registerOfficialMonthlyBalance(runningDate, userId, accountId, command)));

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

    // Then Account Balance
    final var accountBalance = inMemoryAccountRepo.findByAccountId(accountId).get();
    assertEquals(command.closingBalance(), accountBalance.currentBalance(), "Account Balance");
  }

  private AddMonthlyBalanceCommand getPreviousCommand() {
    return monthlyCommands.get(commandIndex - 1);
  }

  @Test
  @Order(2)
  public void month2addingDebitsAndProfitReport() {
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

    final MonthlyBalanceDTO expectedMonthlyBalance =
        MonthlyBalanceDTO.withClosingBalance(accountId, period, snapshot2)
            .totalDebits((deposit2))
            .openingBalance(previousCommand.closingBalance())
            .movementBalance(deposit2)
            .totalMovements(1)
            .monthlyNetProfit(withJBHDecimals(new BigDecimal("10")))
            .netGrowthRate(withJBHDecimals(new BigDecimal("0.91")))
            .build();
    assertMonthlyBalance(expectedMonthlyBalance, currentMonthlyBalance);

    LOG.warn("Testing the registration of the Monthly Balance for period {}", period);

    final AtomicReference<MonthlyBalanceDTO> savedMonthlyBalance = new AtomicReference<>();
    Assertions.assertDoesNotThrow(
        () ->
            savedMonthlyBalance.set(
                useCase.registerOfficialMonthlyBalance(runningDate, userId, accountId, command)));

    final MonthlyBalanceDTO expectedReportedBalance =
        MonthlyBalanceDTO.withClosingBalance(
                accountId, period, getCurrentCommand().closingBalance())
            .officialMonthlyReport(true)
            .monthlyProfitReported(command.monthlyProfitReported())
            .totalDebits((deposit2))
            .openingBalance(previousCommand.closingBalance())
            .movementBalance(deposit2)
            .totalMovements(1)
            .monthlyNetProfit(command.monthlyProfitReported())
            .netGrowthRate(withJBHDecimals(new BigDecimal("4.55")))
            .build();
    assertMonthlyBalance(expectedReportedBalance, savedMonthlyBalance.get());
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
    addMovementUseCase.addMovement(userId, accountId, movement);
    try {
      Thread.sleep(Duration.ofSeconds(2).toMillis());
    } catch (final InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public void shouldAddMovementsBeforeMonthlyReport() {}
}
