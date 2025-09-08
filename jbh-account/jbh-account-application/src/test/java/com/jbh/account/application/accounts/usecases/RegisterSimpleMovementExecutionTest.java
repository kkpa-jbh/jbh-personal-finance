package com.jbh.account.application.accounts.usecases;

import static com.jbh.accounts_mgmt.utils.MoneyUtils.JBH_ZERO;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jbh.account.application.accounts.dto.AddBasicMovementDTO;
import com.jbh.account.application.accounts.dto.AddMultipleBasicMovementDTO;
import com.jbh.account.application.accounts.ports.input.RegisterSimpleMovementInputPort;
import com.jbh.account.application.accounts.ports.output.AccountMonthlyBalanceRepository;
import com.jbh.account.application.accounts.ports.output.AccountRepository;
import com.jbh.account.application.accounts.services.MonthlyBalanceSyncerAppService;
import com.jbh.account.application.accounts.usecases.utils.TestDataFactory;
import com.jbh.account.application.accounts.usecases.utils.UnitOfWorkTest;
import com.jbh.account.application.accounts.vo.AddBasicMovementRequest;
import com.jbh.account.application.acid.UnitOfWork;
import com.jbh.account.application.movements.ports.output.AccountMovementRepository;
import com.jbh.accounts_mgmt.accounts.AccountDomain;
import com.jbh.accounts_mgmt.accounts.AccountId;
import com.jbh.accounts_mgmt.accounts.AccountMonthlyBalanceDomain;
import com.jbh.accounts_mgmt.movements.AccountMovementDomain;
import com.jbh.accounts_mgmt.movements.MovementType;
import com.jbh.accounts_mgmt.utils.MoneyUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegisterSimpleMovementExecutionTest {

  private final UnitOfWork unitOfWork = new UnitOfWorkTest();
  MonthlyBalanceSyncerAppService monthlyBalanceSyncerService;
  LocalDate movementDate = LocalDate.now();
  @Mock
  private AccountRepository accountRepository;
  @Mock
  private AccountMovementRepository accountMovementRepository;
  @Mock
  private AccountMonthlyBalanceRepository accountMonthlyBalanceRepository;
  private RegisterSimpleMovementInputPort useCaseInstanceTest;

  private Logger log = LoggerFactory.getLogger(RegisterSimpleMovementExecutionTest.class);

  @BeforeEach
  void setUp() {

    MockitoAnnotations.openMocks(this);

    monthlyBalanceSyncerService = new MonthlyBalanceSyncerAppService(
        accountMonthlyBalanceRepository);
    useCaseInstanceTest = new RegisterSimpleMovementInputPort(accountRepository, accountMovementRepository,
        unitOfWork,
        monthlyBalanceSyncerService);
  }

  @Test
  void shouldAddMovement_WhenNotSnapshotProvided() throws ExecutionException, InterruptedException {
    // Given
    UUID userId = UUID.randomUUID();
    AccountId accountId = AccountId.generate();

    BigDecimal amount = new BigDecimal("100.00");

    AddBasicMovementRequest request = new AddBasicMovementRequest(movementDate, amount);
    AccountDomain accountDomain = AccountDomain.withId(accountId);

    when(accountRepository.findByAccountId(userId, accountId))
        .thenReturn(Optional.of(accountDomain));

    // When & Then
    AtomicReference<AddBasicMovementDTO> mvmtResponse = new AtomicReference<>();
    assertDoesNotThrow(
        () -> mvmtResponse.set(useCaseInstanceTest.addBasicMovements(userId, accountId, request)));

    verify(accountRepository).findByAccountId(userId, accountId);
    verify(accountMovementRepository).save((AccountMovementDomain) any());
    verify(accountRepository).save(accountDomain);

    assertEquals(accountDomain.getMovementBalance(), amount);
    AccountMonthlyBalanceDomain actualMonthlyBalances = mvmtResponse.get().monthlyBalance();
    assertEquals(amount, actualMonthlyBalances.getTotalDebits());
    assertEquals(amount, actualMonthlyBalances.getClosingBalance());
    assertEquals(JBH_ZERO, actualMonthlyBalances.getOpeningBalance());
    assertEquals(JBH_ZERO, actualMonthlyBalances.getMonthlyProfit());

    when(
        accountMonthlyBalanceRepository.findNextBalancesFromPeriodInclusive(accountId,
            YearMonth.of(movementDate.getYear(), movementDate.getMonthValue()))
    ).thenReturn(Collections.singletonList(actualMonthlyBalances));

    List<AccountMonthlyBalanceDomain> futureResponse =
        monthlyBalanceSyncerService.saveMonthlyBalancesASYNC(accountId,
            Collections.singletonList(actualMonthlyBalances)).get();

    assertEquals(2, futureResponse.size());

    assertEquals(movementDate.plusMonths(1).getMonthValue(), futureResponse.get(1).getMonth());
    assertEquals(amount, futureResponse.get(1).getOpeningBalance());
    assertEquals(JBH_ZERO, futureResponse.get(0).getMonthlyProfit());
    assertEquals(JBH_ZERO, futureResponse.get(1).getMonthlyProfit());

  }

  @Test
  void shouldAddMovement_WhenOnlySnapshotProvided() {
    // Given
    UUID userId = UUID.randomUUID();
    AccountId accountId = AccountId.generate();
    LocalDate movementDate = LocalDate.now();
    BigDecimal balanceSnashot = new BigDecimal("200.00");
    BigDecimal existingAccountPpalBalance = new BigDecimal("100.00");
    AddBasicMovementRequest request = new AddBasicMovementRequest(movementDate, null, balanceSnashot);

    AccountDomain accountDomain = AccountDomain.withId(accountId);
    accountDomain.setMovementBalance(existingAccountPpalBalance);
    accountDomain.setCurrentBalance(new BigDecimal("190.00"));

    when(accountRepository.findByAccountId(userId, accountId))
        .thenReturn(Optional.of(accountDomain));

    // When & Then
    AtomicReference<AddBasicMovementDTO> mvmtResponse = new AtomicReference<>();
    assertDoesNotThrow(
        () -> mvmtResponse.set(useCaseInstanceTest.addBasicMovements(userId, accountId, request)));

    verify(accountRepository).findByAccountId(userId, accountId);
    verify(accountMovementRepository).save((AccountMovementDomain) any());
    verify(accountRepository).save(accountDomain);

    assertEquals(balanceSnashot, mvmtResponse.get().account().getCurrentBalance());
    assertEquals(existingAccountPpalBalance, mvmtResponse.get().account().getMovementBalance());
    assertEquals(JBH_ZERO, mvmtResponse.get().monthlyBalance().getTotalDebits());
    assertEquals(0, mvmtResponse.get().monthlyBalance().getTotalMovements());
    assertEquals(JBH_ZERO, mvmtResponse.get().monthlyBalance().getTotalCredits());
    assertEquals(balanceSnashot, mvmtResponse.get().monthlyBalance().getClosingBalance());
    assertEquals(JBH_ZERO, mvmtResponse.get().monthlyBalance().getMonthlyProfit());
    assertEquals(JBH_ZERO, mvmtResponse.get().monthlyBalance().getOpeningBalance());


  }

  @Test
  void shouldAddMovement_WhenAmountAndSnapshotProvided() {
    // Given
    UUID userId = UUID.randomUUID();
    AccountId accountId = AccountId.generate();
    LocalDate movementDate = LocalDate.now();
    BigDecimal amount = new BigDecimal("22685312.00");
    BigDecimal balanceSnapshot = new BigDecimal("35693653.00");

    AddBasicMovementRequest request = new AddBasicMovementRequest(movementDate, amount, balanceSnapshot);
    AccountDomain accountDomain = AccountDomain.withId(accountId);
    BigDecimal existingMovBalance = new BigDecimal("12591000.00");
    accountDomain.setMovementBalance(existingMovBalance);
    accountDomain.setCurrentBalance(new BigDecimal("12689712.00"));

    when(accountRepository.findByAccountId(userId, accountId))
        .thenReturn(Optional.of(accountDomain));

    // When & Then
    AtomicReference<AddBasicMovementDTO> mvmtResponse = new AtomicReference<>();
    assertDoesNotThrow(() -> mvmtResponse.set(
        useCaseInstanceTest.addBasicMovements(userId, accountId, request)));

    verify(accountRepository).findByAccountId(userId, accountId);
    verify(accountMovementRepository).save((AccountMovementDomain) any());
    verify(accountRepository).save(accountDomain);

    AccountDomain accountResponse = mvmtResponse.get().account();
    assertEquals(amount.add(existingMovBalance), accountResponse.getMovementBalance());
    assertEquals(balanceSnapshot, accountResponse.getCurrentBalance());

    AccountMonthlyBalanceDomain monthlyBalanceResponse = mvmtResponse.get().monthlyBalance();
    YearMonth expectedYearMonth = YearMonth.of(movementDate.getYear(), movementDate.getMonthValue());
    assertEquals(expectedYearMonth,
        YearMonth.of(monthlyBalanceResponse.getYear(), monthlyBalanceResponse.getMonth()));
    assertEquals(expectedYearMonth, monthlyBalanceResponse.getPeriod());
    assertEquals(balanceSnapshot, monthlyBalanceResponse.getClosingBalance());
    assertEquals(amount, monthlyBalanceResponse.getTotalDebits());
    assertEquals(JBH_ZERO, monthlyBalanceResponse.getTotalCredits());
    assertEquals(1, monthlyBalanceResponse.getTotalMovements());
    assertEquals(MovementType.DEPOSIT, mvmtResponse.get().movement().getMovementType());

  }

  @Test
  void shouldAddMvmtWithExistingMonthlyEntries() {
    // Given
    UUID userId = UUID.randomUUID();
    AccountId accountId = AccountId.generate();
    LocalDate movementDate = LocalDate.now();
    BigDecimal amount = new BigDecimal("100.00");

    AddBasicMovementRequest request = new AddBasicMovementRequest(movementDate, amount);
    AccountDomain accountDomain = AccountDomain.withId(accountId);

    when(accountRepository.findByAccountId(userId, accountId))
        .thenReturn(Optional.of(accountDomain));

    int existingEntries = 10;
    BigDecimal existingTotalDebits = new BigDecimal("1000.00");
    BigDecimal existingClosingBalance = new BigDecimal("600.00");
    BigDecimal existingTotalCredits = new BigDecimal("200.00");
    BigDecimal existingOpeningBalance = new BigDecimal("500.00");
    AccountMonthlyBalanceDomain existingMonthlyBalance = AccountMonthlyBalanceDomain.builder()
        .id(1L)
        .accountId(accountId)
        .year(movementDate.getYear())
        .month(movementDate.getMonthValue())
        .openingBalance(existingOpeningBalance)
        .closingBalance(existingClosingBalance)
        .totalCredits(existingTotalCredits)
        .totalDebits(existingTotalDebits)
        .totalMovements(existingEntries)
        .build();

    when(accountMonthlyBalanceRepository.findByAccountIdYearAndMonth(accountId, movementDate.getYear(),
        movementDate.getMonthValue()))
        .thenReturn(Optional.of(existingMonthlyBalance));

    // When & Then
    AtomicReference<AddBasicMovementDTO> processedResponse = new AtomicReference<>();
    assertDoesNotThrow(() -> processedResponse.set(
        useCaseInstanceTest.addBasicMovements(userId, accountId, request)));

    verify(accountRepository).findByAccountId(userId, accountId);
    verify(accountMovementRepository).save((AccountMovementDomain) any());
    verify(accountRepository).save(accountDomain);
    verify(accountMonthlyBalanceRepository, atMostOnce()).save((AccountMonthlyBalanceDomain) any());

    assertEquals(accountDomain.getMovementBalance(), amount);
    assertEquals(existingEntries + 1, processedResponse.get().monthlyBalance().getTotalMovements());
    assertEquals(existingTotalDebits.add(amount), processedResponse.get().monthlyBalance().getTotalDebits());
    assertEquals(existingOpeningBalance, processedResponse.get().monthlyBalance().getOpeningBalance());
    assertEquals(existingClosingBalance.add(amount), processedResponse.get().monthlyBalance().getClosingBalance());
    assertEquals(existingTotalCredits, processedResponse.get().monthlyBalance().getTotalCredits());
    assertEquals(MovementType.DEPOSIT, processedResponse.get().movement().getMovementType());
  }

  @Test
  @DisplayName("Should sync balances from the account with multiple movements correctly. NU")
  void shouldSyncBalancesWithMultipleMovements() throws ExecutionException, InterruptedException, TimeoutException {
    // Given
    UUID userId = UUID.randomUUID();
    AccountId accountId = AccountId.generate();
    List<AddBasicMovementRequest> allSimpleMovements = TestDataFactory.createAccountMovementTestData();

    // Then
    assertEquals(10, allSimpleMovements.size());

    // Verify first entry
    AddBasicMovementRequest firstEntry = allSimpleMovements.get(0);
    assertEquals(LocalDate.of(2024, 7, 30), firstEntry.entryDate());
    assertEquals(0, new BigDecimal("12591000.00").compareTo(firstEntry.totalAmount()));
    assertEquals(0, new BigDecimal("12689712.00").compareTo(firstEntry.balanceSnapshot()));

    // Verify last entry (account reaches zero)
    AddBasicMovementRequest lastEntry = allSimpleMovements.get(9);
    assertEquals(LocalDate.of(2025, 3, 31), lastEntry.entryDate());
    assertEquals(0, new BigDecimal("-3768488.00").compareTo(lastEntry.totalAmount()));
    assertEquals(0, JBH_ZERO.compareTo(lastEntry.balanceSnapshot()));

    // Verify some negative amounts
    AddBasicMovementRequest novemberEntry = allSimpleMovements.get(4); // 30/11/2024
    assertTrue(novemberEntry.totalAmount().compareTo(JBH_ZERO) < 0);
    assertEquals(0, new BigDecimal("-673605.00").compareTo(novemberEntry.totalAmount()));

    AccountDomain accountDomain = AccountDomain.withId(accountId);
    when(accountRepository.findByAccountId(userId, accountId)).thenReturn(Optional.of(accountDomain));

    //Then
    AtomicReference<AddMultipleBasicMovementDTO> processedResponse = new AtomicReference<>();
    assertDoesNotThrow(() -> processedResponse.set(
        useCaseInstanceTest.addBasicMovements(userId, accountId, allSimpleMovements)));

    verify(accountRepository).findByAccountId(userId, accountId);
    verify(accountMovementRepository).save((List<AccountMovementDomain>) any());
    verify(accountRepository).save(accountDomain);

    AccountDomain actualAccountResponse = processedResponse.get().account();
    List<AccountMonthlyBalanceDomain> actualMonthlyBalances = processedResponse.get().monthlyBalances();

    BigDecimal expectedProfitBalance = new BigDecimal("1786605.00");
    assertEquals(expectedProfitBalance.negate(), actualAccountResponse.getMovementBalance());
    assertEquals(new BigDecimal("0.00"), actualAccountResponse.getCurrentBalance());
    assertEquals(expectedProfitBalance, actualAccountResponse.getProfitBalance());
    assertEquals(9, actualMonthlyBalances.size());

    // Assert per month
    int expectedYear = 2024;
    int expectedMonth = 7;
    int sortedIndex = 0;
    AccountMonthlyBalanceDomain expectedMonthBalance = actualMonthlyBalances.get(sortedIndex);
    assertEquals(accountId, expectedMonthBalance.getAccountId());
    assertEquals(1, expectedMonthBalance.getTotalMovements());
    assertEquals(expectedMonth, expectedMonthBalance.getMonth());
    assertEquals(expectedYear, expectedMonthBalance.getYear());
    assertEquals(YearMonth.of(expectedYear, expectedMonth), expectedMonthBalance.getPeriod());
    assertEquals(numberOf("12689712"), expectedMonthBalance.getClosingBalance());
    assertEquals(numberOf("12591000"), expectedMonthBalance.getTotalDebits());
    assertEquals(numberOf("0"), expectedMonthBalance.getTotalCredits());

    // Assert per month
    expectedYear = 2024;
    expectedMonth = 8;
    sortedIndex = 1;
    expectedMonthBalance = actualMonthlyBalances.get(sortedIndex);
    assertEquals(accountId, expectedMonthBalance.getAccountId());
    assertEquals(1, expectedMonthBalance.getTotalMovements());
    assertEquals(expectedMonth, expectedMonthBalance.getMonth());
    assertEquals(expectedYear, expectedMonthBalance.getYear());
    assertEquals(YearMonth.of(expectedYear, expectedMonth), expectedMonthBalance.getPeriod());
    assertEquals(numberOf("22685312"), expectedMonthBalance.getTotalDebits());
    assertEquals(numberOf("35693653"), expectedMonthBalance.getClosingBalance());
    assertEquals(numberOf("0"), expectedMonthBalance.getTotalCredits());

    // Assert per month
    expectedYear = 2024;
    expectedMonth = 9;
    sortedIndex++;
    expectedMonthBalance = actualMonthlyBalances.get(sortedIndex);
    assertEquals(accountId, expectedMonthBalance.getAccountId());
    assertEquals(0, expectedMonthBalance.getTotalMovements());
    assertEquals(expectedMonth, expectedMonthBalance.getMonth());
    assertEquals(expectedYear, expectedMonthBalance.getYear());
    assertEquals(YearMonth.of(expectedYear, expectedMonth), expectedMonthBalance.getPeriod());
    assertEquals(numberOf("0"), expectedMonthBalance.getTotalDebits());
    assertEquals(numberOf("36017457"), expectedMonthBalance.getClosingBalance());
    assertEquals(numberOf("0"), expectedMonthBalance.getTotalCredits());

    // Assert per month
    expectedYear = 2024;
    expectedMonth = 10;
    sortedIndex++;
    expectedMonthBalance = actualMonthlyBalances.get(sortedIndex);
    assertEquals(accountId, expectedMonthBalance.getAccountId());
    assertEquals(0, expectedMonthBalance.getTotalMovements());
    assertEquals(expectedMonth, expectedMonthBalance.getMonth());
    assertEquals(expectedYear, expectedMonthBalance.getYear());
    assertEquals(YearMonth.of(expectedYear, expectedMonth), expectedMonthBalance.getPeriod());
    assertEquals(numberOf("0"), expectedMonthBalance.getTotalDebits());
    assertEquals(numberOf("36357576"), expectedMonthBalance.getClosingBalance());
    assertEquals(numberOf("0"), expectedMonthBalance.getTotalCredits());

    // Assert per month
    expectedYear = 2024;
    expectedMonth = 11;
    sortedIndex++;
    expectedMonthBalance = actualMonthlyBalances.get(sortedIndex);
    assertEquals(accountId, expectedMonthBalance.getAccountId());
    assertEquals(1, expectedMonthBalance.getTotalMovements());
    assertEquals(expectedMonth, expectedMonthBalance.getMonth());
    assertEquals(expectedYear, expectedMonthBalance.getYear());
    assertEquals(YearMonth.of(expectedYear, expectedMonth), expectedMonthBalance.getPeriod());
    assertEquals(numberOf("0"), expectedMonthBalance.getTotalDebits());
    assertEquals(numberOf("35982309"), expectedMonthBalance.getClosingBalance());
    assertEquals(numberOf("673605"), expectedMonthBalance.getTotalCredits());

    // Assert per month
    expectedYear = 2024;
    expectedMonth = 12;
    sortedIndex++;
    expectedMonthBalance = actualMonthlyBalances.get(sortedIndex);
    assertEquals(accountId, expectedMonthBalance.getAccountId());
    assertEquals(1, expectedMonthBalance.getTotalMovements());
    assertEquals(expectedMonth, expectedMonthBalance.getMonth());
    assertEquals(expectedYear, expectedMonthBalance.getYear());
    assertEquals(YearMonth.of(expectedYear, expectedMonth), expectedMonthBalance.getPeriod());
    assertEquals(numberOf("0"), expectedMonthBalance.getTotalDebits());
    assertEquals(numberOf("35000981"), expectedMonthBalance.getClosingBalance());
    assertEquals(numberOf("1271000"), expectedMonthBalance.getTotalCredits());

    // Assert per month
    expectedYear = 2025;
    expectedMonth = 1;
    sortedIndex++;
    expectedMonthBalance = actualMonthlyBalances.get(sortedIndex);
    assertEquals(accountId, expectedMonthBalance.getAccountId());
    assertEquals(1, expectedMonthBalance.getTotalMovements());
    assertEquals(expectedMonth, expectedMonthBalance.getMonth());
    assertEquals(expectedYear, expectedMonthBalance.getYear());
    assertEquals(YearMonth.of(expectedYear, expectedMonth), expectedMonthBalance.getPeriod());
    assertEquals(numberOf("0"), expectedMonthBalance.getTotalDebits());
    assertEquals(numberOf("25638626"), expectedMonthBalance.getClosingBalance());
    assertEquals(numberOf("9590134"), expectedMonthBalance.getTotalCredits());

    // Assert per month
    expectedYear = 2025;
    expectedMonth = 2;
    sortedIndex++;
    expectedMonthBalance = actualMonthlyBalances.get(sortedIndex);
    assertEquals(accountId, expectedMonthBalance.getAccountId());
    assertEquals(2, expectedMonthBalance.getTotalMovements());
    assertEquals(expectedMonth, expectedMonthBalance.getMonth());
    assertEquals(expectedYear, expectedMonthBalance.getYear());
    assertEquals(YearMonth.of(expectedYear, expectedMonth), expectedMonthBalance.getPeriod());
    assertEquals(numberOf("0"), expectedMonthBalance.getTotalDebits());
    assertEquals(numberOf("3768488"), expectedMonthBalance.getClosingBalance());
    assertEquals(numberOf("21759690"), expectedMonthBalance.getTotalCredits());

    // Assert per month
    expectedYear = 2025;
    expectedMonth = 3;
    sortedIndex++;
    expectedMonthBalance = actualMonthlyBalances.get(sortedIndex);
    assertEquals(accountId, expectedMonthBalance.getAccountId());
    assertEquals(1, expectedMonthBalance.getTotalMovements());
    assertEquals(expectedMonth, expectedMonthBalance.getMonth());
    assertEquals(expectedYear, expectedMonthBalance.getYear());
    assertEquals(YearMonth.of(expectedYear, expectedMonth), expectedMonthBalance.getPeriod());
    assertEquals(numberOf("0"), expectedMonthBalance.getTotalDebits());
    assertEquals(numberOf("0"), expectedMonthBalance.getClosingBalance());
    assertEquals(numberOf("3768488"), expectedMonthBalance.getTotalCredits());

    YearMonth initPeriod = YearMonth.of(firstEntry.entryDate().getYear(), firstEntry.entryDate().getMonthValue());
    when(accountMonthlyBalanceRepository.findNextBalancesFromPeriodInclusive(accountId, initPeriod))
        .thenReturn(actualMonthlyBalances);
    CompletableFuture<List<AccountMonthlyBalanceDomain>> futureResponse =
        monthlyBalanceSyncerService.saveMonthlyBalancesASYNC(accountId, actualMonthlyBalances);

    List<AccountMonthlyBalanceDomain> actualProfitBalances = futureResponse.get(5, TimeUnit.SECONDS);
    assertEquals(10, actualProfitBalances.size());

    int index = -1;
    AccountMonthlyBalanceDomain actualResponse = null;
    YearMonth expectedPeriod = YearMonth.of(2024, 7);

    // Row 1
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(numberOf("12689712"), actualResponse.getClosingBalance());
    assertEquals(numberOf("98712"), actualResponse.getMonthlyProfit());
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());

// Row 2
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(numberOf("35693653"), actualResponse.getClosingBalance());
    assertEquals(numberOf("318629"), actualResponse.getMonthlyProfit());
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());

