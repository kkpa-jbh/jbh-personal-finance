package com.jbh.account.application.core.usecases.integration.monthlybalance;

import static com.jbh.account.application.core.usecases.utils.MonthlyBalanceITUtils.assertMonthlyBalance;
import static com.jbh.account.application.core.usecases.utils.MonthlyBalanceITUtils.createMonthlyBalanceCommand;
import static com.jbh.account.domain.utils.MoneyUtils.withJBHDecimals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.jbh.account.application.core.dto.AccountDTO;
import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.application.core.dto.MovementDTO;
import com.jbh.account.application.core.ports.output.account.InMemoryAccountRepository;
import com.jbh.account.application.core.services.monthlybalance.MonthlyBalanceService;
import com.jbh.account.application.core.usecases.AddMovementUseCase;
import com.jbh.account.application.core.usecases.CreateAccountUseCase;
import com.jbh.account.application.core.usecases.RegisterMonthlyBalanceUseCase;
import com.jbh.account.application.core.usecases.UseCaseBuilder;
import com.jbh.account.application.core.vo.commands.AddMonthlyBalanceCommand;
import com.jbh.account.application.core.vo.commands.AddMovementCommand;
import com.jbh.account.application.core.vo.commands.CreateBasicAccountCommand;
import com.jbh.account.application.core.vo.commands.MonthlyBalanceCommandVO;
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

  private static final String ACCOUNT_REPORTED = "Reported";
  private static final List<BigDecimal> withdrawalMovements =
      List.of(withJBHDecimals(new BigDecimal("5")));
  private static final List<BigDecimal> depositMovements =
      List.of(withJBHDecimals(new BigDecimal("10")));

  static CreateAccountUseCase createAccountUseCase;
  static AccountDTO createdAccount;
  static AccountId accountId;
  static MonthlyBalanceService monthlyBalanceService;
  static YearMonth reportedPeriod = YearMonth.of(2024, 7);
  static MonthlyBalanceCommandVO reportedPeriodAmounts =
      new MonthlyBalanceCommandVO(
          withJBHDecimals(new BigDecimal("1010")), withJBHDecimals(new BigDecimal("10")));
  private static AccountDTO finalExpectedAccountBalance;
  private static MonthlyBalanceDTO officialReportedBalance;
  private static InMemoryAccountRepository inMemoryAccountRepo;
  RegisterMonthlyBalanceUseCase useCaseTest;
  LocalDate runningDate = LocalDate.now();
  @Mock private AccountMovementRepository accountMovementRepository;
  private AddMovementUseCase addMovementUseCase;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    monthlyBalanceService = UseCaseBuilder.buildMonthlyBalanceService();
    inMemoryAccountRepo = UseCaseBuilder.getAccountRepository();

    useCaseTest = UseCaseBuilder.buildRegisterMonthlyBalanceUseCase(accountMovementRepository);

    createAccountUseCase = UseCaseBuilder.buildCreateAccountUseCase();

    addMovementUseCase = UseCaseBuilder.buildAddMovementUseCase(accountMovementRepository);
  }

  @Test
  @Order(0)
  void creatingAccount() throws JbhSpecificationApplication {
    createdAccount =
        createAccountUseCase.execute(
            new CreateBasicAccountCommand(userId, ACCOUNT_REPORTED, AccountType.SAVINGS));
    accountId = createdAccount.id();
    LOG.info("Account created with id {}", accountId);
    assert accountId != null;

    // Creating Initial Balance
    final var initialBalance = withJBHDecimals(new BigDecimal("900"));
    final AddMonthlyBalanceCommand previousCommand =
        createMonthlyBalanceCommand(
            reportedPeriod.plusMonths(-1),
            new MonthlyBalanceCommandVO(withJBHDecimals(initialBalance), null));

    useCaseTest.registerOfficialMonthlyBalance(runningDate, userId, accountId, previousCommand);

    verify(accountMovementRepository).save(any(MovementDTO.class));

    finalExpectedAccountBalance = inMemoryAccountRepo.findByAccountId(accountId).get();
    assertEquals(initialBalance, finalExpectedAccountBalance.currentBalance(), "Account Balance");
    assertEquals(initialBalance, finalExpectedAccountBalance.movementBalance(), "Movement Balance");

    final AddMonthlyBalanceCommand command =
        createMonthlyBalanceCommand(reportedPeriod, reportedPeriodAmounts);
    final AtomicReference<MonthlyBalanceDTO> savedInitialMonthlyBalance = new AtomicReference<>();
    Assertions.assertDoesNotThrow(
        () ->
            savedInitialMonthlyBalance.set(
                useCaseTest.registerOfficialMonthlyBalance(
                    runningDate, userId, accountId, command)));

    officialReportedBalance = savedInitialMonthlyBalance.get();
    assertEquals(new BigDecimal("12.22"), officialReportedBalance.netGrowthRate());
    assertEquals(initialBalance, officialReportedBalance.openingBalance());
    assertEquals(
        command.closingBalance().subtract(command.monthlyProfitReported()),
        officialReportedBalance.closingBalance());
    assertEquals(command.monthlyProfitReported(), officialReportedBalance.monthlyReportedProfit());
    assertEquals(command.monthlyProfitReported(), officialReportedBalance.monthlyNetProfit());

    finalExpectedAccountBalance = inMemoryAccountRepo.findByAccountId(accountId).get();
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
    addMovement(reportedPeriod, ExpenseCategory.PERSONAL, withDrawal1, null);

    final MonthlyBalanceDTO currentMonthlyBalance =
        monthlyBalanceService.findByAccountIdAndPeriod(accountId, reportedPeriod).get();
    final var expectedMonthlyBalance =
        MonthlyBalanceDTO.withClosingBalance(
                accountId, reportedPeriod, officialReportedBalance.closingBalance())
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

    final AccountDTO currentAccountBalance = inMemoryAccountRepo.findByAccountId(accountId).get();
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
  @Order(2)
  void addingDeposit1() {

    final BigDecimal deposit = depositMovements.get(0);
    final BigDecimal withDrawal1 = withdrawalMovements.get(0);

    addMovement(reportedPeriod, IncomeCategory.SALARY, deposit, null);

    final MonthlyBalanceDTO currentMonthlyBalance =
        monthlyBalanceService.findByAccountIdAndPeriod(accountId, reportedPeriod).get();
    final var expectedMonthlyBalance =
        MonthlyBalanceDTO.withClosingBalance(
                accountId, reportedPeriod, officialReportedBalance.closingBalance())
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

    final AccountDTO currentAccountBalance = inMemoryAccountRepo.findByAccountId(accountId).get();
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
        () -> addMovement(reportedPeriod, IncomeCategory.SALARY, deposit, null));
  }

  @Test
  @Order(3)
  public void addExcceededWithdrawalsShouldThrowException() {
    final BigDecimal withdrawal1 = new BigDecimal("900");
    final BigDecimal withdrawal2 = new BigDecimal("10");

    // Nothing happens, the balance has enought still (5 more)
    addMovement(reportedPeriod, ExpenseCategory.PERSONAL, withdrawal1, null);

    Assertions.assertThrows(
        RuntimeException.class,
        () -> addMovement(reportedPeriod, ExpenseCategory.PERSONAL, withdrawal2, null));
  }
}
