package com.jbh.account.application.accounts.usecases.inmemory;

import static com.jbh.account.domain.utils.MoneyUtils.JBH_ZERO;
import static com.jbh.account.domain.utils.MoneyUtils.withJBHDecimals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.jbh.account.application.accounts.mappers.AccountMapper;
import com.jbh.account.application.accounts.ports.input.AddMovementInputPort;
import com.jbh.account.application.accounts.ports.input.RegisterMonthlyBalanceInputPort;
import com.jbh.account.application.accounts.ports.output.AccountRepository;
import com.jbh.account.application.accounts.ports.output.monthlybalance.AccountMonthlyBalanceQueryRepo;
import com.jbh.account.application.accounts.ports.output.monthlybalance.AccountMonthlyBalanceWriterRepository;
import com.jbh.account.application.accounts.ports.output.monthlybalance.inmemory.InMemoryMonthlyBalanceRepositories;
import com.jbh.account.application.accounts.services.MonthlyBalanceSyncerAppService;
import com.jbh.account.application.accounts.services.monthlybalance.MonthlyBalanceService;
import com.jbh.account.application.accounts.services.monthlybalance.MonthlyBalanceServiceImpl;
import com.jbh.account.application.accounts.usecases.AddMovementUseCase;
import com.jbh.account.application.accounts.usecases.utils.UnitOfWorkTest;
import com.jbh.account.application.accounts.vo.AddMovementCommand;
import com.jbh.account.application.accounts.vo.commands.AddMonthlyBalanceCommand;
import com.jbh.account.application.async.AsyncTaskExecutorImpl;
import com.jbh.account.application.movements.ports.output.AccountMovementRepository;
import com.jbh.account.domain.entity.AccountDomain;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountMonthlyBalanceDTO;
import com.jbh.account.domain.vo.ExpenseCategory;
import com.jbh.account.domain.vo.IncomeCategory;
import com.jbh.account.domain.vo.MovementCategoryDTO;
import com.jbh.account.domain.vo.MovementType;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
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

/** Lulo Tests */
@TestMethodOrder(OrderAnnotation.class)
public class RegisterMonthlyReportedWithoutProfitITTest {

  static final BigDecimal closingBalanceNov24 = BigDecimal.valueOf(17676950.00);
  static final BigDecimal closingBalanceDec24 = BigDecimal.valueOf(28287547.00);
  static final BigDecimal closingBalanceJan25 = withJBHDecimals(new BigDecimal("28509910.00"));
  static final BigDecimal closingBalanceFeb25 = withJBHDecimals(new BigDecimal("34064380.00"));
  private static final Logger LOG =
      LoggerFactory.getLogger(RegisterMonthlyReportedWithoutProfitITTest.class);
  // Static to be shared between tests
  static AccountId accountId = AccountId.generate();
  static UUID userId = UUID.randomUUID();
  static InMemoryMonthlyBalanceRepositories inMemoryMonthlyBalanceRepos =
      new InMemoryMonthlyBalanceRepositories();
  // Static to be shared between tests
  static int totalMonthsCreated = 1;
  private final MovementCategoryDTO SS_EXPENSE_CATEGORY =
      MovementCategoryDTO.withType(ExpenseCategory.SOCIAL_SECURITY);
  AccountMonthlyBalanceWriterRepository monthlyBalanceInMemoWriter =
      inMemoryMonthlyBalanceRepos.getWriterRepo();
  AccountMonthlyBalanceQueryRepo monthlyBalanceInMemoQuery =
      inMemoryMonthlyBalanceRepos.getQueryRepo();
  MonthlyBalanceService monthlyBalanceService =
      new MonthlyBalanceServiceImpl(monthlyBalanceInMemoQuery, monthlyBalanceInMemoWriter);
  RegisterMonthlyBalanceInputPort useCaseTest;
  AddMovementUseCase addMovementUseCase;
  MonthlyBalanceSyncerAppService monthlyBalanceSyncer;
  @Mock private AccountRepository accountRepository;
  @Mock private AccountMovementRepository accountMovementRepository;
  ;

  @BeforeAll
  static void beforeAll() {
    inMemoryMonthlyBalanceRepos.clearStorage();
  }

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    useCaseTest =
        new RegisterMonthlyBalanceInputPort(
            accountRepository, monthlyBalanceService, accountMovementRepository);

    monthlyBalanceSyncer =
        new MonthlyBalanceSyncerAppService(monthlyBalanceService, new AsyncTaskExecutorImpl());