// Row 3
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(numberOf("36017457"), actualResponse.getClosingBalance());
    assertEquals(numberOf("323804"), actualResponse.getMonthlyProfit());
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());

// Row 4
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(numberOf("36357576"), actualResponse.getClosingBalance());
    assertEquals(numberOf("340119"), actualResponse.getMonthlyProfit());
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());

// Row 5
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(numberOf("35982309"), actualResponse.getClosingBalance());
    assertEquals(numberOf("298338"), actualResponse.getMonthlyProfit());
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());

// Row 6
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(numberOf("35000981"), actualResponse.getClosingBalance());
    assertEquals(numberOf("289672"), actualResponse.getMonthlyProfit());
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());

// Row 7
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(numberOf("25638626"), actualResponse.getClosingBalance());
    assertEquals(numberOf("227779"), actualResponse.getMonthlyProfit());
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());

// Row 8
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(numberOf("3768488"), actualResponse.getClosingBalance());
    assertEquals(numberOf("-110448"), actualResponse.getMonthlyProfit());
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());

// Row 9
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(numberOf("0"), actualResponse.getClosingBalance());
    assertEquals(numberOf("0"), actualResponse.getMonthlyProfit());
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());

  }

  private BigDecimal numberOf(String val) {
    return MoneyUtils.withJBHDecimals(new BigDecimal(val));
  }

  @Test
  @DisplayName("Should create movements for PIBI")
  void shouldCreatedMovementsForPIBI() throws ExecutionException, InterruptedException, TimeoutException {
    // Given
    List<AddBasicMovementRequest> testData = TestDataFactory.movementsForPIBI();

    // Then
    assertEquals(25, testData.size());

    // Verify first entry (October 2023)
    YearMonth firstEntryYM = YearMonth.of(2023, 10);
    AddBasicMovementRequest firstEntry = testData.get(0);
    assertEquals(LocalDate.of(2023, 10, 31), firstEntry.entryDate());
    assertEquals(0, new BigDecimal("13010000.00").compareTo(firstEntry.totalAmount()));
    assertEquals(0, new BigDecimal("13062118.00").compareTo(firstEntry.balanceSnapshot()));

    // Verify large deposit in May 2024
    AddBasicMovementRequest mayDeposit = testData.get(8); // 03/05/2024
    assertEquals(LocalDate.of(2024, 5, 3), mayDeposit.entryDate());
    assertEquals(0, new BigDecimal("24100000.00").compareTo(mayDeposit.totalAmount()));
    assertEquals(0, new BigDecimal("65394365.00").compareTo(mayDeposit.balanceSnapshot()));

    // Verify large withdrawal in July 2024
    AddBasicMovementRequest julyWithdrawal = testData.get(13); // 31/07/2024
    assertEquals(LocalDate.of(2024, 7, 31), julyWithdrawal.entryDate());
    assertEquals(0, new BigDecimal("-20000000.00").compareTo(julyWithdrawal.totalAmount()));
    assertEquals(0, new BigDecimal("32809397.00").compareTo(julyWithdrawal.balanceSnapshot()));

    // Verify final entry (August 2025)
    AddBasicMovementRequest lastEntry = testData.get(24);
    assertEquals(LocalDate.of(2025, 8, 30), lastEntry.entryDate());
    assertEquals(0, JBH_ZERO.compareTo(lastEntry.totalAmount()));
    assertEquals(0, new BigDecimal("37074883.00").compareTo(lastEntry.balanceSnapshot()));

    UUID userId = UUID.randomUUID();
    AccountId accountId = AccountId.generate();
    AccountDomain account = AccountDomain.withId(accountId);
    when(accountRepository.findByAccountId(userId, accountId)).thenReturn(Optional.of(account));

    AtomicReference<AddMultipleBasicMovementDTO> processedResponse = new AtomicReference<>();
    assertDoesNotThrow(() ->
        processedResponse.set(useCaseInstanceTest.addBasicMovements(userId, accountId, testData)));

    AccountDomain actualAccount = processedResponse.get().account();

    assertEquals(numberOf("29710000"), actualAccount.getMovementBalance());
    assertEquals(numberOf("37074883"), actualAccount.getCurrentBalance());
    assertEquals(numberOf("7364883"), actualAccount.getProfitBalance());
    List<AccountMonthlyBalanceDomain> savedMonthlyBalances = processedResponse.get().monthlyBalances();

    // THEN
    YearMonth expectedPeriod = YearMonth.of(2023, 10);
    log.info("Account Tested. Preparing to test the monthly Balances ...{} ", expectedPeriod);
    when(accountMonthlyBalanceRepository.findNextBalancesFromPeriodInclusive(accountId, expectedPeriod)).
        thenReturn(savedMonthlyBalances);
    CompletableFuture<List<AccountMonthlyBalanceDomain>> futureResponse =
        monthlyBalanceSyncerService.saveMonthlyBalancesASYNC(accountId, savedMonthlyBalances);
    List<AccountMonthlyBalanceDomain> actualMonthlyBalances = futureResponse.get();

    // Response
    int index = -1;
    AccountMonthlyBalanceDomain actualResponse = null;

    // Row Octubre/23
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("13010000"), actualResponse.getMovementBalance());
    assertEquals(numberOf("13062118"), actualResponse.getClosingBalance());
    assertEquals(numberOf("52118"), actualResponse.getMonthlyProfit());

