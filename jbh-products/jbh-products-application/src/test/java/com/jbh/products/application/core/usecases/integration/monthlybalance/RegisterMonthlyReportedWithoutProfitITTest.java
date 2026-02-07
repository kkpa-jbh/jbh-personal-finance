package com.jbh.products.application.core.usecases.integration.monthlybalance;

import static com.jbh.commons.util.JbhMoneyUtils.JBH_ZERO;
import static com.jbh.commons.util.JbhMoneyUtils.withJBHDecimals;
import static com.jbh.products.application.builders.CommandTestBuilder.createBasicAccountCommand;
import static com.jbh.products.application.builders.UseCaseBuilder.DEFAULT_ACCOUNT_NAME;
import static com.jbh.products.application.builders.UseCaseBuilder.DEFAULT_ACCOUNT_TYPE;
import static com.jbh.products.application.core.usecases.utils.AccountITUtils.assertAccount;
import static com.jbh.products.application.core.usecases.utils.MonthlyBalanceITUtils.assertMonthlyBalance;
import static com.jbh.products.application.core.usecases.utils.MonthlyBalanceITUtils.createMonthlyBalanceCommand;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jbh.commons.exception.BusinessException;
import com.jbh.products.application.builders.UseCaseBuilder;
import com.jbh.products.application.core.dto.MonthlyBalanceDTO;
import com.jbh.products.application.core.dto.ProductDTO;
import com.jbh.products.application.core.dto.ProductDTO.ProductDTOBuilder;
import com.jbh.products.application.core.ports.output.account.InMemoryAccountRepository;
import com.jbh.products.application.core.ports.output.monthlybalance.AccountMonthlyBalanceQueryRepo;
import com.jbh.products.application.core.ports.output.monthlybalance.AccountMonthlyBalanceWriterRepository;
import com.jbh.products.application.core.ports.output.monthlybalance.InMemoryMonthlyBalanceRepositories;
import com.jbh.products.application.core.services.monthlybalance.MonthlyBalanceService;
import com.jbh.products.application.core.usecases.AddMovementUseCase;
import com.jbh.products.application.core.usecases.CreateProductUseCase;
import com.jbh.products.application.core.usecases.FindMonthlyBalanceUseCase;
import com.jbh.products.application.core.usecases.RegisterMonthlyBalanceUseCase;
import com.jbh.products.application.core.usecases.utils.IgnoreAccountOptions;
import com.jbh.products.application.core.vo.commands.AddMonthlyBalanceCommand;
import com.jbh.products.application.core.vo.commands.AddMovementCommand;
import com.jbh.products.application.movements.ports.output.AccountMovementWriterRepository;
import com.jbh.products.domain.movement.vo.ExpenseCategory;
import com.jbh.products.domain.movement.vo.IncomeCategory;
import com.jbh.products.domain.movement.vo.MovementCategoryVO;
import com.jbh.products.domain.movement.vo.MovementType;
import com.jbh.products.domain.product.vo.ProductId;
import com.jbh.products.domain.product.vo.ProductPK;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
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

  public static final BigDecimal monthlyExpensesFeb25 =
      withJBHDecimals(new BigDecimal("680430.00"));
  static final BigDecimal closingBalanceNov24 = withJBHDecimals(new BigDecimal("17676950.00"));
  static final BigDecimal closingBalanceDec24 = withJBHDecimals(new BigDecimal("28287547.00"));
  static final BigDecimal closingBalanceJan25 = withJBHDecimals(new BigDecimal("28509910.00"));
  static final BigDecimal closingBalanceFeb25 = withJBHDecimals(new BigDecimal("34064380.00"));
  static final BigDecimal closingBalanceMar25 = withJBHDecimals(new BigDecimal("24971093.00"));
  static final BigDecimal closingBalanceAug25 = withJBHDecimals(new BigDecimal("9017069.00"));
  static final BigDecimal salaryAmountNov24 = withJBHDecimals(new BigDecimal("10000000"));
  static final BigDecimal salaryAmountDec24 = withJBHDecimals(new BigDecimal("6000000"));

  static final BigDecimal salaryAmountJan25 = withJBHDecimals(new BigDecimal("7000000"));
  static final BigDecimal salaryAmountFeb25 = withJBHDecimals(new BigDecimal("7000000"));
  static final BigDecimal salaryAmountMar25 = withJBHDecimals(new BigDecimal("7000000.00"));
  static final BigDecimal salaryAmountApr25 = withJBHDecimals(new BigDecimal("16000000"));
  static final BigDecimal salaryAmountMay25 = withJBHDecimals(new BigDecimal("16000000"));
  private static final Logger LOG =
      LoggerFactory.getLogger(RegisterMonthlyReportedWithoutProfitITTest.class);
  private static final InMemoryAccountRepository inMemoryAccountRepo =
      UseCaseBuilder.getAccountRepository();
  static BigDecimal expensesFeb25 = withJBHDecimals(BigDecimal.ZERO);
  // Static to be shared between tests
  static UUID userId = UUID.randomUUID();
  static InMemoryMonthlyBalanceRepositories inMemoryMonthlyBalanceRepos =
      UseCaseBuilder.getInMemoryMonthlyBalanceRepos();
  // Static to be shared between tests
  static int totalMonthsCreated = 1;
  static ProductDTO createdAccount;
  static ProductId accountId;
  @Mock private static AccountMovementWriterRepository accountMovementRepository;
  private static MonthlyBalanceService monthlyBalanceService;
  private static RegisterMonthlyBalanceUseCase useCaseTest;
  ;
  private static AddMovementUseCase addMovementUseCase;
  private static ProductDTO finalAccountBalance;
  public final ProductDTOBuilder ACCOUNT_DEFAULT_BUILDER =
      ProductDTO.defaultBuilder(userId, accountId, DEFAULT_ACCOUNT_NAME, DEFAULT_ACCOUNT_TYPE);
  AccountMonthlyBalanceWriterRepository monthlyBalanceInMemoWriter =
      inMemoryMonthlyBalanceRepos.getWriterRepo();
  AccountMonthlyBalanceQueryRepo monthlyBalanceInMemoQuery =
      inMemoryMonthlyBalanceRepos.getQueryRepo();
  private FindMonthlyBalanceUseCase findMonthlyBalanceUseCase;
  private CreateProductUseCase createAccountUseCase;

  @BeforeAll
  static void beforeAll() {
    inMemoryMonthlyBalanceRepos.clearStorage();
    inMemoryAccountRepo.clearStorage();
  }

  private static void refreshActualAccountBalance() {
    finalAccountBalance = inMemoryAccountRepo.findByProductId(accountId).get();
  }

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    monthlyBalanceService = UseCaseBuilder.buildMonthlyBalanceService();
    useCaseTest = UseCaseBuilder.buildRegisterMonthlyBalanceUseCase(accountMovementRepository);

    addMovementUseCase = UseCaseBuilder.buildAddMovementUseCase(accountMovementRepository);
    createAccountUseCase = UseCaseBuilder.buildCreateAccountUseCase();
    findMonthlyBalanceUseCase = UseCaseBuilder.buildFindMonthlyBalanceUseCase();

    UseCaseBuilder.delayTests();
  }

  @Test
  @Order(1)
  void settingInitialReportedBalanceNov24() throws BusinessException {

    // Given
    createdAccount =
        createAccountUseCase.execute(
            createBasicAccountCommand(userId, DEFAULT_ACCOUNT_NAME, DEFAULT_ACCOUNT_TYPE));
    accountId = createdAccount.id();
    LOG.info("Account created with id {}", accountId);
    assertNotNull(accountId);

    final YearMonth monthlyPeriod = YearMonth.of(2024, 11);
    final LocalDate runningDate = monthlyPeriod.plusMonths(1).atDay(1);

    logBlockHeader("TESTING " + monthlyPeriod);

    final AddMovementCommand salaryMovement =
        AddMovementCommand.withCategory(
                LocalDate.of(2024, 11, 15),
                withJBHDecimals(salaryAmountNov24),
                MovementCategoryVO.withType(IncomeCategory.SALARY))
            .build();
    addMovement(salaryMovement);

    final AddMonthlyBalanceCommand command =
        createMonthlyBalanceCommand(monthlyPeriod, closingBalanceNov24, null);

    final AtomicReference<MonthlyBalanceDTO> savedMonthlyBalance = new AtomicReference<>();
    Assertions.assertDoesNotThrow(
        () ->
            savedMonthlyBalance.set(
                useCaseTest.registerOfficialMonthlyBalance(
                    runningDate, userId, accountId, command)));

    // Then
    final var actualMonthlyBalance = savedMonthlyBalance.get();
    final var expectedMonthlyBalance =
        MonthlyBalanceDTO.defaultBuilder()
            .accountId(accountId)
            .period(monthlyPeriod)
            .closingBalance(withJBHDecimals(closingBalanceNov24))
            .totalDebits(withJBHDecimals(salaryAmountNov24))
            .totalCredits(JBH_ZERO)
            .movementBalance(withJBHDecimals(salaryAmountNov24))
            .totalMovements(1)
            .officialMonthlyReport(true)
            .monthlyNetProfit(new BigDecimal("7676950.00"))
            .build();
    assertMonthlyBalance(expectedMonthlyBalance, actualMonthlyBalance);

    // Verifying the opening balance for the next month
    final Optional<MonthlyBalanceDTO> nextMonthBalanceOpt =
        monthlyBalanceInMemoQuery.findByAccountIdAndPeriod(accountId, monthlyPeriod.plusMonths(1));

    assertTrue(nextMonthBalanceOpt.isPresent());
    final MonthlyBalanceDTO actualNextMonthBalance = nextMonthBalanceOpt.get();
    final var expectedNextMonth = MonthlyBalanceDTO.defaultBuilder();
    expectedNextMonth.period(monthlyPeriod.plusMonths(1));
    expectedNextMonth.openingBalance(withJBHDecimals(closingBalanceNov24));

    assertMonthlyBalance(expectedNextMonth.build(), actualNextMonthBalance);

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
    try {
      addMovementUseCase.addMovement(userId, accountId, movement);
    } catch (final BusinessException e) {
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
  void settingReportedBalanceDec24() {
    // Given
    final YearMonth monthlyPeriod = YearMonth.of(2024, 12);
    final LocalDate runningDate = monthlyPeriod.plusMonths(1).atDay(1);

    logBlockHeader("TESTING " + monthlyPeriod);

    final ProductDTO expectedAccount =
        ACCOUNT_DEFAULT_BUILDER
            .currentBalance(withJBHDecimals(closingBalanceNov24))
            .movementBalance(withJBHDecimals(salaryAmountNov24))
            .build();

    final ProductDTO persistedAccount =
        inMemoryAccountRepo.findByUserAndProductId(userId, accountId).get();
    assertAccount(
        (expectedAccount),
        persistedAccount,
        IgnoreAccountOptions.IGNORE_ACCOUNT_NAME,
        IgnoreAccountOptions.IGNORE_ACCOUNT_TYPE);

    final AddMovementCommand salaryMovement =
        AddMovementCommand.withCategory(
                LocalDate.of(2024, 12, 15),
                withJBHDecimals(salaryAmountDec24),
                MovementCategoryVO.withType(IncomeCategory.SALARY))
            .build();
    addMovement(salaryMovement);

    final AddMonthlyBalanceCommand command =
        createMonthlyBalanceCommand(monthlyPeriod, closingBalanceDec24, null);

    final AtomicReference<MonthlyBalanceDTO> savedMonthlyBalance = new AtomicReference<>();
    Assertions.assertDoesNotThrow(
        () ->
            savedMonthlyBalance.set(
                useCaseTest.registerOfficialMonthlyBalance(
                    runningDate, userId, accountId, command)));

    // Then
    final var actualMonthlyBalance = savedMonthlyBalance.get();
    assertEquals(monthlyPeriod, actualMonthlyBalance.period());

    final var expectedMonthlyBalance = MonthlyBalanceDTO.defaultBuilder();
    expectedMonthlyBalance.period(monthlyPeriod);
    expectedMonthlyBalance.openingBalance(withJBHDecimals(closingBalanceNov24));
    expectedMonthlyBalance.closingBalance(withJBHDecimals(closingBalanceDec24));
    expectedMonthlyBalance.movementBalance(withJBHDecimals(salaryAmountDec24));
    expectedMonthlyBalance.totalDebits(salaryAmountDec24);
    expectedMonthlyBalance.monthlyNetProfit(new BigDecimal("-4610597.00"));
    expectedMonthlyBalance.monthlyNetProfit(
        withJBHDecimals(
            closingBalanceDec24.subtract(closingBalanceNov24).subtract(salaryAmountDec24)));
    expectedMonthlyBalance.totalMovements(1);
    expectedMonthlyBalance.officialMonthlyReport(true);
    assertMonthlyBalance(expectedMonthlyBalance.build(), actualMonthlyBalance);

    // Verifying the opening balance for the next month
    final Optional<MonthlyBalanceDTO> nextMonthBalanceOpt =
        monthlyBalanceInMemoQuery.findByAccountIdAndPeriod(accountId, monthlyPeriod.plusMonths(1));

    assertTrue(nextMonthBalanceOpt.isPresent());
    final MonthlyBalanceDTO nextMonthBalance = nextMonthBalanceOpt.get();
    assertEquals(monthlyPeriod.plusMonths(1), nextMonthBalance.period());
    assertEquals(withJBHDecimals(closingBalanceDec24), nextMonthBalance.openingBalance());
    assertEquals(JBH_ZERO, nextMonthBalance.movementBalance());
    assertEquals(JBH_ZERO, nextMonthBalance.closingBalance());
    assertEquals(JBH_ZERO, nextMonthBalance.totalDebits());
    assertEquals(JBH_ZERO, nextMonthBalance.totalCredits());
    assertEquals(JBH_ZERO, nextMonthBalance.monthlyNetProfit());
    assertEquals(0, nextMonthBalance.totalMovements());
    assertFalse(nextMonthBalance.gapPeriod());

    assertEquals(++totalMonthsCreated, inMemoryMonthlyBalanceRepos.getQueryRepo().size());
  }

  @Test
  @Order(3)
  void settingReportedBalanceJan25() {
    // Given
    final YearMonth monthlyPeriod = YearMonth.of(2025, 1);
    final LocalDate runningDate = monthlyPeriod.plusMonths(1).atDay(1);

    logBlockHeader("TESTING " + monthlyPeriod);

    final ProductDTO expectedAccount =
        ACCOUNT_DEFAULT_BUILDER
            .currentBalance(withJBHDecimals(closingBalanceDec24))
            .movementBalance(withJBHDecimals(salaryAmountDec24).add(salaryAmountNov24))
            .netProfitBalance(withJBHDecimals(new BigDecimal("7676950.00")))
            .netGrowthRate(withJBHDecimals(new BigDecimal("59.05")))
            .build();
    final ProductDTO persistedAccount =
        inMemoryAccountRepo.findByUserAndProductId(userId, accountId).get();
    assertAccount(expectedAccount, persistedAccount);

    addMovement(
        AddMovementCommand.withCategory(
                LocalDate.of(2025, 1, 15),
                withJBHDecimals(salaryAmountJan25),
                MovementCategoryVO.withType(IncomeCategory.SALARY))
            .build());

    final AddMonthlyBalanceCommand command =
        createMonthlyBalanceCommand(monthlyPeriod, closingBalanceJan25, null);

    final AtomicReference<MonthlyBalanceDTO> savedMonthlyBalance = new AtomicReference<>();
    Assertions.assertDoesNotThrow(
        () ->
            savedMonthlyBalance.set(
                useCaseTest.registerOfficialMonthlyBalance(
                    runningDate, userId, accountId, command)));

    // Then
    final var actualMonthlyBalance = savedMonthlyBalance.get();
    assertEquals(monthlyPeriod, actualMonthlyBalance.period());
    assertEquals(withJBHDecimals(closingBalanceJan25), actualMonthlyBalance.closingBalance());
    assertEquals(withJBHDecimals(salaryAmountJan25), actualMonthlyBalance.movementBalance());
    assertEquals(salaryAmountJan25, actualMonthlyBalance.totalDebits());
    assertEquals(JBH_ZERO, actualMonthlyBalance.totalCredits());
    assertEquals(new BigDecimal("-6777637.00"), actualMonthlyBalance.monthlyNetProfit());
    assertEquals(1, actualMonthlyBalance.totalMovements());
    assertFalse(actualMonthlyBalance.gapPeriod());
    assertTrue(actualMonthlyBalance.officialMonthlyReport());

    // Verifying the opening balance for the next month
    final Optional<MonthlyBalanceDTO> nextMonthBalanceOpt =
        monthlyBalanceInMemoQuery.findByAccountIdAndPeriod(accountId, monthlyPeriod.plusMonths(1));

    assertTrue(nextMonthBalanceOpt.isPresent());
    final MonthlyBalanceDTO nextMonthBalance = nextMonthBalanceOpt.get();
    assertEquals(monthlyPeriod.plusMonths(1), nextMonthBalance.period());
    assertEquals(withJBHDecimals(closingBalanceJan25), nextMonthBalance.openingBalance());
    assertEquals(JBH_ZERO, nextMonthBalance.movementBalance());
    assertEquals(JBH_ZERO, nextMonthBalance.closingBalance());
    assertEquals(JBH_ZERO, nextMonthBalance.totalDebits());
    assertEquals(JBH_ZERO, nextMonthBalance.totalCredits());
    assertEquals(JBH_ZERO, nextMonthBalance.monthlyNetProfit());
    assertEquals(0, nextMonthBalance.totalMovements());
    assertFalse(nextMonthBalance.gapPeriod());

    assertEquals(++totalMonthsCreated, inMemoryMonthlyBalanceRepos.getQueryRepo().size());
  }

  @Test
  @Order(4)
  void settingReportedBalanceFeb25WithExistingMovements() {
    // Given some movements of february before reported balance
    final YearMonth monthlyPeriod202502 = YearMonth.of(2025, 2);

    logBlockHeader("TESTING " + monthlyPeriod202502);

    // SS Movement
    final var ssAmount = new BigDecimal("457100.00");
    final AddMovementCommand ssMovement =
        AddMovementCommand.builder()
            .entryDate(LocalDate.of(2025, 2, 1))
            .totalAmount(withJBHDecimals(ssAmount))
            .movementType(MovementType.WITHDRAWAL)
            .categoryDTO(MovementCategoryVO.withType(ExpenseCategory.SOCIAL_SECURITY))
            .build();

    expensesFeb25 = expensesFeb25.add(ssAmount);
    addMovement(ssMovement);

    // Public Service Movement
    final var publicSrvAmount = new BigDecimal("203000.00");
    final AddMovementCommand publicServiceMovement =
        AddMovementCommand.builder()
            .entryDate(LocalDate.of(2025, 2, 10))
            .totalAmount(withJBHDecimals(publicSrvAmount))
            .movementType(MovementType.WITHDRAWAL)
            .categoryDTO(MovementCategoryVO.withType(ExpenseCategory.PUBLIC_SERVICES))
            .build();
    expensesFeb25 = expensesFeb25.add(publicSrvAmount);
    addMovement(publicServiceMovement);

    // Personal Movement
    final var personalAmount = new BigDecimal("105000.00");
    final AddMovementCommand personalMovement =
        AddMovementCommand.withCategory(
                LocalDate.of(2025, 2, 15),
                withJBHDecimals(personalAmount),
                MovementCategoryVO.withType(ExpenseCategory.PERSONAL))
            .build();
    expensesFeb25 = expensesFeb25.add(personalAmount);
    addMovement(personalMovement);

    // Salary Movement
    final AddMovementCommand salaryMovement =
        AddMovementCommand.withCategory(
                LocalDate.of(2025, 2, 15),
                withJBHDecimals(salaryAmountFeb25),
                MovementCategoryVO.withType(IncomeCategory.SALARY))
            .build();
    addMovement(salaryMovement);

    // Given Reported Balance

    final LocalDate runningDate = monthlyPeriod202502.plusMonths(1).atDay(1);

    final AddMonthlyBalanceCommand command =
        createMonthlyBalanceCommand(monthlyPeriod202502, closingBalanceFeb25, null);

    // When
    final AtomicReference<MonthlyBalanceDTO> savedMonthlyBalance = new AtomicReference<>();
    Assertions.assertDoesNotThrow(
        () ->
            savedMonthlyBalance.set(
                useCaseTest.registerOfficialMonthlyBalance(
                    runningDate, userId, accountId, command)));

    // Then
    final var actualMonthlyBalance = savedMonthlyBalance.get();
    final var expectedMonthlyBalance20252 =
        MonthlyBalanceDTO.defaultBuilder()
            .accountId(accountId)
            .period(monthlyPeriod202502)
            .openingBalance(closingBalanceJan25)
            .closingBalance(closingBalanceFeb25)
            .totalDebits(salaryAmountFeb25)
            .totalCredits(expensesFeb25)
            .movementBalance(salaryAmountFeb25.subtract(expensesFeb25))
            .totalMovements(4)
            .officialMonthlyReport(true)
            .monthlyNetProfit(monthlyExpensesFeb25.negate())
            .build();
    assertMonthlyBalance(expectedMonthlyBalance20252, actualMonthlyBalance);

    // Verifying the opening balance for the next month
    final Optional<MonthlyBalanceDTO> nextMonthBalanceOpt =
        monthlyBalanceInMemoQuery.findByAccountIdAndPeriod(
            accountId, monthlyPeriod202502.plusMonths(1));

    assertTrue(nextMonthBalanceOpt.isPresent());
    final MonthlyBalanceDTO nextMonthBalance = nextMonthBalanceOpt.get();
    assertEquals(monthlyPeriod202502.plusMonths(1), nextMonthBalance.period());
    assertEquals(withJBHDecimals(closingBalanceFeb25), nextMonthBalance.openingBalance());
    assertEquals(JBH_ZERO, nextMonthBalance.movementBalance());
    assertEquals(JBH_ZERO, nextMonthBalance.closingBalance());
    assertEquals(JBH_ZERO, nextMonthBalance.totalDebits());
    assertEquals(JBH_ZERO, nextMonthBalance.totalCredits());
    assertEquals(JBH_ZERO, nextMonthBalance.monthlyNetProfit());
    assertEquals(0, nextMonthBalance.totalMovements());
    assertFalse(nextMonthBalance.gapPeriod());

    assertEquals(++totalMonthsCreated, inMemoryMonthlyBalanceRepos.getQueryRepo().size());

    // Add a movement after the monthly balance was registered oficially,
    // should not affect the monthly balance. Only the monthly expenses should be updated
    // and the total movements should be updated
    final var newAmount = new BigDecimal("100.00");
    final AddMovementCommand newMovementAfterClosedMonthlyBalance =
        AddMovementCommand.withCategory(
                LocalDate.of(2025, 2, 26),
                withJBHDecimals(newAmount),
                MovementCategoryVO.withType(ExpenseCategory.PUBLIC_SERVICES))
            .build();
    expensesFeb25 = expensesFeb25.add(newAmount);
    addMovement(newMovementAfterClosedMonthlyBalance);

    final MonthlyBalanceDTO updatedMonthlyBalance =
        monthlyBalanceInMemoQuery.findByAccountIdAndPeriod(accountId, monthlyPeriod202502).get();

    final var expectedUpdatedMonthlyBalance =
        MonthlyBalanceDTO.defaultBuilder()
            .accountId(accountId)
            .period(expectedMonthlyBalance20252.period())
            .openingBalance(expectedMonthlyBalance20252.openingBalance())
            .closingBalance(expectedMonthlyBalance20252.closingBalance())
            .totalDebits(salaryAmountFeb25)
            .totalCredits(expectedMonthlyBalance20252.totalCredits().add(newAmount))
            .movementBalance(expectedMonthlyBalance20252.movementBalance().subtract(newAmount))
            .totalMovements(5)
            .officialMonthlyReport(true)
            .monthlyNetProfit(monthlyExpensesFeb25.subtract(newAmount).negate())
            .build();

    assertMonthlyBalance(expectedUpdatedMonthlyBalance, updatedMonthlyBalance);

    final var fiftyMillionsExpenses = new BigDecimal("50000000.00");
    final AddMovementCommand fiftyMillionsExpensesCommand =
        AddMovementCommand.withCategory(
                LocalDate.of(2025, 2, 26),
                withJBHDecimals(fiftyMillionsExpenses),
                MovementCategoryVO.withType(ExpenseCategory.PERSONAL))
            .build();
    Assertions.assertThrows(
        RuntimeException.class, () -> addMovement(fiftyMillionsExpensesCommand));
  }

  @Test
  @Order(5)
  void settingReportedBalanceMar25() {
    // Given
    final YearMonth monthlyPeriod = YearMonth.of(2025, 3);
    final LocalDate runningDate = monthlyPeriod.plusMonths(1).atDay(1);

    logBlockHeader("TESTING " + monthlyPeriod);

    addMovement(
        AddMovementCommand.withCategory(
                LocalDate.of(monthlyPeriod.getYear(), monthlyPeriod.getMonthValue(), 15),
                withJBHDecimals(salaryAmountMar25),
                MovementCategoryVO.withType(IncomeCategory.SALARY))
            .build());

    final AddMonthlyBalanceCommand command =
        createMonthlyBalanceCommand(monthlyPeriod, closingBalanceMar25, null);

    final AtomicReference<MonthlyBalanceDTO> savedMonthlyBalance = new AtomicReference<>();
    Assertions.assertDoesNotThrow(
        () ->
            savedMonthlyBalance.set(
                useCaseTest.registerOfficialMonthlyBalance(
                    runningDate, userId, accountId, command)));

    // Then
    final var actualMonthlyBalance = savedMonthlyBalance.get();
    assertEquals(monthlyPeriod, actualMonthlyBalance.period());
    assertEquals(withJBHDecimals(closingBalanceMar25), actualMonthlyBalance.closingBalance());
    assertEquals(withJBHDecimals(salaryAmountMar25), actualMonthlyBalance.movementBalance());
    assertEquals(salaryAmountMar25, actualMonthlyBalance.totalDebits());
    assertEquals(JBH_ZERO, actualMonthlyBalance.totalCredits());

    final BigDecimal monthlyExp = withJBHDecimals(new BigDecimal("16093287.00"));
    assertEquals(monthlyExp.negate(), actualMonthlyBalance.monthlyNetProfit());
    assertEquals(1, actualMonthlyBalance.totalMovements());
    assertFalse(actualMonthlyBalance.gapPeriod());
    assertTrue(actualMonthlyBalance.officialMonthlyReport());

    // Verifying the opening balance for the next month
    final Optional<MonthlyBalanceDTO> nextMonthBalanceOpt =
        monthlyBalanceInMemoQuery.findByAccountIdAndPeriod(accountId, monthlyPeriod.plusMonths(1));

    assertTrue(nextMonthBalanceOpt.isPresent());
    final MonthlyBalanceDTO nextMonthBalance = nextMonthBalanceOpt.get();
    assertEquals(monthlyPeriod.plusMonths(1), nextMonthBalance.period());
    assertEquals(withJBHDecimals(closingBalanceMar25), nextMonthBalance.openingBalance());
    assertEquals(JBH_ZERO, nextMonthBalance.movementBalance());
    assertEquals(JBH_ZERO, nextMonthBalance.closingBalance());
    assertEquals(JBH_ZERO, nextMonthBalance.totalDebits());
    assertEquals(JBH_ZERO, nextMonthBalance.totalCredits());
    assertEquals(JBH_ZERO, nextMonthBalance.monthlyNetProfit());
    assertEquals(0, nextMonthBalance.totalMovements());
    assertFalse(nextMonthBalance.gapPeriod());

    assertEquals(++totalMonthsCreated, inMemoryMonthlyBalanceRepos.getQueryRepo().size());
  }

  @Test
  @Order(6)
  void notReportedBalanceApril25() {
    // Given
    final YearMonth monthlyPeriod = YearMonth.of(2025, 4);
    final LocalDate runningDate = monthlyPeriod.plusMonths(1).atDay(1);

    logBlockHeader("TESTING " + monthlyPeriod);

    addMovement(
        AddMovementCommand.withCategory(
                LocalDate.of(monthlyPeriod.getYear(), monthlyPeriod.getMonthValue(), 15),
                withJBHDecimals(salaryAmountApr25),
                MovementCategoryVO.withType(IncomeCategory.SALARY))
            .build());

    final AddMonthlyBalanceCommand command = createMonthlyBalanceCommand(monthlyPeriod, null);

    final AtomicReference<MonthlyBalanceDTO> savedMonthlyBalance = new AtomicReference<>();
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            savedMonthlyBalance.set(
                useCaseTest.registerOfficialMonthlyBalance(
                    runningDate, userId, accountId, command)));

    final AddMonthlyBalanceCommand command2 =
        createMonthlyBalanceCommand(monthlyPeriod, BigDecimal.ZERO, null);

    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> useCaseTest.registerOfficialMonthlyBalance(runningDate, userId, accountId, command2));
  }

  @Test
  @Order(7)
  void NotReportedBalanceMay25() {
    // Given
    final YearMonth monthlyPeriod = YearMonth.of(2025, 5);
    final LocalDate runningDate = monthlyPeriod.plusMonths(1).atDay(1);

    logBlockHeader("TESTING " + monthlyPeriod);

    addMovement(
        AddMovementCommand.withCategory(
                LocalDate.of(monthlyPeriod.getYear(), monthlyPeriod.getMonthValue(), 15),
                withJBHDecimals(salaryAmountMay25),
                MovementCategoryVO.withType(IncomeCategory.SALARY))
            .build());

    final AddMonthlyBalanceCommand command = createMonthlyBalanceCommand(monthlyPeriod, null);

    final AtomicReference<MonthlyBalanceDTO> savedMonthlyBalance = new AtomicReference<>();
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            savedMonthlyBalance.set(
                useCaseTest.registerOfficialMonthlyBalance(
                    runningDate, userId, accountId, command)));
  }

  @Test
  @Order(8)
  void notReportedBalanceAug25ShouldThrowException() {
    // Given
    final YearMonth monthlyPeriod = YearMonth.of(2025, 8);
    final LocalDate runningDate = monthlyPeriod.plusMonths(1).atDay(1);

    logBlockHeader("TESTING " + monthlyPeriod);

    final AddMonthlyBalanceCommand command =
        createMonthlyBalanceCommand(monthlyPeriod, closingBalanceAug25, null);

    final AtomicReference<MonthlyBalanceDTO> savedMonthlyBalance = new AtomicReference<>();
    Assertions.assertThrows(
        BusinessException.class,
        () ->
            savedMonthlyBalance.set(
                useCaseTest.registerOfficialMonthlyBalance(
                    runningDate, userId, accountId, command)));
  }

  @Test
  @Order(9)
  void verifyAccountAfterMonthlyBalancesGAP() {

    final Optional<MonthlyBalanceDTO> lastOfficialReport =
        monthlyBalanceService.findLastOfficialReport(accountId);
    assertTrue(lastOfficialReport.isPresent());
    final YearMonth lastOfficialReportPeriod = lastOfficialReport.get().period();
    assertEquals(YearMonth.of(2025, 3), lastOfficialReportPeriod);

    final ProductDTO expectedAccount =
        ACCOUNT_DEFAULT_BUILDER
            .netGrowthRate(withJBHDecimals(new BigDecimal("-18.70")))
            .currentBalance(
                withJBHDecimals(closingBalanceMar25).add(salaryAmountApr25).add(salaryAmountMay25))
            .movementBalance(
                salaryAmountNov24
                    .add(salaryAmountDec24)
                    .add(salaryAmountJan25)
                    .add(salaryAmountFeb25)
                    .add(salaryAmountMar25)
                    .add(salaryAmountApr25)
                    .add(salaryAmountMay25)
                    .subtract(expensesFeb25))
            .build();

    final ProductDTO persistedAccount =
        inMemoryAccountRepo.findByUserAndProductId(userId, accountId).get();
    assertAccount(expectedAccount, persistedAccount, IgnoreAccountOptions.IGNORE_ACCOUNT_PROFIT);
  }

  @Test
  @Order(99)
  void verifyAllMonthlyBalances() throws BusinessException {
    final YearMonth startPeriod = YearMonth.of(2024, 11);
    final YearMonth endPeriod = YearMonth.now();
    final List<MonthlyBalanceDTO> monthlyBalances =
        findMonthlyBalanceUseCase.findMonthlyBalancesByProduct(
            new ProductPK(userId, accountId), startPeriod, endPeriod);

    assertNotNull(monthlyBalances);
    monthlyBalances.forEach(
        monthlyBalanceDTO -> {
          LOG.info("Monthly Balance {}", monthlyBalanceDTO);
          assertNotNull(monthlyBalanceDTO);
        });
  }

  @Test
  @Order(99)
  void shouldThrowExceptionWhenAccountPKMistMatch() {
    final YearMonth startPeriod = YearMonth.of(2024, 11);
    final YearMonth endPeriod = YearMonth.now();

    assertThrows(
        BusinessException.class,
        () ->
            findMonthlyBalanceUseCase.findMonthlyBalancesByProduct(
                new ProductPK(UUID.randomUUID(), accountId), startPeriod, endPeriod));
  }
}
