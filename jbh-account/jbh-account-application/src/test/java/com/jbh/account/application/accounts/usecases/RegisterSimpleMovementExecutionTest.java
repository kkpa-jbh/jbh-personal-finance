package com.jbh.account.application.accounts.usecases;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jbh.account.application.accounts.dto.AddBasicMovementResponse;
import com.jbh.account.application.accounts.ports.input.RegisterSimpleMovementInputPort;
import com.jbh.account.application.accounts.ports.output.AccountMonthlyBalanceRepository;
import com.jbh.account.application.accounts.ports.output.AccountRepository;
import com.jbh.account.application.accounts.usecases.utils.TestDataFactory;
import com.jbh.account.application.accounts.usecases.utils.UnitOfWorkTest;
import com.jbh.account.application.accounts.vo.AddBasicMovementRequest;
import com.jbh.account.application.accounts.vo.AddMultipleBasicMovementResponse;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class RegisterSimpleMovementExecutionTest {

  private final UnitOfWork unitOfWork = new UnitOfWorkTest();
  @Mock
  private AccountRepository accountRepository;
  @Mock
  private AccountMovementRepository accountMovementRepository;
  @Mock
  private AccountMonthlyBalanceRepository accountMonthlyBalanceRepository;

  private RegisterSimpleMovementInputPort useCaseInstanceTest;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    useCaseInstanceTest = new RegisterSimpleMovementInputPort(accountRepository, accountMovementRepository,
        unitOfWork,
        accountMonthlyBalanceRepository);
  }

  @Test
  void shouldAddMovement_WhenNotSnapshotProvided() {
    // Given
    UUID userId = UUID.randomUUID();
    AccountId accountId = AccountId.generate();
    LocalDate movementDate = LocalDate.now();
    BigDecimal amount = new BigDecimal("100.00");

    AddBasicMovementRequest request = new AddBasicMovementRequest(movementDate, amount);
    AccountDomain accountDomain = AccountDomain.withId(accountId);

    when(accountRepository.findByAccountId(userId, accountId))
        .thenReturn(Optional.of(accountDomain));

    // When & Then
    AtomicReference<AddBasicMovementResponse> mvmtResponse = new AtomicReference<>();
    assertDoesNotThrow(
        () -> mvmtResponse.set(useCaseInstanceTest.addBasicMovements(userId, accountId, request)));

    verify(accountRepository).findByAccountId(userId, accountId);
    verify(accountMovementRepository).save((AccountMovementDomain) any());
    verify(accountRepository).save(accountDomain);

    assertEquals(accountDomain.getMovementBalance(), amount);
    assertEquals(amount, mvmtResponse.get().monthlyBalance().getTotalDebits());
    assertEquals(amount, mvmtResponse.get().monthlyBalance().getClosingBalance());
  }

  @Test
  void shouldAddMovement_WhenOnlySnapshotProvided() {
    // Given
    UUID userId = UUID.randomUUID();
    AccountId accountId = AccountId.generate();
    LocalDate movementDate = LocalDate.now();
    BigDecimal balanceSnashot = new BigDecimal("200.00");

    AddBasicMovementRequest request = new AddBasicMovementRequest(movementDate, null, balanceSnashot);
    AccountDomain accountDomain = AccountDomain.withId(accountId);
    BigDecimal existingAccountPpalBalance = new BigDecimal("100.00");
    accountDomain.setMovementBalance(existingAccountPpalBalance);
    accountDomain.setCurrentBalance(new BigDecimal("190.00"));

    when(accountRepository.findByAccountId(userId, accountId))
        .thenReturn(Optional.of(accountDomain));

    // When & Then
    AtomicReference<AddBasicMovementResponse> mvmtResponse = new AtomicReference<>();
    assertDoesNotThrow(
        () -> mvmtResponse.set(useCaseInstanceTest.addBasicMovements(userId, accountId, request)));

    verify(accountRepository).findByAccountId(userId, accountId);
    verify(accountMovementRepository).save((AccountMovementDomain) any());
    verify(accountRepository).save(accountDomain);

    assertEquals(balanceSnashot, mvmtResponse.get().account().getCurrentBalance());
    assertEquals(existingAccountPpalBalance, mvmtResponse.get().account().getMovementBalance());
    assertEquals(BigDecimal.ZERO, mvmtResponse.get().monthlyBalance().getTotalDebits());
    assertEquals(0, mvmtResponse.get().monthlyBalance().getTotalMovements());
    assertEquals(BigDecimal.ZERO, mvmtResponse.get().monthlyBalance().getTotalCredits());
    assertEquals(balanceSnashot, mvmtResponse.get().monthlyBalance().getClosingBalance());
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
    AtomicReference<AddBasicMovementResponse> mvmtResponse = new AtomicReference<>();
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
    assertEquals(BigDecimal.ZERO, monthlyBalanceResponse.getTotalCredits());
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
    AtomicReference<AddBasicMovementResponse> processedResponse = new AtomicReference<>();
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
  void shouldSyncBalancesWithMultipleMovements() {
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
    assertEquals(0, BigDecimal.ZERO.compareTo(lastEntry.balanceSnapshot()));

    // Verify some negative amounts
    AddBasicMovementRequest novemberEntry = allSimpleMovements.get(4); // 30/11/2024
    assertTrue(novemberEntry.totalAmount().compareTo(BigDecimal.ZERO) < 0);
    assertEquals(0, new BigDecimal("-673605.00").compareTo(novemberEntry.totalAmount()));

    AccountDomain accountDomain = AccountDomain.withId(accountId);
    when(accountRepository.findByAccountId(userId, accountId)).thenReturn(Optional.of(accountDomain));

    //Then
    AtomicReference<AddMultipleBasicMovementResponse> processedResponse = new AtomicReference<>();
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

  }

  private BigDecimal numberOf(String val) {
    return MoneyUtils.withJBHDecimals(new BigDecimal(val));
  }

  @Test
  @DisplayName("Should create extended account movement test data with correct values")
  void shouldCreateExtendedAccountMovementTestData() {
    // Given
    List<AddBasicMovementRequest> testData = TestDataFactory.createExtendedAccountMovementTestData();

    // Then
    assertEquals(25, testData.size());

    // Verify first entry (October 2023)
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
    assertEquals(0, BigDecimal.ZERO.compareTo(lastEntry.totalAmount()));
    assertEquals(0, new BigDecimal("37074883.00").compareTo(lastEntry.balanceSnapshot()));
  }
}