// Row Noviembre/23
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("13000000"), actualResponse.getMovementBalance());
    assertEquals(numberOf("26309995"), actualResponse.getClosingBalance());
    assertEquals(numberOf("247877"), actualResponse.getMonthlyProfit());

// Row Diciembre/23
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("13000000"), actualResponse.getMovementBalance());
    assertEquals(numberOf("39645174"), actualResponse.getClosingBalance());
    assertEquals(numberOf("335179"), actualResponse.getMonthlyProfit());

// Row Enero/24
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("40001713"), actualResponse.getClosingBalance());
    assertEquals(numberOf("356539"), actualResponse.getMonthlyProfit());

// Row Febrero/24
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("40001713"), actualResponse.getClosingBalance());
    assertEquals(numberOf("0"), actualResponse.getMonthlyProfit());

// Row Marzo/24
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("40701019"), actualResponse.getClosingBalance());
    assertEquals(numberOf("699306"), actualResponse.getMonthlyProfit());

// Row Abril/24
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("41197965"), actualResponse.getClosingBalance());
    assertEquals(numberOf("496946"), actualResponse.getMonthlyProfit());

// Row Mayo/24
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("-5900000"), actualResponse.getMovementBalance());
    assertEquals(numberOf("35762219"), actualResponse.getClosingBalance());
    assertEquals(numberOf("464254"), actualResponse.getMonthlyProfit());