    addMovementUseCase =
        new AddMovementInputPort(
            accountRepository,
            accountMovementRepository,
            new UnitOfWorkTest(),
            monthlyBalanceSyncer);
  }

  @Test
  @Order(1)
  void settingInitialReportedBalanceNov24() {

    // Given
    final AccountDomain accountDomain =
        AccountDomain.withBasicMovementForExisting(accountId, JBH_ZERO, JBH_ZERO);
    when(accountRepository.findByAccountId(userId, accountId))
        .thenReturn(Optional.of(AccountMapper.toDTO(accountDomain)));
    final YearMonth monthlyPeriod = YearMonth.of(2024, 11);
    final LocalDate runningDate = monthlyPeriod.plusMonths(1).atDay(1);
    final BigDecimal salaryAmount = new BigDecimal("10000000");

    logBlockHeader("TESTING " + monthlyPeriod);

    final AddMovementCommand salaryMovement =
        new AddMovementCommand(
            LocalDate.of(2024, 11, 15),
            withJBHDecimals(salaryAmount),
            MovementCategoryDTO.withType(IncomeCategory.SALARY));
    addMovement(salaryMovement);

    final AddMonthlyBalanceCommand command =
        new AddMonthlyBalanceCommand(monthlyPeriod, closingBalanceNov24, null);

    final AtomicReference<AccountMonthlyBalanceDTO> savedMonthlyBalance = new AtomicReference<>();
    Assertions.assertDoesNotThrow(
        () ->
            savedMonthlyBalance.set(
                useCaseTest.registerMonthlyBalance(runningDate, userId, accountId, command)));

    // Then
    final var actualMonthlyBalance = savedMonthlyBalance.get();
    assertEquals(monthlyPeriod, actualMonthlyBalance.getPeriod());
    assertEquals(withJBHDecimals(closingBalanceNov24), actualMonthlyBalance.getClosingBalance());
    assertEquals(withJBHDecimals(salaryAmount), actualMonthlyBalance.getMovementBalance());
    assertEquals(withJBHDecimals(salaryAmount), actualMonthlyBalance.getTotalDebits());
    assertEquals(JBH_ZERO, actualMonthlyBalance.getTotalCredits());
    assertEquals(JBH_ZERO, actualMonthlyBalance.getMonthlyProfit());
    assertEquals(
        withJBHDecimals(new BigDecimal("-7676950.00")), actualMonthlyBalance.getMonthlyExpenses());
    assertEquals(1, actualMonthlyBalance.getTotalMovements());
    assertFalse(actualMonthlyBalance.isGapPeriod());
    assertTrue(actualMonthlyBalance.isOfficialMonthlyReport());

    // Verifying the opening balance for the next month
    final Optional<AccountMonthlyBalanceDTO> nextMonthBalanceOpt =
        monthlyBalanceInMemoQuery.findByAccountIdAndPeriod(accountId, monthlyPeriod.plusMonths(1));

    assertTrue(nextMonthBalanceOpt.isPresent());
    final AccountMonthlyBalanceDTO nextMonthBalance = nextMonthBalanceOpt.get();
    assertEquals(monthlyPeriod.plusMonths(1), nextMonthBalance.getPeriod());
    assertEquals(withJBHDecimals(closingBalanceNov24), nextMonthBalance.getOpeningBalance());
    assertEquals(JBH_ZERO, nextMonthBalance.getMovementBalance());
    assertEquals(JBH_ZERO, nextMonthBalance.getClosingBalance());
    assertEquals(JBH_ZERO, nextMonthBalance.getTotalDebits());
    assertEquals(JBH_ZERO, nextMonthBalance.getTotalCredits());
    assertEquals(JBH_ZERO, nextMonthBalance.getMonthlyProfit());
    assertEquals(JBH_ZERO, nextMonthBalance.getMonthlyExpenses());
    assertEquals(0, nextMonthBalance.getTotalMovements());
    assertFalse(nextMonthBalance.isGapPeriod());

    assertEquals(++totalMonthsCreated, inMemoryMonthlyBalanceRepos.getQueryRepo().size());
  }

  public void logBlockHeader(final String text) {
    final int blockWidth = 60; // Total width of the block
    final char borderChar = '=';

    // Create the border line
    final String borderLine = String.valueOf(borderChar).repeat(blockWidth);

    // Calculate padding for centering the text
    final int textLength = text.length();
    final int totalPadding = blockWidth - textLength;
    final int leftPadding = totalPadding / 2;
    final int rightPadding = totalPadding - leftPadding;

    // Create the centered text line
    final String centeredText = " ".repeat(leftPadding) + text + " ".repeat(rightPadding);

    // Log the block header
    LOG.info(borderLine);
    LOG.info(centeredText);
    LOG.info(borderLine);
  }

  private void addMovement(final AddMovementCommand movement) {
    addMovementUseCase.addMovement(userId, accountId, movement);
    try {
      Thread.sleep(Duration.ofSeconds(1).toMillis());
    } catch (final InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  @Order(2)
  void settingReportedBalanceDec24() {
    // Given
    final YearMonth monthlyPeriod = YearMonth.of(2024, 12);
    final LocalDate runningDate = monthlyPeriod.plusMonths(1).atDay(1);
    final BigDecimal salaryAmount = withJBHDecimals(new BigDecimal("6000000"));

    logBlockHeader("TESTING " + monthlyPeriod);

    final AccountDomain accountDomain =
        AccountDomain.withBasicMovementForExisting(accountId, JBH_ZERO, closingBalanceNov24);
    when(accountRepository.findByAccountId(userId, accountId))
        .thenReturn(Optional.of(AccountMapper.toDTO(accountDomain)));

    final AddMovementCommand salaryMovement =
        new AddMovementCommand(
            LocalDate.of(2024, 12, 15),
            withJBHDecimals(salaryAmount),
            MovementCategoryDTO.withType(IncomeCategory.SALARY));
    addMovement(salaryMovement);

    final AddMonthlyBalanceCommand command =
        new AddMonthlyBalanceCommand(monthlyPeriod, closingBalanceDec24, null);

    final AtomicReference<AccountMonthlyBalanceDTO> savedMonthlyBalance = new AtomicReference<>();
    Assertions.assertDoesNotThrow(
        () ->
            savedMonthlyBalance.set(
                useCaseTest.registerMonthlyBalance(runningDate, userId, accountId, command)));

    // Then
    final var actualMonthlyBalance = savedMonthlyBalance.get();
    assertEquals(monthlyPeriod, actualMonthlyBalance.getPeriod());
    assertEquals(withJBHDecimals(closingBalanceDec24), actualMonthlyBalance.getClosingBalance());
    assertEquals(withJBHDecimals(salaryAmount), actualMonthlyBalance.getMovementBalance());
    assertEquals(salaryAmount, actualMonthlyBalance.getTotalDebits());
    assertEquals(JBH_ZERO, actualMonthlyBalance.getTotalCredits());
    assertEquals(JBH_ZERO, actualMonthlyBalance.getMonthlyProfit());
    assertEquals(new BigDecimal("-4610597.00"), actualMonthlyBalance.getMonthlyExpenses());
    assertEquals(1, actualMonthlyBalance.getTotalMovements());
    assertFalse(actualMonthlyBalance.isGapPeriod());
    assertTrue(actualMonthlyBalance.isOfficialMonthlyReport());

    // Verifying the opening balance for the next month
    final Optional<AccountMonthlyBalanceDTO> nextMonthBalanceOpt =
        monthlyBalanceInMemoQuery.findByAccountIdAndPeriod(accountId, monthlyPeriod.plusMonths(1));

    assertTrue(nextMonthBalanceOpt.isPresent());
    final AccountMonthlyBalanceDTO nextMonthBalance = nextMonthBalanceOpt.get();
    assertEquals(monthlyPeriod.plusMonths(1), nextMonthBalance.getPeriod());
    assertEquals(withJBHDecimals(closingBalanceDec24), nextMonthBalance.getOpeningBalance());
    assertEquals(JBH_ZERO, nextMonthBalance.getMovementBalance());
    assertEquals(JBH_ZERO, nextMonthBalance.getClosingBalance());
    assertEquals(JBH_ZERO, nextMonthBalance.getTotalDebits());
    assertEquals(JBH_ZERO, nextMonthBalance.getTotalCredits());
    assertEquals(JBH_ZERO, nextMonthBalance.getMonthlyProfit());
    assertEquals(JBH_ZERO, nextMonthBalance.getMonthlyExpenses());
    assertEquals(0, nextMonthBalance.getTotalMovements());
    assertFalse(nextMonthBalance.isGapPeriod());

    assertEquals(++totalMonthsCreated, inMemoryMonthlyBalanceRepos.getQueryRepo().size());
  }

  @Test
  @Order(3)
  void settingReportedBalanceJan25() {
    // Given
    final YearMonth monthlyPeriod = YearMonth.of(2025, 1);
    final LocalDate runningDate = monthlyPeriod.plusMonths(1).atDay(1);
    final BigDecimal salaryAmount = withJBHDecimals(new BigDecimal("7000000.00"));

    logBlockHeader("TESTING " + monthlyPeriod);

    final AccountDomain accountDomain =
        AccountDomain.withBasicMovementForExisting(accountId, JBH_ZERO, closingBalanceDec24);
    when(accountRepository.findByAccountId(userId, accountId))
        .thenReturn(Optional.of(AccountMapper.toDTO(accountDomain)));

    addMovement(
        new AddMovementCommand(
            LocalDate.of(2025, 1, 15),
            withJBHDecimals(salaryAmount),
            MovementCategoryDTO.withType(IncomeCategory.SALARY)));

    final AddMonthlyBalanceCommand command =
        new AddMonthlyBalanceCommand(monthlyPeriod, closingBalanceJan25, null);

    final AtomicReference<AccountMonthlyBalanceDTO> savedMonthlyBalance = new AtomicReference<>();
    Assertions.assertDoesNotThrow(
        () ->
            savedMonthlyBalance.set(
                useCaseTest.registerMonthlyBalance(runningDate, userId, accountId, command)));

    // Then
    final var actualMonthlyBalance = savedMonthlyBalance.get();
    assertEquals(monthlyPeriod, actualMonthlyBalance.getPeriod());
    assertEquals(withJBHDecimals(closingBalanceJan25), actualMonthlyBalance.getClosingBalance());
    assertEquals(withJBHDecimals(salaryAmount), actualMonthlyBalance.getMovementBalance());
    assertEquals(salaryAmount, actualMonthlyBalance.getTotalDebits());
    assertEquals(JBH_ZERO, actualMonthlyBalance.getTotalCredits());
    assertEquals(JBH_ZERO, actualMonthlyBalance.getMonthlyProfit());
    assertEquals(new BigDecimal("6777637.00"), actualMonthlyBalance.getMonthlyExpenses());
    assertEquals(1, actualMonthlyBalance.getTotalMovements());
    assertFalse(actualMonthlyBalance.isGapPeriod());
    assertTrue(actualMonthlyBalance.isOfficialMonthlyReport());

    // Verifying the opening balance for the next month
    final Optional<AccountMonthlyBalanceDTO> nextMonthBalanceOpt =
        monthlyBalanceInMemoQuery.findByAccountIdAndPeriod(accountId, monthlyPeriod.plusMonths(1));

    assertTrue(nextMonthBalanceOpt.isPresent());
    final AccountMonthlyBalanceDTO nextMonthBalance = nextMonthBalanceOpt.get();
    assertEquals(monthlyPeriod.plusMonths(1), nextMonthBalance.getPeriod());
    assertEquals(withJBHDecimals(closingBalanceJan25), nextMonthBalance.getOpeningBalance());
    assertEquals(JBH_ZERO, nextMonthBalance.getMovementBalance());
    assertEquals(JBH_ZERO, nextMonthBalance.getClosingBalance());
    assertEquals(JBH_ZERO, nextMonthBalance.getTotalDebits());
    assertEquals(JBH_ZERO, nextMonthBalance.getTotalCredits());
    assertEquals(JBH_ZERO, nextMonthBalance.getMonthlyProfit());
    assertEquals(JBH_ZERO, nextMonthBalance.getMonthlyExpenses());
    assertEquals(0, nextMonthBalance.getTotalMovements());
    assertFalse(nextMonthBalance.isGapPeriod());

    assertEquals(++totalMonthsCreated, inMemoryMonthlyBalanceRepos.getQueryRepo().size());
  }

  @Test
  @Order(4)
  void settingReportedBalanceFeb25WithExistingMovements() {
    // Given some movements of february before reported balance
    final YearMonth monthlyPeriod = YearMonth.of(2025, 2);

    logBlockHeader("TESTING " + monthlyPeriod);

    final AccountDomain accountDomain =
        AccountDomain.withBasicMovementForExisting(accountId, JBH_ZERO, closingBalanceJan25);
    when(accountRepository.findByAccountId(userId, accountId))
        .thenReturn(Optional.of(AccountMapper.toDTO(accountDomain)));

    // SS Movement
    final var ssAmount = new BigDecimal("457100.00");
    final AddMovementCommand ssMovement =
        new AddMovementCommand(
            LocalDate.of(2025, 2, 1),
            withJBHDecimals(ssAmount),
            null,
            MovementType.WITHDRAWAL,
            SS_EXPENSE_CATEGORY);

    addMovement(ssMovement);

    // Public Service Movement
    final var publicSrvAmount = new BigDecimal("203000.00");
    final AddMovementCommand publicServiceMovement =
        new AddMovementCommand(
            LocalDate.of(2025, 2, 10),
            withJBHDecimals(publicSrvAmount),
            null,
            MovementType.WITHDRAWAL,
            MovementCategoryDTO.withType(ExpenseCategory.PUBLIC_SERVICES));
    addMovement(publicServiceMovement);

    // Personal Movement
    final var personalAmount = new BigDecimal("105000.00");
    final AddMovementCommand personalMovement =
        new AddMovementCommand(
            LocalDate.of(2025, 2, 15),
            withJBHDecimals(personalAmount),
            MovementCategoryDTO.withType(ExpenseCategory.PERSONAL));
    addMovement(personalMovement);

    // Salary Movement
    final var salaryAmount = new BigDecimal("7000000.00");
    final AddMovementCommand salaryMovement =
        new AddMovementCommand(
            LocalDate.of(2025, 2, 15),
            withJBHDecimals(salaryAmount),
            MovementCategoryDTO.withType(IncomeCategory.SALARY));
    addMovement(salaryMovement);

    // Given Reported Balance

    final LocalDate runningDate = monthlyPeriod.plusMonths(1).atDay(1);

    final AddMonthlyBalanceCommand command =
        new AddMonthlyBalanceCommand(monthlyPeriod, closingBalanceFeb25, null);

    // When
    final AtomicReference<AccountMonthlyBalanceDTO> savedMonthlyBalance = new AtomicReference<>();
    Assertions.assertDoesNotThrow(
        () ->
            savedMonthlyBalance.set(
                useCaseTest.registerMonthlyBalance(runningDate, userId, accountId, command)));

    // Then
    final var actualMonthlyBalance = savedMonthlyBalance.get();
    assertEquals(monthlyPeriod, actualMonthlyBalance.getPeriod());
    assertEquals(withJBHDecimals(closingBalanceFeb25), actualMonthlyBalance.getClosingBalance());
    assertEquals(
        salaryAmount.subtract(ssAmount).subtract(publicSrvAmount).subtract(personalAmount),
        actualMonthlyBalance.getMovementBalance());
    assertEquals(salaryAmount, actualMonthlyBalance.getTotalDebits());
    assertEquals(
        ssAmount.add(publicSrvAmount).add(personalAmount), actualMonthlyBalance.getTotalCredits());
    assertEquals(JBH_ZERO, actualMonthlyBalance.getMonthlyProfit());
    assertEquals(new BigDecimal("680430.00"), actualMonthlyBalance.getMonthlyExpenses());
    assertEquals(4, actualMonthlyBalance.getTotalMovements());
    assertFalse(actualMonthlyBalance.isGapPeriod());
    assertTrue(actualMonthlyBalance.isOfficialMonthlyReport());

    // Verifying the opening balance for the next month
    final Optional<AccountMonthlyBalanceDTO> nextMonthBalanceOpt =
        monthlyBalanceInMemoQuery.findByAccountIdAndPeriod(accountId, monthlyPeriod.plusMonths(1));

    assertTrue(nextMonthBalanceOpt.isPresent());
    final AccountMonthlyBalanceDTO nextMonthBalance = nextMonthBalanceOpt.get();
    assertEquals(monthlyPeriod.plusMonths(1), nextMonthBalance.getPeriod());
    assertEquals(withJBHDecimals(closingBalanceFeb25), nextMonthBalance.getOpeningBalance());
    assertEquals(JBH_ZERO, nextMonthBalance.getMovementBalance());
    assertEquals(JBH_ZERO, nextMonthBalance.getClosingBalance());
    assertEquals(JBH_ZERO, nextMonthBalance.getTotalDebits());
    assertEquals(JBH_ZERO, nextMonthBalance.getTotalCredits());
    assertEquals(JBH_ZERO, nextMonthBalance.getMonthlyProfit());
    assertEquals(JBH_ZERO, nextMonthBalance.getMonthlyExpenses());
    assertEquals(0, nextMonthBalance.getTotalMovements());
    assertFalse(nextMonthBalance.isGapPeriod());

    assertEquals(++totalMonthsCreated, inMemoryMonthlyBalanceRepos.getQueryRepo().size());
  }
}
