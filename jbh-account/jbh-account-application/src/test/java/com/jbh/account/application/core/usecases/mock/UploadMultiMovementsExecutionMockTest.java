package com.jbh.account.application.core.usecases.mock;

import static com.jbh.account.application.core.mappers.MonthlyBalanceMapper.toDomain;
import static com.jbh.account.application.core.usecases.integration.monthlybalance.IgnoreOption.IGNORE_MONTHLY_PROFIT;
import static com.jbh.account.application.core.usecases.integration.monthlybalance.IgnoreOption.IGNORE_OPENING_BALANCE;
import static com.jbh.account.application.core.usecases.utils.CommandTestBuilder.createBasicAccountCommand;
import static com.jbh.account.application.core.usecases.utils.MonthlyBalanceITUtils.assertMonthlyBalance;
import static com.jbh.account.domain.utils.JbhMoneyUtils.JBH_ZERO;
import static com.jbh.account.domain.utils.JbhMoneyUtils.withJBHDecimals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jbh.account.application.acid.UnitOfWork;
import com.jbh.account.application.async.AsyncTaskExecutorImpl;
import com.jbh.account.application.core.dto.AccountDTO;
import com.jbh.account.application.core.dto.AddMultipleBasicMovementDTO;
import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.application.core.ports.input.AddMovementsUploadedFileInputPort;
import com.jbh.account.application.core.ports.output.AccountRepository;
import com.jbh.account.application.core.ports.output.monthlybalance.AccountMonthlyBalanceQueryRepo;
import com.jbh.account.application.core.ports.output.monthlybalance.AccountMonthlyBalanceWriterRepository;
import com.jbh.account.application.core.services.MonthlyBalanceSyncForUploadedMovements;
import com.jbh.account.application.core.services.account.AccountService;
import com.jbh.account.application.core.services.monthlybalance.MonthlyBalanceServiceImpl;
import com.jbh.account.application.core.usecases.AddMovementsUploadedFileUseCase;
import com.jbh.account.application.core.usecases.CreateAccountUseCase;
import com.jbh.account.application.core.usecases.UseCaseBuilder;
import com.jbh.account.application.core.usecases.utils.TestDataFactory;
import com.jbh.account.application.core.usecases.utils.UnitOfWorkTest;
import com.jbh.account.application.core.vo.commands.AddMovementUploadedFileCommand;
import com.jbh.account.application.core.vo.commands.CreateBasicAccountCommand;
import com.jbh.account.application.movements.ports.output.AccountMovementRepository;
import com.jbh.account.domain.entity.AccountDomain;
import com.jbh.account.domain.entity.AccountMonthlyBalanceDomain;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TestMethodOrder(MethodOrderer.class)
public class UploadMultiMovementsExecutionMockTest {
  static AccountId accountId = AccountId.generate();
  static UUID userId = UUID.randomUUID();
  static AccountDomain accountDomain =
      AccountDomain.withBasicMovementForExisting(accountId, userId, JBH_ZERO, JBH_ZERO);
  private static CreateAccountUseCase createAccountUseCase;
  private static AccountDTO currentAccount;
  private static AccountService accountService;
  private static AccountRepository accountRepository;
  private final UnitOfWork unitOfWork = new UnitOfWorkTest();
  private final Logger log = LoggerFactory.getLogger(UploadMultiMovementsExecutionMockTest.class);
  MonthlyBalanceSyncForUploadedMovements monthlyBalanceSyncerService;
  @Mock private AccountMovementRepository accountMovementRepository;
  @Mock private AccountMonthlyBalanceQueryRepo accountMonthlyBalanceQueryRepo;
  @Mock private AccountMonthlyBalanceWriterRepository monthlyBalanceWriterRepoMock;
  private AddMovementsUploadedFileUseCase useCaseInstanceTest;
  private MonthlyBalanceServiceImpl monthlyBalanceService;

  @BeforeEach
  void setUp() {

    MockitoAnnotations.openMocks(this);

    accountRepository = UseCaseBuilder.getAccountRepository();
    accountService = UseCaseBuilder.buildAccountService();

    accountService.save(accountDomain);

    monthlyBalanceService =
        new MonthlyBalanceServiceImpl(
            accountMonthlyBalanceQueryRepo,
            monthlyBalanceWriterRepoMock,
            new AsyncTaskExecutorImpl(),
            accountService);
    monthlyBalanceSyncerService = new MonthlyBalanceSyncForUploadedMovements(monthlyBalanceService);

    useCaseInstanceTest =
        new AddMovementsUploadedFileInputPort(
            accountService, accountMovementRepository, unitOfWork, monthlyBalanceSyncerService);

    createAccountUseCase = UseCaseBuilder.buildCreateAccountUseCase();
  }