// Row Junio/24
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("16600000"), actualResponse.getMovementBalance());
    assertEquals(numberOf("52889397"), actualResponse.getClosingBalance());
    assertEquals(numberOf("527178"), actualResponse.getMonthlyProfit());

// Row Julio/24
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("-20000000"), actualResponse.getMovementBalance());
    assertEquals(numberOf("32809397"), actualResponse.getClosingBalance());
    assertEquals(numberOf("-80000"), actualResponse.getMonthlyProfit());

// Row Agosto/24
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("33560000"), actualResponse.getClosingBalance());
    assertEquals(numberOf("750603"), actualResponse.getMonthlyProfit());

// Row Septiembre/24
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("33876037"), actualResponse.getClosingBalance());
    assertEquals(numberOf("316037"), actualResponse.getMonthlyProfit());

// Row Octubre/24
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("34204708"), actualResponse.getClosingBalance());
    assertEquals(numberOf("328671"), actualResponse.getMonthlyProfit());

// Row Noviembre/24
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("34525863"), actualResponse.getClosingBalance());
    assertEquals(numberOf("321155"), actualResponse.getMonthlyProfit());

// Row Diciembre/24
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("34838994"), actualResponse.getClosingBalance());
    assertEquals(numberOf("313131"), actualResponse.getMonthlyProfit());

