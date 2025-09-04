package usecases;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jbh.account_app.accounts.ports.input.AddTransactionInputPort;
import com.jbh.account_app.accounts.ports.output.AccountMonthlyBalanceRepository;
import com.jbh.account_app.accounts.ports.output.AccountRepository;
import com.jbh.account_app.accounts.vo.AddTransactionWithDateAmount;
import com.jbh.account_app.acid.UnitOfWork;
import com.jbh.account_app.transactions.ports.output.TransactionRepository;
import com.jbh.accounts_mgmt.accounts.domain.AccountDomain;
import com.jbh.accounts_mgmt.accounts.domain.AccountId;
import com.jbh.accounts_mgmt.accounts.domain.AccountMonthlyBalanceDomain;
import com.jbh.accounts_mgmt.exceptions.GenericSpecificationException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import utils.UnitOfWorkTest;

public class AddTransactionUseCaseTest {

  private final UnitOfWork unitOfWork = new UnitOfWorkTest();
  @Mock
  private AccountRepository accountRepository;
  @Mock
  private TransactionRepository transactionRepository;
  @Mock
  private AccountMonthlyBalanceRepository accountMonthlyBalanceRepository;
  private AddTransactionInputPort addTransactionUseCase;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    addTransactionUseCase = new AddTransactionInputPort(accountRepository, transactionRepository, unitOfWork,
        accountMonthlyBalanceRepository);
  }


  @Test
  void shouldThrowException_WhenUserIdIsNull() {
    // Given
    AccountId accountId = AccountId.generate();
    LocalDate txnDate = LocalDate.now();
    BigDecimal amount = new BigDecimal("100.00");

    AddTransactionWithDateAmount request = new AddTransactionWithDateAmount(null, txnDate, amount);

    // When & Then
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> addTransactionUseCase.addTransaction(accountId, request));

    assertEquals("User ID cannot be null", exception.getMessage());
  }

  @Test
  void shouldThrowException_WhenTransactionDateIsNull() {
    // Given
    UUID userId = UUID.randomUUID();
    AccountId accountId = AccountId.generate();
    BigDecimal amount = new BigDecimal("100.00");

    AddTransactionWithDateAmount request = new AddTransactionWithDateAmount(userId, null, amount);

    // When & Then
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> addTransactionUseCase.addTransaction(accountId, request));

    assertEquals("Transaction date cannot be null", exception.getMessage());
  }

  @Test
  void shouldThrowException_WhenAmountIsNull() {
    // Given
    UUID userId = UUID.randomUUID();
    AccountId accountId = AccountId.generate();
    LocalDate txnDate = LocalDate.now();

    AddTransactionWithDateAmount request = new AddTransactionWithDateAmount(userId, txnDate, null);

    // When & Then
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> addTransactionUseCase.addTransaction(accountId, request));

    assertEquals("Total amount cannot be null", exception.getMessage());
  }

  @Test
  void shouldThrowException_WhenAccountNotFound() {
    // Given
    UUID userId = UUID.randomUUID();
    AccountId accountId = AccountId.generate();
    LocalDate txnDate = LocalDate.now();
    BigDecimal amount = new BigDecimal("100.00");

    AddTransactionWithDateAmount request = new AddTransactionWithDateAmount(userId, txnDate, amount);

    when(accountRepository.findByAccountId(userId, accountId))
        .thenReturn(Optional.empty());

    // When & Then
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> addTransactionUseCase.addTransaction(accountId, request));

    assertEquals("Account not found", exception.getMessage());
    verify(accountRepository).findByAccountId(userId, accountId);
  }

  @Test
  void shouldHandleZeroAmount() {
    // Given
    UUID userId = UUID.randomUUID();
    AccountId accountId = AccountId.generate();
    LocalDate txnDate = LocalDate.now();
    BigDecimal amount = BigDecimal.ZERO;

    AddTransactionWithDateAmount request = new AddTransactionWithDateAmount(userId, txnDate, amount);
    AccountDomain accountDomain = AccountDomain.withId(accountId);

    when(accountRepository.findByAccountId(userId, accountId))
        .thenReturn(Optional.of(accountDomain));

    // When & Then
    assertDoesNotThrow(() -> addTransactionUseCase.addTransaction(accountId, request));
    verify(accountRepository).findByAccountId(userId, accountId);
    verify(transactionRepository).save(any());
    verify(accountRepository).save(accountDomain);
    assertEquals(accountDomain.getBalance(), amount);
  }

  @Test
  void shouldHandleNegativeAmount() {
    // Given
    UUID userId = UUID.randomUUID();
    AccountId accountId = AccountId.generate();
    LocalDate txnDate = LocalDate.now();
    BigDecimal amount = new BigDecimal("-50.00");

    AddTransactionWithDateAmount request = new AddTransactionWithDateAmount(userId, txnDate, amount);
    AccountDomain accountDomain = AccountDomain.withId(accountId);
    accountDomain.setBalance(new BigDecimal("170.00"));
    accountDomain.setEffectiveBalance(new BigDecimal("170.00"));

    when(accountRepository.findByAccountId(userId, accountId))
        .thenReturn(Optional.of(accountDomain));

    // When & Then
    assertDoesNotThrow(() -> addTransactionUseCase.addTransaction(accountId, request));

    verify(accountRepository).findByAccountId(userId, accountId);
    verify(transactionRepository).save(any());
    verify(accountRepository).save(accountDomain);
    assertEquals(new BigDecimal("120.00"), accountDomain.getBalance());
  }

  @Test
  void shouldHandlePastDate() {
    // Given
    UUID userId = UUID.randomUUID();
    AccountId accountId = AccountId.generate();
    LocalDate txnDate = LocalDate.now().minusDays(30);
    BigDecimal amount = new BigDecimal("100.00");

    AddTransactionWithDateAmount request = new AddTransactionWithDateAmount(userId, txnDate, amount);
    AccountDomain accountDomain = AccountDomain.withId(accountId);

    when(accountRepository.findByAccountId(userId, accountId))
        .thenReturn(Optional.of(accountDomain));

    // When & Then
    assertDoesNotThrow(() -> addTransactionUseCase.addTransaction(accountId, request));

    verify(accountRepository).findByAccountId(userId, accountId);
    verify(transactionRepository).save(any());
    verify(accountRepository).save(accountDomain);
  }

  @Test
  void shouldNotHandleFutureDate() {
    // Given
    UUID userId = UUID.randomUUID();
    AccountId accountId = AccountId.generate();
    LocalDate txnDate = LocalDate.now().plusDays(30);
    BigDecimal amount = new BigDecimal("100.00");

    AddTransactionWithDateAmount request = new AddTransactionWithDateAmount(userId, txnDate, amount);
    AccountDomain accountDomain = AccountDomain.withId(accountId);

    when(accountRepository.findByAccountId(userId, accountId))
        .thenReturn(Optional.of(accountDomain));

    // When & Then
    assertThrows(GenericSpecificationException.class, () -> addTransactionUseCase.addTransaction(accountId, request));

    verify(accountRepository).findByAccountId(userId, accountId);
    verify(transactionRepository, never()).save(any());
    verify(accountRepository, never()).save(accountDomain);
  }

  @Test
  void shouldThrowErrorWithInsufficientEffectiveBalance() {
    // Given
    UUID userId = UUID.randomUUID();
    AccountId accountId = AccountId.generate();
    LocalDate txnDate = LocalDate.now();
    BigDecimal amount = new BigDecimal("-50.00");

    AddTransactionWithDateAmount request = new AddTransactionWithDateAmount(userId, txnDate, amount);
    AccountDomain accountDomain = AccountDomain.withId(accountId);
    accountDomain.setBalance(new BigDecimal("30.00"));
    accountDomain.setEffectiveBalance(new BigDecimal("30.00"));

    when(accountRepository.findByAccountId(userId, accountId))
        .thenReturn(Optional.of(accountDomain));

    // When & Then
    GenericSpecificationException exception = assertThrows(GenericSpecificationException.class,
        () -> addTransactionUseCase.addTransaction(accountId, request));

    assertEquals("Insufficient effective balance", exception.getMessage());

    verify(accountRepository).findByAccountId(userId, accountId);
    verify(transactionRepository, never()).save(any());
    verify(accountRepository, never()).save(accountDomain);
  }

  @Test
  void shouldAddTransactionSuccessfully_WhenValidInputProvided() {
    // Given
    UUID userId = UUID.randomUUID();
    AccountId accountId = AccountId.generate();
    LocalDate txnDate = LocalDate.now();
    BigDecimal amount = new BigDecimal("100.00");

    AddTransactionWithDateAmount request = new AddTransactionWithDateAmount(userId, txnDate, amount);
    AccountDomain accountDomain = AccountDomain.withId(accountId);

    when(accountRepository.findByAccountId(userId, accountId))
        .thenReturn(Optional.of(accountDomain));

    // When & Then
    assertDoesNotThrow(() -> addTransactionUseCase.addTransaction(accountId, request));

    verify(accountRepository).findByAccountId(userId, accountId);
    verify(transactionRepository).save(any());
    verify(accountRepository).save(accountDomain);

    assertEquals(accountDomain.getBalance(), amount);
  }

  @Test
  void shouldAddEntryWithExistingMonthlyEntries() {
    // Given
    UUID userId = UUID.randomUUID();
    AccountId accountId = AccountId.generate();
    LocalDate txnDate = LocalDate.now();
    BigDecimal amount = new BigDecimal("100.00");

    AddTransactionWithDateAmount request = new AddTransactionWithDateAmount(userId, txnDate, amount);
    AccountDomain accountDomain = AccountDomain.withId(accountId);

    when(accountRepository.findByAccountId(userId, accountId))
        .thenReturn(Optional.of(accountDomain));

    int existingEntries = 10;
    BigDecimal existingTotalDebits = new BigDecimal("1000.00");
    BigDecimal existingClosingBalance = new BigDecimal("600.00");
    AccountMonthlyBalanceDomain existingBalance = AccountMonthlyBalanceDomain.builder()
        .id(1L)
        .accountId(accountId)
        .balanceYear(txnDate.getYear())
        .balanceMonth(txnDate.getMonthValue())
        .openingBalance(new BigDecimal("500.00"))
        .closingBalance(existingClosingBalance)
        .totalCredits(new BigDecimal("200.00"))
        .totalDebits(existingTotalDebits)
        .transactionCount(existingEntries)
        .build();

    when(accountMonthlyBalanceRepository.findByAccountIdYearAndMonth(accountId, txnDate.getYear(),
        txnDate.getMonthValue()))
        .thenReturn(Optional.of(existingBalance));

    // When & Then
    assertDoesNotThrow(() -> addTransactionUseCase.addTransaction(accountId, request));

    verify(accountRepository).findByAccountId(userId, accountId);
    verify(transactionRepository).save(any());
    verify(accountRepository).save(accountDomain);
    verify(accountMonthlyBalanceRepository, atMostOnce()).save(any());

    assertEquals(accountDomain.getBalance(), amount);
    assertEquals(existingEntries + 1, existingBalance.getTransactionCount());
  }
}