  @Order(1)
  @Test
  @DisplayName("Should validate command")
  void shouldValidateCommand() {
    // Given
    final LocalDate entryDate = LocalDate.now();
    final BigDecimal totalAmount = new BigDecimal("100.00");
    final BigDecimal balanceSnapshot = new BigDecimal("120.00");
    final AddMovementUploadedFileCommand commandWithoutDate =
        new AddMovementUploadedFileCommand(null, totalAmount, balanceSnapshot);

    assertThrows(IllegalArgumentException.class, commandWithoutDate::validate);

    final AddMovementUploadedFileCommand commandWithoutAmount =
        new AddMovementUploadedFileCommand(entryDate, null, null);
  }

  @Test
  @DisplayName("Should create movements for NU")
  @Order(3)
  void shouldCreateMovementsForNU()
      throws ExecutionException, InterruptedException, TimeoutException {

    createAccount("NU");

    final List<AddMovementUploadedFileCommand> allSimpleMovements =
        TestDataFactory.createAccountMovementTestData();

    // Then
    assertEquals(10, allSimpleMovements.size());

    // Verify first entry
    final AddMovementUploadedFileCommand firstEntry = allSimpleMovements.get(0);
    assertEquals(LocalDate.of(2024, 7, 30), firstEntry.entryDate());
    assertEquals(0, new BigDecimal("12591000.00").compareTo(firstEntry.totalAmount()));
    assertEquals(0, new BigDecimal("12689712.00").compareTo(firstEntry.balanceSnapshot()));

    // Verify last entry (account reaches zero)
    final AddMovementUploadedFileCommand lastEntry = allSimpleMovements.get(9);
    assertEquals(LocalDate.of(2025, 3, 31), lastEntry.entryDate());
    assertEquals(0, new BigDecimal("-3768488.00").compareTo(lastEntry.totalAmount()));
    assertEquals(0, JBH_ZERO.compareTo(lastEntry.balanceSnapshot()));

    // Verify some negative amounts
    final AddMovementUploadedFileCommand novemberEntry = allSimpleMovements.get(4); // 30/11/2024
    assertTrue(novemberEntry.totalAmount().compareTo(JBH_ZERO) < 0);
    assertEquals(0, new BigDecimal("-673605.00").compareTo(novemberEntry.totalAmount()));

    // When
    final List<MonthlyBalanceDTO> savedMonthlyBalances = new ArrayList<>();
    for (final AddMovementUploadedFileCommand command : allSimpleMovements) {
      final YearMonth yearMonth =
          YearMonth.of(command.entryDate().getYear(), command.entryDate().getMonthValue());
      final MonthlyBalanceDTO monthlyBalanceDTO =
          MonthlyBalanceDTO.withClosingBalance(accountId, yearMonth, command.balanceSnapshot())
              .movementBalance(command.totalAmount())
              .build();
      savedMonthlyBalances.add(monthlyBalanceDTO);
    }
    when(accountMonthlyBalanceQueryRepo.findAllByAccountIdUntilNow(accountId))
        .thenReturn(savedMonthlyBalances);

    // Then
    final AtomicReference<AddMultipleBasicMovementDTO> processedResponse = new AtomicReference<>();
    assertDoesNotThrow(
        () ->
            processedResponse.set(
                useCaseInstanceTest.uploadMovementsFromFile(
                    userId, accountId, allSimpleMovements)));

    verify(accountMovementRepository).save(anyList());

    final AccountDTO actualAccountResponse = processedResponse.get().account();
    final List<MonthlyBalanceDTO> actualBalancesWithoutAsyncOperation =
        processedResponse.get().monthlyBalances();

    final BigDecimal expectedProfitBalance = new BigDecimal("1786605.00");
    assertEquals(expectedProfitBalance.negate(), actualAccountResponse.movementBalance());
    assertEquals(new BigDecimal("0.00"), actualAccountResponse.currentBalance());
    assertEquals(expectedProfitBalance, actualAccountResponse.netProfitBalance());
    assertEquals(9, actualBalancesWithoutAsyncOperation.size());

    // MONTHLY BALANCES ASSERTS SHOULD NOT INCLUDE
    // MONTHLY PROFIT AND OPENING BALANCES
    // BECAUSE THEY ARE NOT SYNCED YET. THEY ARE SYNCED ASYNC

    // Assert per month
    int expectedYear = 2024;
    int expectedMonth = 7;
    int sortedIndex = 0;
    final var monthlyProfit20247 = numberOf("98712");
    MonthlyBalanceDTO actualMonthBalance = actualBalancesWithoutAsyncOperation.get(sortedIndex);
    final var closingBalance20247 = numberOf("12689712");
    MonthlyBalanceDTO expectedMonthBalance =
        MonthlyBalanceDTO.defaultBuilder()
            .accountId(accountId)
            .totalMovements(1)
            .period(YearMonth.of(expectedYear, expectedMonth))
            .month(expectedMonth)
            .year(expectedYear)
            .closingBalance(closingBalance20247)
            .movementBalance(numberOf("12591000"))
            .totalDebits(numberOf("12591000"))
            .build();
    assertMonthlyBalance(
        expectedMonthBalance, actualMonthBalance, IGNORE_MONTHLY_PROFIT, IGNORE_OPENING_BALANCE);

    // Assert per month
    expectedYear = 2024;
    expectedMonth = 8;
    sortedIndex = 1;
    final var monthlyProfit20248 = numberOf("318629");
    actualMonthBalance = actualBalancesWithoutAsyncOperation.get(sortedIndex);
    expectedMonthBalance =
        MonthlyBalanceDTO.defaultBuilder()
            .accountId(accountId)
            .totalMovements(1)
            .period(YearMonth.of(expectedYear, expectedMonth))
            .month(expectedMonth)
            .year(expectedYear)
            .openingBalance(closingBalance20247)
            .closingBalance(numberOf("35693653"))
            .totalDebits(numberOf("22685312"))
            .movementBalance(numberOf("22685312"))
            .build();
    assertMonthlyBalance(
        expectedMonthBalance, actualMonthBalance, IGNORE_MONTHLY_PROFIT, IGNORE_OPENING_BALANCE);

    // Assert per month
    expectedYear = 2024;
    expectedMonth = 9;
    final var monthlyProfit20249 = numberOf("323804");
    sortedIndex++;
    actualMonthBalance = actualBalancesWithoutAsyncOperation.get(sortedIndex);
    expectedMonthBalance =
        MonthlyBalanceDTO.defaultBuilder()
            .accountId(accountId)
            .totalMovements(0)
            .period(YearMonth.of(expectedYear, expectedMonth))
            .month(expectedMonth)
            .year(expectedYear)
            .closingBalance(numberOf("36017457"))
            .totalDebits(numberOf("0"))
            .monthlyNetProfit(monthlyProfit20249)
            .build();
    assertMonthlyBalance(
        expectedMonthBalance, actualMonthBalance, IGNORE_MONTHLY_PROFIT, IGNORE_OPENING_BALANCE);

    // Assert per month
    expectedYear = 2024;
    expectedMonth = 10;
    final var monthlyProfit202410 = numberOf("340119");
    sortedIndex++;
    actualMonthBalance = actualBalancesWithoutAsyncOperation.get(sortedIndex);
    expectedMonthBalance =
        MonthlyBalanceDTO.defaultBuilder()
            .accountId(accountId)
            .totalMovements(0)
            .period(YearMonth.of(expectedYear, expectedMonth))
            .month(expectedMonth)
            .year(expectedYear)
            .closingBalance(numberOf("36357576"))
            .totalDebits(numberOf("0"))
            .monthlyNetProfit(monthlyProfit202410)
            .build();
    assertMonthlyBalance(
        expectedMonthBalance, actualMonthBalance, IGNORE_MONTHLY_PROFIT, IGNORE_OPENING_BALANCE);

    // Assert per month
    expectedYear = 2024;
    expectedMonth = 11;
    final var monthlyProfit202411 = numberOf("298338");
    sortedIndex++;
    actualMonthBalance = actualBalancesWithoutAsyncOperation.get(sortedIndex);
    expectedMonthBalance =
        MonthlyBalanceDTO.defaultBuilder()
            .accountId(accountId)
            .totalMovements(1)
            .period(YearMonth.of(expectedYear, expectedMonth))
            .month(expectedMonth)
            .year(expectedYear)
            .closingBalance(numberOf("35982309"))
            .totalDebits(numberOf("0"))
            .monthlyNetProfit(monthlyProfit202411)
            .totalCredits(numberOf("673605"))
            .movementBalance(numberOf("-673605"))
            .build();
    assertMonthlyBalance(
        expectedMonthBalance, actualMonthBalance, IGNORE_MONTHLY_PROFIT, IGNORE_OPENING_BALANCE);

    // Assert per month
    expectedYear = 2024;
    expectedMonth = 12;
    final var monthlyProfit202412 = numberOf("289672");
    sortedIndex++;
    actualMonthBalance = actualBalancesWithoutAsyncOperation.get(sortedIndex);
    expectedMonthBalance =
        MonthlyBalanceDTO.defaultBuilder()
            .accountId(accountId)
            .totalMovements(1)
            .period(YearMonth.of(expectedYear, expectedMonth))
            .month(expectedMonth)
            .year(expectedYear)
            .closingBalance(numberOf("35000981"))
            .totalDebits(numberOf("0"))
            .totalCredits(numberOf("1271000"))
            .movementBalance(numberOf("-1271000"))
            .monthlyNetProfit(monthlyProfit202412)
            .build();
    assertMonthlyBalance(
        expectedMonthBalance, actualMonthBalance, IGNORE_MONTHLY_PROFIT, IGNORE_OPENING_BALANCE);

    // Assert per month
    expectedYear = 2025;
    expectedMonth = 1;
    final var monthlyProfit20251 = numberOf("227779");
    sortedIndex++;
    actualMonthBalance = actualBalancesWithoutAsyncOperation.get(sortedIndex);
    expectedMonthBalance =
        MonthlyBalanceDTO.defaultBuilder()
            .accountId(accountId)
            .totalMovements(1)
            .period(YearMonth.of(expectedYear, expectedMonth))
            .month(expectedMonth)
            .year(expectedYear)
            .closingBalance(numberOf("25638626"))
            .totalDebits(numberOf("0"))
            .monthlyNetProfit(monthlyProfit20251)
            .totalCredits(numberOf("9590134"))
            .movementBalance(numberOf("-9590134"))
            .build();
    assertMonthlyBalance(
        expectedMonthBalance, actualMonthBalance, IGNORE_MONTHLY_PROFIT, IGNORE_OPENING_BALANCE);

    // Assert per month
    expectedYear = 2025;
    expectedMonth = 2;
    final var monthlyProfit20252 = numberOf("-110448");
    sortedIndex++;
    actualMonthBalance = actualBalancesWithoutAsyncOperation.get(sortedIndex);
    expectedMonthBalance =
        MonthlyBalanceDTO.defaultBuilder()
            .accountId(accountId)
            .totalMovements(2)
            .period(YearMonth.of(expectedYear, expectedMonth))
            .month(expectedMonth)
            .year(expectedYear)
            .closingBalance(numberOf("3768488"))
            .totalDebits(numberOf("0"))
            .monthlyNetProfit(monthlyProfit20252)
            .totalCredits(numberOf("21759690"))
            .movementBalance(numberOf("-21759690"))
            .build();
    assertMonthlyBalance(
        expectedMonthBalance, actualMonthBalance, IGNORE_MONTHLY_PROFIT, IGNORE_OPENING_BALANCE);

    // Assert per month
    expectedYear = 2025;
    expectedMonth = 3;
    sortedIndex++;
    actualMonthBalance = actualBalancesWithoutAsyncOperation.get(sortedIndex);
    expectedMonthBalance =
        MonthlyBalanceDTO.defaultBuilder()
            .accountId(accountId)
            .totalMovements(1)
            .period(YearMonth.of(expectedYear, expectedMonth))
            .month(expectedMonth)
            .year(expectedYear)
            .closingBalance(numberOf("0"))
            .totalDebits(numberOf("0"))
            .totalCredits(numberOf("3768488"))
            .movementBalance(numberOf("-3768488"))
            .build();
    assertMonthlyBalance(
        expectedMonthBalance, actualMonthBalance, IGNORE_MONTHLY_PROFIT, IGNORE_OPENING_BALANCE);

    final YearMonth initPeriod =
        YearMonth.of(firstEntry.entryDate().getYear(), firstEntry.entryDate().getMonthValue());
    when(accountMonthlyBalanceQueryRepo.findNextBalancesFromPeriodInclusive(accountId, initPeriod))
        .thenReturn(actualBalancesWithoutAsyncOperation);
    final CompletableFuture<List<MonthlyBalanceDTO>> futureResponse =
        monthlyBalanceSyncerService.persistBalancesAsync(
            accountId, actualBalancesWithoutAsyncOperation);

    final List<MonthlyBalanceDTO> actualProfitBalances = futureResponse.get(7, TimeUnit.SECONDS);
    assertEquals(10, actualProfitBalances.size());

    int index = -1;
    AccountMonthlyBalanceDomain actualResponse = null;
    final YearMonth expectedPeriod = YearMonth.of(2024, 7);

    // Row 1
    actualResponse = toDomain(actualProfitBalances.get(++index));
    assertEquals(closingBalance20247, actualResponse.getClosingBalance());

    assertEquals(monthlyProfit20247, actualResponse.getMonthlyNetProfit());
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());

