package usecases;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jbh.account_app.accounts.ports.input.RegisterSimpleMovementInputPort;
import com.jbh.account_app.accounts.ports.output.AccountMonthlyBalanceRepository;
import com.jbh.account_app.accounts.ports.output.AccountRepository;
import com.jbh.account_app.accounts.vo.AddMovementWithDateAmount;
import com.jbh.account_app.acid.UnitOfWork;
import com.jbh.account_app.movements.ports.output.AccountMovementRepository;
import com.jbh.accounts_mgmt.accounts.AccountDomain;
import com.jbh.accounts_mgmt.accounts.AccountId;
import com.jbh.accounts_mgmt.accounts.AccountMonthlyBalanceDomain;
import com.jbh.accounts_mgmt.exceptions.GenericSpecificationException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import utils.TestDataFactory;
import utils.UnitOfWorkTest;

public class AddAccountEntryUseCaseTest {

  private final UnitOfWork unitOfWork = new UnitOfWorkTest();
  @Mock
  private AccountRepository accountRepository;
  @Mock
  private AccountMovementRepository accountMovementRepository;
  @Mock
  private AccountMonthlyBalanceRepository accountMonthlyBalanceRepository;
  private RegisterSimpleMovementInputPort registerSimpleMovementInputPort;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    registerSimpleMovementInputPort = new RegisterSimpleMovementInputPort(accountRepository, accountMovementRepository,
        unitOfWork,
        accountMonthlyBalanceRepository);
  }


  @Test
  void shouldThrowException_WhenUserIdIsNull() {
    // Given
    UUID userId = null;
    AccountId accountId = AccountId.generate();
    LocalDate movementDate = LocalDate.now();
    BigDecimal amount = new BigDecimal("100.00");

    AddMovementWithDateAmount request = new AddMovementWithDateAmount(movementDate, amount);

    // When & Then
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> registerSimpleMovementInputPort.addSimpleMovement(userId, accountId, request));

    assertEquals("User ID cannot be null", exception.getMessage());
  }

  @Test
  void shouldThrowException_WhenmovemenDateIsNull() {
    // Given
    UUID userId = UUID.randomUUID();
    AccountId accountId = AccountId.generate();
    BigDecimal amount = new BigDecimal("100.00");
    AddMovementWithDateAmount request = new AddMovementWithDateAmount(null, amount);

    // When & Then
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> registerSimpleMovementInputPort.addSimpleMovement(userId, accountId, request));

    assertEquals("Entry date cannot be null", exception.getMessage());
  }

  @Test
  void shouldThrowException_WhenAmountIsNull() {
    // Given
    UUID userId = UUID.randomUUID();
    AccountId accountId = AccountId.generate();
    LocalDate movementDate = LocalDate.now();

    AddMovementWithDateAmount request = new AddMovementWithDateAmount(movementDate, null);

    // When & Then
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> registerSimpleMovementInputPort.addSimpleMovement(userId, accountId, request));

    assertEquals("There is not any amount to add", exception.getMessage());
  }

  @Test
  void shouldThrowException_WhenAccountNotFound() {
    // Given
    UUID userId = UUID.randomUUID();
    AccountId accountId = AccountId.generate();
    LocalDate movementDate = LocalDate.now();
    BigDecimal amount = new BigDecimal("100.00");

    AddMovementWithDateAmount request = new AddMovementWithDateAmount(movementDate, amount);

    when(accountRepository.findByAccountId(userId, accountId))
        .thenReturn(Optional.empty());

    // When & Then
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> registerSimpleMovementInputPort.addSimpleMovement(userId, accountId, request));

    assertEquals("Account not found", exception.getMessage());
    verify(accountRepository).findByAccountId(userId, accountId);
  }

  @Test
  void shouldHandleZeroAmount() {
    // Given
    UUID userId = UUID.randomUUID();
    AccountId accountId = AccountId.generate();
    LocalDate movementDate = LocalDate.now();
    BigDecimal amount = BigDecimal.ZERO;

    AddMovementWithDateAmount request = new AddMovementWithDateAmount(movementDate, amount);
    AccountDomain accountDomain = AccountDomain.withId(accountId);

    when(accountRepository.findByAccountId(userId, accountId))
        .thenReturn(Optional.of(accountDomain));

    // When & Then
    assertDoesNotThrow(() -> registerSimpleMovementInputPort.addSimpleMovement(userId, accountId, request));
    verify(accountRepository).findByAccountId(userId, accountId);
    verify(accountMovementRepository).save(any());
    verify(accountRepository).save(accountDomain);
    assertEquals(accountDomain.getBalance(), amount);
  }

  @Test
  void shouldHandleNegativeAmount() {
    // Given
    UUID userId = UUID.randomUUID();
    AccountId accountId = AccountId.generate();
    LocalDate movementDate = LocalDate.now();
    BigDecimal amount = new BigDecimal("-50.00");

    AddMovementWithDateAmount request = new AddMovementWithDateAmount(movementDate, amount);
    AccountDomain accountDomain = AccountDomain.withId(accountId);
    accountDomain.setBalance(new BigDecimal("170.00"));
    accountDomain.setEffectiveBalance(new BigDecimal("170.00"));

    when(accountRepository.findByAccountId(userId, accountId))
        .thenReturn(Optional.of(accountDomain));

    // When & Then
    assertDoesNotThrow(() -> registerSimpleMovementInputPort.addSimpleMovement(userId, accountId, request));

    verify(accountRepository).findByAccountId(userId, accountId);
    verify(accountMovementRepository).save(any());
    verify(accountRepository).save(accountDomain);
    assertEquals(new BigDecimal("120.00"), accountDomain.getBalance());
  }

  @Test
  void shouldHandlePastDate() {
    // Given
    UUID userId = UUID.randomUUID();
    AccountId accountId = AccountId.generate();
    LocalDate movementDate = LocalDate.now().minusDays(30);
    BigDecimal amount = new BigDecimal("100.00");

    AddMovementWithDateAmount request = new AddMovementWithDateAmount(movementDate, amount);
    AccountDomain accountDomain = AccountDomain.withId(accountId);

    when(accountRepository.findByAccountId(userId, accountId))
        .thenReturn(Optional.of(accountDomain));

    // When & Then
    assertDoesNotThrow(() -> registerSimpleMovementInputPort.addSimpleMovement(userId, accountId, request));

    verify(accountRepository).findByAccountId(userId, accountId);
    verify(accountMovementRepository).save(any());
    verify(accountRepository).save(accountDomain);
  }

  @Test
  void shouldNotHandleFutureDate() {
    // Given
    UUID userId = UUID.randomUUID();
    AccountId accountId = AccountId.generate();
    LocalDate movementDate = LocalDate.now().plusDays(30);
    BigDecimal amount = new BigDecimal("100.00");

    AddMovementWithDateAmount request = new AddMovementWithDateAmount(movementDate, amount);
    AccountDomain accountDomain = AccountDomain.withId(accountId);

    when(accountRepository.findByAccountId(userId, accountId))
        .thenReturn(Optional.of(accountDomain));

    // When & Then
    assertThrows(GenericSpecificationException.class,
        () -> registerSimpleMovementInputPort.addSimpleMovement(userId, accountId, request));

    verify(accountRepository).findByAccountId(userId, accountId);
    verify(accountMovementRepository, never()).save(any());
    verify(accountRepository, never()).save(accountDomain);
  }

  @Test
  void shouldThrowErrorWithInsufficientEffectiveBalance() {
    // Given
    UUID userId = UUID.randomUUID();
    AccountId accountId = AccountId.generate();
    LocalDate movementDate = LocalDate.now();
    BigDecimal amount = new BigDecimal("-50.00");

    AddMovementWithDateAmount request = new AddMovementWithDateAmount(movementDate, amount);
    AccountDomain accountDomain = AccountDomain.withId(accountId);
    accountDomain.setBalance(new BigDecimal("30.00"));
    accountDomain.setEffectiveBalance(new BigDecimal("30.00"));

    when(accountRepository.findByAccountId(userId, accountId))
        .thenReturn(Optional.of(accountDomain));

    // When & Then
    GenericSpecificationException exception = assertThrows(GenericSpecificationException.class,
        () -> registerSimpleMovementInputPort.addSimpleMovement(userId, accountId, request));

    assertEquals("Insufficient effective balance", exception.getMessage());

    verify(accountRepository).findByAccountId(userId, accountId);
    verify(accountMovementRepository, never()).save(any());
    verify(accountRepository, never()).save(accountDomain);
  }

  @Test
  void shouldAddmovemenSuccessfully_WhenValidInputProvided() {
    // Given
    UUID userId = UUID.randomUUID();
    AccountId accountId = AccountId.generate();
    LocalDate movementDate = LocalDate.now();
    BigDecimal amount = new BigDecimal("100.00");

    AddMovementWithDateAmount request = new AddMovementWithDateAmount(movementDate, amount);
    AccountDomain accountDomain = AccountDomain.withId(accountId);

    when(accountRepository.findByAccountId(userId, accountId))
        .thenReturn(Optional.of(accountDomain));

    // When & Then
    assertDoesNotThrow(() -> registerSimpleMovementInputPort.addSimpleMovement(userId, accountId, request));

    verify(accountRepository).findByAccountId(userId, accountId);
    verify(accountMovementRepository).save(any());
    verify(accountRepository).save(accountDomain);

    assertEquals(accountDomain.getBalance(), amount);
  }

  @Test
  void shouldAddEntryWithExistingMonthlyEntries() {
    // Given
    UUID userId = UUID.randomUUID();
    AccountId accountId = AccountId.generate();
    LocalDate movementDate = LocalDate.now();
    BigDecimal amount = new BigDecimal("100.00");

    AddMovementWithDateAmount request = new AddMovementWithDateAmount(movementDate, amount);
    AccountDomain accountDomain = AccountDomain.withId(accountId);

    when(accountRepository.findByAccountId(userId, accountId))
        .thenReturn(Optional.of(accountDomain));

    int existingEntries = 10;
    BigDecimal existingTotalDebits = new BigDecimal("1000.00");
    BigDecimal existingClosingBalance = new BigDecimal("600.00");
    AccountMonthlyBalanceDomain existingBalance = AccountMonthlyBalanceDomain.builder()
        .id(1L)
        .accountId(accountId)
        .balanceYear(movementDate.getYear())
        .balanceMonth(movementDate.getMonthValue())
        .openingBalance(new BigDecimal("500.00"))
        .closingBalance(existingClosingBalance)
        .totalCredits(new BigDecimal("200.00"))
        .totalDebits(existingTotalDebits)
        .MovementCount(existingEntries)
        .build();

    when(accountMonthlyBalanceRepository.findByAccountIdYearAndMonth(accountId, movementDate.getYear(),
        movementDate.getMonthValue()))
        .thenReturn(Optional.of(existingBalance));

    // When & Then
    assertDoesNotThrow(() -> registerSimpleMovementInputPort.addSimpleMovement(userId, accountId, request));

    verify(accountRepository).findByAccountId(userId, accountId);
    verify(accountMovementRepository).save(any());
    verify(accountRepository).save(accountDomain);
    verify(accountMonthlyBalanceRepository, atMostOnce()).save(any());

    assertEquals(accountDomain.getBalance(), amount);
    assertEquals(existingEntries + 1, existingBalance.getMovementCount());
  }

  @Test
  @DisplayName("Should create account movement test data with correct values")
  void shouldCreateAccountMovementTestData() {
    // Given
    List<AddMovementWithDateAmount> testData = TestDataFactory.createAccountMovementTestData();

    // Then
    assertEquals(10, testData.size());

    // Verify first entry
    AddMovementWithDateAmount firstEntry = testData.get(0);
    assertEquals(LocalDate.of(2024, 7, 30), firstEntry.entryDate());
    assertEquals(0, new BigDecimal("12591000.00").compareTo(firstEntry.totalAmount()));
    assertEquals(0, new BigDecimal("12689712.00").compareTo(firstEntry.balanceSnapshot()));

    // Verify last entry (account reaches zero)
    AddMovementWithDateAmount lastEntry = testData.get(9);
    assertEquals(LocalDate.of(2025, 3, 31), lastEntry.entryDate());
    assertEquals(0, new BigDecimal("-3768488.00").compareTo(lastEntry.totalAmount()));
    assertEquals(0, BigDecimal.ZERO.compareTo(lastEntry.balanceSnapshot()));

    // Verify some negative amounts
    AddMovementWithDateAmount novemberEntry = testData.get(4); // 30/11/2024
    assertTrue(novemberEntry.totalAmount().compareTo(BigDecimal.ZERO) < 0);
    assertEquals(0, new BigDecimal("-673605.00").compareTo(novemberEntry.totalAmount()));
  }

  @Test
  @DisplayName("Should create extended account movement test data with correct values")
  void shouldCreateExtendedAccountMovementTestData() {
    // Given
    List<AddMovementWithDateAmount> testData = TestDataFactory.createExtendedAccountMovementTestData();

    // Then
    assertEquals(25, testData.size());

    // Verify first entry (October 2023)
    AddMovementWithDateAmount firstEntry = testData.get(0);
    assertEquals(LocalDate.of(2023, 10, 31), firstEntry.entryDate());
    assertEquals(0, new BigDecimal("13010000.00").compareTo(firstEntry.totalAmount()));
    assertEquals(0, new BigDecimal("13062118.00").compareTo(firstEntry.balanceSnapshot()));

    // Verify large deposit in May 2024
    AddMovementWithDateAmount mayDeposit = testData.get(8); // 03/05/2024
    assertEquals(LocalDate.of(2024, 5, 3), mayDeposit.entryDate());
    assertEquals(0, new BigDecimal("24100000.00").compareTo(mayDeposit.totalAmount()));
    assertEquals(0, new BigDecimal("65394365.00").compareTo(mayDeposit.balanceSnapshot()));

    // Verify large withdrawal in July 2024
    AddMovementWithDateAmount julyWithdrawal = testData.get(13); // 31/07/2024
    assertEquals(LocalDate.of(2024, 7, 31), julyWithdrawal.entryDate());
    assertEquals(0, new BigDecimal("-20000000.00").compareTo(julyWithdrawal.totalAmount()));
    assertEquals(0, new BigDecimal("32809397.00").compareTo(julyWithdrawal.balanceSnapshot()));

    // Verify final entry (August 2025)
    AddMovementWithDateAmount lastEntry = testData.get(24);
    assertEquals(LocalDate.of(2025, 8, 30), lastEntry.entryDate());
    assertEquals(0, BigDecimal.ZERO.compareTo(lastEntry.totalAmount()));
    assertEquals(0, new BigDecimal("37074883.00").compareTo(lastEntry.balanceSnapshot()));
  }
}
