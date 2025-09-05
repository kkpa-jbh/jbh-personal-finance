package com.jbh.account.application.accounts.usecases;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jbh.account.application.accounts.ports.input.RegisterSimpleMovementInputPort;
import com.jbh.account.application.accounts.ports.output.AccountMonthlyBalanceRepository;
import com.jbh.account.application.accounts.ports.output.AccountRepository;
import com.jbh.account.application.accounts.usecases.utils.UnitOfWorkTest;
import com.jbh.account.application.accounts.vo.AddBasicMovementRequest;
import com.jbh.account.application.accounts.vo.AddBasicMovementResponse;
import com.jbh.account.application.acid.UnitOfWork;
import com.jbh.account.application.movements.ports.output.AccountMovementRepository;
import com.jbh.accounts_mgmt.accounts.AccountDomain;
import com.jbh.accounts_mgmt.accounts.AccountId;
import com.jbh.accounts_mgmt.exceptions.GenericSpecificationException;
import com.jbh.accounts_mgmt.movements.MovementType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class RegisterSimpleMovementValidationTest {

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

    AddBasicMovementRequest request = new AddBasicMovementRequest(movementDate, amount);

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
    AddBasicMovementRequest request = new AddBasicMovementRequest(null, amount);

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

    AddBasicMovementRequest request = new AddBasicMovementRequest(movementDate, null);

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

    AddBasicMovementRequest request = new AddBasicMovementRequest(movementDate, amount);

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

    AddBasicMovementRequest request = new AddBasicMovementRequest(movementDate, amount);
    AccountDomain accountDomain = AccountDomain.withId(accountId);

    when(accountRepository.findByAccountId(userId, accountId))
        .thenReturn(Optional.of(accountDomain));

    // When & Then
    assertDoesNotThrow(() -> registerSimpleMovementInputPort.addSimpleMovement(userId, accountId, request));
    verify(accountRepository).findByAccountId(userId, accountId);
    verify(accountMovementRepository).save(anyList());
    verify(accountRepository).save(accountDomain);
    assertEquals(accountDomain.getMovementBalance(), amount);
  }

  @Test
  void shouldHandleNegativeAmount() {
    // Given
    UUID userId = UUID.randomUUID();
    AccountId accountId = AccountId.generate();
    LocalDate movementDate = LocalDate.now();
    BigDecimal amount = new BigDecimal("-50.00");

    AddBasicMovementRequest request = new AddBasicMovementRequest(movementDate, amount);
    AccountDomain accountDomain = AccountDomain.withId(accountId);
    accountDomain.setMovementBalance(new BigDecimal("170.00"));
    accountDomain.setCurrentBalance(new BigDecimal("180.00"));

    when(accountRepository.findByAccountId(userId, accountId))
        .thenReturn(Optional.of(accountDomain));

    // When & Then
    AtomicReference<AddBasicMovementResponse> mvmtResponse = new AtomicReference<>();
    assertDoesNotThrow(
        () -> mvmtResponse.set(registerSimpleMovementInputPort.addSimpleMovement(userId, accountId, request)));

    verify(accountRepository).findByAccountId(userId, accountId);
    verify(accountMovementRepository).save(anyList());
    verify(accountRepository).save(accountDomain);

    assertEquals(new BigDecimal("120.00"), mvmtResponse.get().account().getMovementBalance());
    assertEquals(new BigDecimal("130.00"), mvmtResponse.get().account().getCurrentBalance());

    assertEquals(amount.abs(), mvmtResponse.get().monthlyBalance().getTotalCredits());
    assertEquals(amount, mvmtResponse.get().monthlyBalance().getClosingBalance());
    assertEquals(1, mvmtResponse.get().monthlyBalance().getTotalMovements());
    assertEquals(BigDecimal.ZERO, mvmtResponse.get().monthlyBalance().getTotalDebits());
    assertEquals(MovementType.WITHDRAWAL, mvmtResponse.get().movement().getMovementType());
  }

  @Test
  void shouldHandlePastDate() {
    // Given
    UUID userId = UUID.randomUUID();
    AccountId accountId = AccountId.generate();
    LocalDate movementDate = LocalDate.now().minusDays(30);
    BigDecimal amount = new BigDecimal("100.00");

    AddBasicMovementRequest request = new AddBasicMovementRequest(movementDate, amount);
    AccountDomain accountDomain = AccountDomain.withId(accountId);

    when(accountRepository.findByAccountId(userId, accountId))
        .thenReturn(Optional.of(accountDomain));

    // When & Then
    assertDoesNotThrow(() -> registerSimpleMovementInputPort.addSimpleMovement(userId, accountId, request));

    verify(accountRepository).findByAccountId(userId, accountId);
    verify(accountMovementRepository).save(anyList());
    verify(accountRepository).save(accountDomain);
  }

  @Test
  void shouldNotHandleFutureDate() {
    // Given
    UUID userId = UUID.randomUUID();
    AccountId accountId = AccountId.generate();
    LocalDate movementDate = LocalDate.now().plusDays(30);
    BigDecimal amount = new BigDecimal("100.00");

    AddBasicMovementRequest request = new AddBasicMovementRequest(movementDate, amount);
    AccountDomain accountDomain = AccountDomain.withId(accountId);

    when(accountRepository.findByAccountId(userId, accountId))
        .thenReturn(Optional.of(accountDomain));

    // When & Then
    assertThrows(GenericSpecificationException.class,
        () -> registerSimpleMovementInputPort.addSimpleMovement(userId, accountId, request));

    verify(accountRepository).findByAccountId(userId, accountId);
    verify(accountMovementRepository, never()).save(anyList());
    verify(accountRepository, never()).save(accountDomain);
  }

  @Test
  void shouldThrowErrorWithInsufficientEffectiveBalance() {
    // Given
    UUID userId = UUID.randomUUID();
    AccountId accountId = AccountId.generate();
    LocalDate movementDate = LocalDate.now();
    BigDecimal amount = new BigDecimal("-50.00");

    AddBasicMovementRequest request = new AddBasicMovementRequest(movementDate, amount);
    AccountDomain accountDomain = AccountDomain.withId(accountId);
    accountDomain.setMovementBalance(new BigDecimal("30.00"));
    accountDomain.setCurrentBalance(new BigDecimal("30.00"));

    when(accountRepository.findByAccountId(userId, accountId))
        .thenReturn(Optional.of(accountDomain));

    // When & Then
    GenericSpecificationException exception = assertThrows(GenericSpecificationException.class,
        () -> registerSimpleMovementInputPort.addSimpleMovement(userId, accountId, request));

    assertEquals("Insufficient effective balance", exception.getMessage());

    verify(accountRepository).findByAccountId(userId, accountId);
    verify(accountMovementRepository, never()).save(anyList());
    verify(accountRepository, never()).save(accountDomain);
  }

}