    // Row 2
    actualResponse = toDomain(actualProfitBalances.get(++index));
    assertEquals(numberOf("35693653"), actualResponse.getClosingBalance());
    assertEquals(monthlyProfit20248, actualResponse.getMonthlyNetProfit());
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());

    // Row 3
    actualResponse = toDomain(actualProfitBalances.get(++index));
    assertEquals(numberOf("36017457"), actualResponse.getClosingBalance());
    assertEquals(monthlyProfit20249, actualResponse.getMonthlyNetProfit());
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());

    // Row 4
    actualResponse = toDomain(actualProfitBalances.get(++index));
    assertEquals(numberOf("36357576"), actualResponse.getClosingBalance());
    assertEquals(monthlyProfit202410, actualResponse.getMonthlyNetProfit());
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());

    // Row 5
    actualResponse = toDomain(actualProfitBalances.get(++index));
    assertEquals(numberOf("35982309"), actualResponse.getClosingBalance());
    assertEquals(monthlyProfit202411, actualResponse.getMonthlyNetProfit());
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());

    // Row 6
    actualResponse = toDomain(actualProfitBalances.get(++index));
    assertEquals(numberOf("35000981"), actualResponse.getClosingBalance());
    assertEquals(monthlyProfit202412, actualResponse.getMonthlyNetProfit());
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());

    // Row 7
    actualResponse = toDomain(actualProfitBalances.get(++index));
    assertEquals(numberOf("25638626"), actualResponse.getClosingBalance());
    assertEquals(monthlyProfit20251, actualResponse.getMonthlyNetProfit());
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());

    // Row 8
    actualResponse = toDomain(actualProfitBalances.get(++index));
    assertEquals(numberOf("3768488"), actualResponse.getClosingBalance());
    assertEquals(monthlyProfit20252, actualResponse.getMonthlyNetProfit());
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());

    // Row 9
    actualResponse = toDomain(actualProfitBalances.get(++index));
    assertEquals(numberOf("0"), actualResponse.getClosingBalance());
    assertEquals(numberOf("0"), actualResponse.getMonthlyNetProfit());
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());

    // Assert account net growth rate
    final AccountDTO accountDTO = accountService.findAccountOrThrow(accountId);
    assertEquals(numberOf("10.28"), accountDTO.netGrowthRate());
    assertNotNull(accountDTO.metadata());
    assertEquals(true, accountDTO.metadata().get("FULLY_WITHDRAWN"));
  }

  void createAccount(final String name) {
    currentAccount =
        createAccountUseCase.execute(createBasicAccountCommand(userId, name, AccountType.SAVINGS));
    accountId = currentAccount.id();
    assertNotNull(accountId);
    log.info("Account created with id {} for user {}", accountId, userId);
  }

  private BigDecimal numberOf(final String val) {
    return withJBHDecimals(new BigDecimal(val));
  }

  @Test
  @DisplayName("Should create movements for PIKMI")
  void shouldCreatedMovementsForPIKMI()
      throws ExecutionException, InterruptedException, TimeoutException {

    createAccount("PIKMI");

    // Given
    final List<AddMovementUploadedFileCommand> testData = TestDataFactory.movementsForPIKMI();

    // Then
    assertEquals(46, testData.size());

    final AtomicReference<AddMultipleBasicMovementDTO> processedResponse = new AtomicReference<>();
    assertDoesNotThrow(
        () ->
            processedResponse.set(
                useCaseInstanceTest.uploadMovementsFromFile(userId, accountId, testData)));

    final AccountDTO actualAccount = processedResponse.get().account();

    assertEquals(numberOf("56386448"), actualAccount.movementBalance());
    assertEquals(numberOf("70908065"), actualAccount.currentBalance());
    assertEquals(numberOf("14521617"), actualAccount.netProfitBalance());
    final List<MonthlyBalanceDTO> savedMonthlyBalances = processedResponse.get().monthlyBalances();

    final YearMonth expectedPeriod = YearMonth.of(2023, 7);
    log.info("Account Tested. Preparing to test the monthly Balances ASYNC...{} ", expectedPeriod);
    when(accountMonthlyBalanceQueryRepo.findNextBalancesFromPeriodInclusive(
            accountId, expectedPeriod))
        .thenReturn(savedMonthlyBalances);

    final CompletableFuture<List<MonthlyBalanceDTO>> futureResponse =
        monthlyBalanceSyncerService.persistBalancesAsync(accountId, savedMonthlyBalances);
    final List<MonthlyBalanceDTO> actualMonthlyBalances = futureResponse.get();

    int index = -1;
    AccountMonthlyBalanceDomain actualResponse = null;

    // Row Julio/23
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("15000000"), actualResponse.getMovementBalance());
    assertEquals(numberOf("15074686"), actualResponse.getClosingBalance());
    assertEquals(numberOf("74686"), actualResponse.getMonthlyNetProfit());

    // Row Agosto/23
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("29657000"), actualResponse.getMovementBalance());
    assertEquals(numberOf("45525753"), actualResponse.getClosingBalance());
    assertEquals(numberOf("794067"), actualResponse.getMonthlyNetProfit());

    // Row Septiembre/23
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("9117414"), actualResponse.getMovementBalance());
    assertEquals(numberOf("54787054"), actualResponse.getClosingBalance());
    assertEquals(numberOf("143887"), actualResponse.getMonthlyNetProfit());

    // Row Octubre/23
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("-5550618"), actualResponse.getMovementBalance());
    assertEquals(numberOf("50069218"), actualResponse.getClosingBalance());
    assertEquals(numberOf("832782"), actualResponse.getMonthlyNetProfit());

    // Row Noviembre/23
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("-11728845"), actualResponse.getMovementBalance());
    assertEquals(numberOf("40726167"), actualResponse.getClosingBalance());
    assertEquals(numberOf("2385794"), actualResponse.getMonthlyNetProfit());

    // Row Diciembre/23
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("-17246355"), actualResponse.getMovementBalance());
    assertEquals(numberOf("23720010"), actualResponse.getClosingBalance());
    assertEquals(numberOf("240198"), actualResponse.getMonthlyNetProfit());

    // Row Enero/24
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("-12346955"), actualResponse.getMovementBalance());
    assertEquals(numberOf("11480093"), actualResponse.getClosingBalance());
    assertEquals(numberOf("107038"), actualResponse.getMonthlyNetProfit());

    // Row Febrero/24
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("-7346955"), actualResponse.getMovementBalance());
    assertEquals(numberOf("4105556"), actualResponse.getClosingBalance());
    assertEquals(numberOf("-27582"), actualResponse.getMonthlyNetProfit());

    // Row Marzo/24
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("653645"), actualResponse.getMovementBalance());
    assertEquals(numberOf("4839561"), actualResponse.getClosingBalance());
    assertEquals(numberOf("80360"), actualResponse.getMonthlyNetProfit());

    // Row Abril/24
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("653645"), actualResponse.getMovementBalance());
    assertEquals(numberOf("5542958"), actualResponse.getClosingBalance());
    assertEquals(numberOf("49752"), actualResponse.getMonthlyNetProfit());

    // Row Mayo/24
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("2401286"), actualResponse.getMovementBalance());
    assertEquals(numberOf("8016341"), actualResponse.getClosingBalance());
    assertEquals(numberOf("72097"), actualResponse.getMonthlyNetProfit());

    // Row Junio/24
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("520531"), actualResponse.getMovementBalance());
    assertEquals(numberOf("8613715"), actualResponse.getClosingBalance());
    assertEquals(numberOf("76843"), actualResponse.getMonthlyNetProfit());

    // Row Julio/24
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("520531"), actualResponse.getMovementBalance());
    assertEquals(numberOf("9136328"), actualResponse.getClosingBalance());
    assertEquals(numberOf("2082"), actualResponse.getMonthlyNetProfit());

    // Row Agosto/24
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("60520531"), actualResponse.getMovementBalance());
    assertEquals(numberOf("72058459"), actualResponse.getClosingBalance());
    assertEquals(numberOf("2401600"), actualResponse.getMonthlyNetProfit());

    // Row Septiembre/24
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("520531"), actualResponse.getMovementBalance());
    assertEquals(numberOf("73257353"), actualResponse.getClosingBalance());
    assertEquals(numberOf("678363"), actualResponse.getMonthlyNetProfit());

    // Row Octubre/24
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("520531"), actualResponse.getMovementBalance());
    assertEquals(numberOf("74490431"), actualResponse.getClosingBalance());
    assertEquals(numberOf("712547"), actualResponse.getMonthlyNetProfit());

    // Row Noviembre/24
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("520531"), actualResponse.getMovementBalance());
    assertEquals(numberOf("75711997"), actualResponse.getClosingBalance());
    assertEquals(numberOf("701035"), actualResponse.getMonthlyNetProfit());

    // Row Diciembre/24
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("76398664"), actualResponse.getClosingBalance());
    assertEquals(numberOf("686667"), actualResponse.getMonthlyNetProfit());

    // Row Enero/25
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("76976067"), actualResponse.getClosingBalance());
    assertEquals(numberOf("577403"), actualResponse.getMonthlyNetProfit());

    // Row Febrero/25
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("77501534"), actualResponse.getClosingBalance());
    assertEquals(numberOf("525467"), actualResponse.getMonthlyNetProfit());

    // Row Marzo/25
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("78053700"), actualResponse.getClosingBalance());
    assertEquals(numberOf("552166"), actualResponse.getMonthlyNetProfit());

    // Row Abril/25
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("-10000000"), actualResponse.getMovementBalance());
    assertEquals(numberOf("68053700"), actualResponse.getClosingBalance());
    assertEquals(numberOf("0"), actualResponse.getMonthlyNetProfit());

    // Row May/25
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("68053700"), actualResponse.getClosingBalance());
    assertEquals(numberOf("0"), actualResponse.getMonthlyNetProfit());

    // Row June/25
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("68053700"), actualResponse.getClosingBalance());
    assertEquals(numberOf("0"), actualResponse.getMonthlyNetProfit());

    // Row July/25
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("68053700"), actualResponse.getClosingBalance());
    assertEquals(numberOf("0"), actualResponse.getMonthlyNetProfit());

    // Row Agosto/25
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("70908065"), actualResponse.getClosingBalance());
    assertEquals(numberOf("2854365"), actualResponse.getMonthlyNetProfit());

    // Row 25/09/2025 - Last one affected by movement. Only Opening Balance.
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("70908065"), actualResponse.getOpeningBalance());
    assertEquals(numberOf("0"), actualResponse.getMonthlyNetProfit());
    assertEquals(numberOf("0"), actualResponse.getClosingBalance());

    final AtomicInteger lastIdx = new AtomicInteger(++index);
    assertThrows(IndexOutOfBoundsException.class, () -> actualMonthlyBalances.get(lastIdx.get()));
  }

  @Test
  @DisplayName("Should create movements for PIBI")
  void shouldCreatedMovementsForPIBI()
      throws ExecutionException, InterruptedException, TimeoutException {

    createAccount("PIBI");
    // Given
    final List<AddMovementUploadedFileCommand> testData = TestDataFactory.movementsForPIBI();

    // Then
    assertEquals(25, testData.size());

    // Verify first entry (October 2023)
    final YearMonth firstEntryYM = YearMonth.of(2023, 10);
    final AddMovementUploadedFileCommand firstEntry = testData.get(0);
    assertEquals(LocalDate.of(2023, 10, 31), firstEntry.entryDate());
    assertEquals(0, new BigDecimal("13010000.00").compareTo(firstEntry.totalAmount()));
    assertEquals(0, new BigDecimal("13062118.00").compareTo(firstEntry.balanceSnapshot()));

    // Verify large deposit in May 2024
    final AddMovementUploadedFileCommand mayDeposit = testData.get(8); // 03/05/2024
    assertEquals(LocalDate.of(2024, 5, 3), mayDeposit.entryDate());
    assertEquals(0, new BigDecimal("24100000.00").compareTo(mayDeposit.totalAmount()));
    assertEquals(0, new BigDecimal("65394365.00").compareTo(mayDeposit.balanceSnapshot()));

    // Verify large withdrawal in July 2024
    final AddMovementUploadedFileCommand julyWithdrawal = testData.get(13); // 31/07/2024
    assertEquals(LocalDate.of(2024, 7, 31), julyWithdrawal.entryDate());
    assertEquals(0, new BigDecimal("-20000000.00").compareTo(julyWithdrawal.totalAmount()));
    assertEquals(0, new BigDecimal("32809397.00").compareTo(julyWithdrawal.balanceSnapshot()));

    // Verify final entry (August 2025)
    final AddMovementUploadedFileCommand lastEntry = testData.get(24);
    assertEquals(LocalDate.of(2025, 8, 30), lastEntry.entryDate());
    assertEquals(0, JBH_ZERO.compareTo(lastEntry.totalAmount()));
    assertEquals(0, new BigDecimal("37074883.00").compareTo(lastEntry.balanceSnapshot()));

    /*
    when(accountRepository.findByUserAndAccountId(userId, accountId))
        .thenReturn(Optional.of(AccountMapper.toDTO(accountDomain)));

     */

    final AtomicReference<AddMultipleBasicMovementDTO> processedResponse = new AtomicReference<>();
    assertDoesNotThrow(
        () ->
            processedResponse.set(
                useCaseInstanceTest.uploadMovementsFromFile(userId, accountId, testData)));

    final AccountDTO actualAccount = processedResponse.get().account();

    assertEquals(numberOf("29710000"), actualAccount.movementBalance());
    assertEquals(numberOf("37074883"), actualAccount.currentBalance());
    assertEquals(numberOf("7364883"), actualAccount.netProfitBalance());
    final List<MonthlyBalanceDTO> savedMonthlyBalances = processedResponse.get().monthlyBalances();

    // THEN
    final YearMonth monthlyInitiPeriod = YearMonth.of(2023, 10);
    log.info("Account Tested. Preparing to test the monthly Balances ...{} ", monthlyInitiPeriod);
    when(accountMonthlyBalanceQueryRepo.findNextBalancesFromPeriodInclusive(
            accountId, monthlyInitiPeriod))
        .thenReturn(savedMonthlyBalances);
    final CompletableFuture<List<MonthlyBalanceDTO>> futureResponse =
        monthlyBalanceSyncerService.persistBalancesAsync(accountId, savedMonthlyBalances);
    final List<MonthlyBalanceDTO> actualMonthlyBalances = futureResponse.get();

    // Response
    int index = -1;
    AccountMonthlyBalanceDomain actualResponse = null;

    // Row Octubre/23
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(monthlyInitiPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("13010000"), actualResponse.getMovementBalance());
    assertEquals(numberOf("13062118"), actualResponse.getClosingBalance());
    assertEquals(numberOf("52118"), actualResponse.getMonthlyNetProfit());

    // Row Noviembre/23
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(monthlyInitiPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("13000000"), actualResponse.getMovementBalance());
    assertEquals(numberOf("26309995"), actualResponse.getClosingBalance());
    assertEquals(numberOf("247877"), actualResponse.getMonthlyNetProfit());

    // Row Diciembre/23
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(monthlyInitiPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("13000000"), actualResponse.getMovementBalance());
    assertEquals(numberOf("39645174"), actualResponse.getClosingBalance());
    assertEquals(numberOf("335179"), actualResponse.getMonthlyNetProfit());

    // Row Enero/24
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(monthlyInitiPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("40001713"), actualResponse.getClosingBalance());
    assertEquals(numberOf("356539"), actualResponse.getMonthlyNetProfit());

    // Row Febrero/24
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(monthlyInitiPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("40001713"), actualResponse.getClosingBalance());
    assertEquals(numberOf("0"), actualResponse.getMonthlyNetProfit());

    // Row Marzo/24
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(monthlyInitiPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("40701019"), actualResponse.getClosingBalance());
    assertEquals(numberOf("699306"), actualResponse.getMonthlyNetProfit());

    // Row Abril/24
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(monthlyInitiPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("41197965"), actualResponse.getClosingBalance());
    assertEquals(numberOf("496946"), actualResponse.getMonthlyNetProfit());

    // Row Mayo/24
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(monthlyInitiPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("-5900000"), actualResponse.getMovementBalance());
    assertEquals(numberOf("35762219"), actualResponse.getClosingBalance());
    assertEquals(numberOf("464254"), actualResponse.getMonthlyNetProfit());

    // Row Junio/24
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(monthlyInitiPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("16600000"), actualResponse.getMovementBalance());
    assertEquals(numberOf("52889397"), actualResponse.getClosingBalance());
    assertEquals(numberOf("527178"), actualResponse.getMonthlyNetProfit());

    // Row Julio/24
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(monthlyInitiPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("-20000000"), actualResponse.getMovementBalance());
    assertEquals(numberOf("32809397"), actualResponse.getClosingBalance());
    assertEquals(numberOf("-80000"), actualResponse.getMonthlyNetProfit());

    // Row Agosto/24
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(monthlyInitiPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("33560000"), actualResponse.getClosingBalance());
    assertEquals(numberOf("750603"), actualResponse.getMonthlyNetProfit());

    // Row Septiembre/24
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(monthlyInitiPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("33876037"), actualResponse.getClosingBalance());
    assertEquals(numberOf("316037"), actualResponse.getMonthlyNetProfit());

    // Row Octubre/24
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(monthlyInitiPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("34204708"), actualResponse.getClosingBalance());
    assertEquals(numberOf("328671"), actualResponse.getMonthlyNetProfit());

    // Row Noviembre/24
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(monthlyInitiPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("34525863"), actualResponse.getClosingBalance());
    assertEquals(numberOf("321155"), actualResponse.getMonthlyNetProfit());

    // Row Diciembre/24
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(monthlyInitiPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("34838994"), actualResponse.getClosingBalance());
    assertEquals(numberOf("313131"), actualResponse.getMonthlyNetProfit());

    // Row Enero/25
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(monthlyInitiPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("35102299"), actualResponse.getClosingBalance());
    assertEquals(numberOf("263305"), actualResponse.getMonthlyNetProfit());

    // Row Febrero/25
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(monthlyInitiPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("35341920"), actualResponse.getClosingBalance());
    assertEquals(numberOf("239621"), actualResponse.getMonthlyNetProfit());

    // Row Marzo/25
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(monthlyInitiPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("35593716"), actualResponse.getClosingBalance());
    assertEquals(numberOf("251796"), actualResponse.getMonthlyNetProfit());

    // Row Abril/25
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(monthlyInitiPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("35829015"), actualResponse.getClosingBalance());
    assertEquals(numberOf("235299"), actualResponse.getMonthlyNetProfit());

    // Row Mayo/25
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(monthlyInitiPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("36102719"), actualResponse.getClosingBalance());
    assertEquals(numberOf("273704"), actualResponse.getMonthlyNetProfit());

    // Row Jun/25
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(monthlyInitiPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("36102719"), actualResponse.getClosingBalance());
    assertEquals(numberOf("0"), actualResponse.getMonthlyNetProfit());

    // Row Jul/25
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(monthlyInitiPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("36102719"), actualResponse.getClosingBalance());
    assertEquals(numberOf("0"), actualResponse.getMonthlyNetProfit());

    // Row 25/08/2025
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(monthlyInitiPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("37074883"), actualResponse.getClosingBalance());
    assertEquals(numberOf("972164"), actualResponse.getMonthlyNetProfit());

    // Row 25/09/2025 - Last one affected by movement. Only Opening Balance.
    actualResponse = toDomain(actualMonthlyBalances.get(++index));
    assertEquals(monthlyInitiPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("37074883"), actualResponse.getOpeningBalance());
    assertEquals(numberOf("0"), actualResponse.getMonthlyNetProfit());
    assertEquals(numberOf("0"), actualResponse.getClosingBalance());

    final AtomicInteger lastIdx = new AtomicInteger(++index);
    assertThrows(IndexOutOfBoundsException.class, () -> actualMonthlyBalances.get(lastIdx.get()));
  }
}