// Row Enero/25
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("35102299"), actualResponse.getClosingBalance());
    assertEquals(numberOf("263305"), actualResponse.getMonthlyProfit());

// Row Febrero/25
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("35341920"), actualResponse.getClosingBalance());
    assertEquals(numberOf("239621"), actualResponse.getMonthlyProfit());

// Row Marzo/25
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("35593716"), actualResponse.getClosingBalance());
    assertEquals(numberOf("251796"), actualResponse.getMonthlyProfit());

// Row Abril/25
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("35829015"), actualResponse.getClosingBalance());
    assertEquals(numberOf("235299"), actualResponse.getMonthlyProfit());

// Row Mayo/25
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("36102719"), actualResponse.getClosingBalance());
    assertEquals(numberOf("273704"), actualResponse.getMonthlyProfit());

    // Row Jun/25
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("36102719"), actualResponse.getClosingBalance());
    assertEquals(numberOf("0"), actualResponse.getMonthlyProfit());

    // Row Jul/25
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("36102719"), actualResponse.getClosingBalance());
    assertEquals(numberOf("0"), actualResponse.getMonthlyProfit());

// Row 25/08/2025
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("37074883"), actualResponse.getClosingBalance());
    assertEquals(numberOf("972164"), actualResponse.getMonthlyProfit());

    // Row 25/09/2025 - Last one affected by movement. Only Opening Balance.
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("37074883"), actualResponse.getOpeningBalance());
    assertEquals(numberOf("0"), actualResponse.getMonthlyProfit());
    assertEquals(numberOf("0"), actualResponse.getClosingBalance());

    AtomicInteger lastIdx = new AtomicInteger(++index);
    assertThrows(IndexOutOfBoundsException.class, () -> actualMonthlyBalances.get(lastIdx.get()));

  }

  @Test
  @DisplayName("Should create movements for PIKMI")
  void shouldCreatedMovementsForPIKMI() throws ExecutionException, InterruptedException, TimeoutException {
    // Given
    List<AddBasicMovementRequest> testData = TestDataFactory.movementsForPIKMI();

    // Then
    assertEquals(46, testData.size());

    UUID userId = UUID.randomUUID();
    AccountId accountId = AccountId.generate();
    AccountDomain account = AccountDomain.withId(accountId);
    when(accountRepository.findByAccountId(userId, accountId)).thenReturn(Optional.of(account));

    AtomicReference<AddMultipleBasicMovementDTO> processedResponse = new AtomicReference<>();
    assertDoesNotThrow(() ->
        processedResponse.set(useCaseInstanceTest.addBasicMovements(userId, accountId, testData)));

    AccountDomain actualAccount = processedResponse.get().account();

    assertEquals(numberOf("56386448"), actualAccount.getMovementBalance());
    assertEquals(numberOf("70908065"), actualAccount.getCurrentBalance());
    assertEquals(numberOf("14521617"), actualAccount.getProfitBalance());
    List<AccountMonthlyBalanceDomain> savedMonthlyBalances = processedResponse.get().monthlyBalances();

    YearMonth expectedPeriod = YearMonth.of(2023, 7);
    log.info("Account Tested. Preparing to test the monthly Balances ASYNC...{} ", expectedPeriod);
    when(accountMonthlyBalanceRepository.findNextBalancesFromPeriodInclusive(accountId, expectedPeriod)).
        thenReturn(savedMonthlyBalances);

    CompletableFuture<List<AccountMonthlyBalanceDomain>> futureResponse =
        monthlyBalanceSyncerService.saveMonthlyBalancesASYNC(accountId, savedMonthlyBalances);
    List<AccountMonthlyBalanceDomain> actualMonthlyBalances = futureResponse.get();

    int index = -1;
    AccountMonthlyBalanceDomain actualResponse = null;

    // Row Julio/23
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("15000000"), actualResponse.getMovementBalance());
    assertEquals(numberOf("15074686"), actualResponse.getClosingBalance());
    assertEquals(numberOf("74686"), actualResponse.getMonthlyProfit());

// Row Agosto/23
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("29657000"), actualResponse.getMovementBalance());
    assertEquals(numberOf("45525753"), actualResponse.getClosingBalance());
    assertEquals(numberOf("794067"), actualResponse.getMonthlyProfit());

// Row Septiembre/23
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("9117414"), actualResponse.getMovementBalance());
    assertEquals(numberOf("54787054"), actualResponse.getClosingBalance());
    assertEquals(numberOf("143887"), actualResponse.getMonthlyProfit());

// Row Octubre/23
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("-5550618"), actualResponse.getMovementBalance());
    assertEquals(numberOf("50069218"), actualResponse.getClosingBalance());
    assertEquals(numberOf("832782"), actualResponse.getMonthlyProfit());

// Row Noviembre/23
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("-11728845"), actualResponse.getMovementBalance());
    assertEquals(numberOf("40726167"), actualResponse.getClosingBalance());
    assertEquals(numberOf("2385794"), actualResponse.getMonthlyProfit());

// Row Diciembre/23
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("-17246355"), actualResponse.getMovementBalance());
    assertEquals(numberOf("23720010"), actualResponse.getClosingBalance());
    assertEquals(numberOf("240198"), actualResponse.getMonthlyProfit());

// Row Enero/24
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("-12346955"), actualResponse.getMovementBalance());
    assertEquals(numberOf("11480093"), actualResponse.getClosingBalance());
    assertEquals(numberOf("107038"), actualResponse.getMonthlyProfit());

// Row Febrero/24
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("-7346955"), actualResponse.getMovementBalance());
    assertEquals(numberOf("4105556"), actualResponse.getClosingBalance());
    assertEquals(numberOf("-27582"), actualResponse.getMonthlyProfit());

// Row Marzo/24
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("653645"), actualResponse.getMovementBalance());
    assertEquals(numberOf("4839561"), actualResponse.getClosingBalance());
    assertEquals(numberOf("80360"), actualResponse.getMonthlyProfit());

// Row Abril/24
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("653645"), actualResponse.getMovementBalance());
    assertEquals(numberOf("5542958"), actualResponse.getClosingBalance());
    assertEquals(numberOf("49752"), actualResponse.getMonthlyProfit());

// Row Mayo/24
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("2401286"), actualResponse.getMovementBalance());
    assertEquals(numberOf("8016341"), actualResponse.getClosingBalance());
    assertEquals(numberOf("72097"), actualResponse.getMonthlyProfit());

// Row Junio/24
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("520531"), actualResponse.getMovementBalance());
    assertEquals(numberOf("8613715"), actualResponse.getClosingBalance());
    assertEquals(numberOf("76843"), actualResponse.getMonthlyProfit());

// Row Julio/24
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("520531"), actualResponse.getMovementBalance());
    assertEquals(numberOf("9136328"), actualResponse.getClosingBalance());
    assertEquals(numberOf("2082"), actualResponse.getMonthlyProfit());

// Row Agosto/24
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("60520531"), actualResponse.getMovementBalance());
    assertEquals(numberOf("72058459"), actualResponse.getClosingBalance());
    assertEquals(numberOf("2401600"), actualResponse.getMonthlyProfit());

// Row Septiembre/24
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("520531"), actualResponse.getMovementBalance());
    assertEquals(numberOf("73257353"), actualResponse.getClosingBalance());
    assertEquals(numberOf("678363"), actualResponse.getMonthlyProfit());

// Row Octubre/24
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("520531"), actualResponse.getMovementBalance());
    assertEquals(numberOf("74490431"), actualResponse.getClosingBalance());
    assertEquals(numberOf("712547"), actualResponse.getMonthlyProfit());

// Row Noviembre/24
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("520531"), actualResponse.getMovementBalance());
    assertEquals(numberOf("75711997"), actualResponse.getClosingBalance());
    assertEquals(numberOf("701035"), actualResponse.getMonthlyProfit());

// Row Diciembre/24
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("76398664"), actualResponse.getClosingBalance());
    assertEquals(numberOf("686667"), actualResponse.getMonthlyProfit());

// Row Enero/25
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("76976067"), actualResponse.getClosingBalance());
    assertEquals(numberOf("577403"), actualResponse.getMonthlyProfit());

// Row Febrero/25
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("77501534"), actualResponse.getClosingBalance());
    assertEquals(numberOf("525467"), actualResponse.getMonthlyProfit());

// Row Marzo/25
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("78053700"), actualResponse.getClosingBalance());
    assertEquals(numberOf("552166"), actualResponse.getMonthlyProfit());

// Row Abril/25
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("-10000000"), actualResponse.getMovementBalance());
    assertEquals(numberOf("68053700"), actualResponse.getClosingBalance());
    assertEquals(numberOf("0"), actualResponse.getMonthlyProfit());

    // Row May/25
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("68053700"), actualResponse.getClosingBalance());
    assertEquals(numberOf("0"), actualResponse.getMonthlyProfit());

    // Row June/25
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("68053700"), actualResponse.getClosingBalance());
    assertEquals(numberOf("0"), actualResponse.getMonthlyProfit());

    // Row July/25
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("68053700"), actualResponse.getClosingBalance());
    assertEquals(numberOf("0"), actualResponse.getMonthlyProfit());

// Row Agosto/25
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("70908065"), actualResponse.getClosingBalance());
    assertEquals(numberOf("2854365"), actualResponse.getMonthlyProfit());

    // Row 25/09/2025 - Last one affected by movement. Only Opening Balance.
    actualResponse = actualMonthlyBalances.get(++index);
    assertEquals(expectedPeriod.plusMonths(index), actualResponse.getPeriod());
    assertEquals(numberOf("0"), actualResponse.getMovementBalance());
    assertEquals(numberOf("70908065"), actualResponse.getOpeningBalance());
    assertEquals(numberOf("0"), actualResponse.getMonthlyProfit());
    assertEquals(numberOf("0"), actualResponse.getClosingBalance());

    AtomicInteger lastIdx = new AtomicInteger(++index);
    assertThrows(IndexOutOfBoundsException.class, () -> actualMonthlyBalances.get(lastIdx.get()));
  }
}

