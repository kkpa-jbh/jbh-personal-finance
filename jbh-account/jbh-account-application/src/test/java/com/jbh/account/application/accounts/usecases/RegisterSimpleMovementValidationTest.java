package com.jbh.account.application.accounts.usecases;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jbh.account.application.accounts.dto.AddBasicMovementDTO;
import com.jbh.account.application.accounts.ports.input.AddBasicMovementInputPort;
import com.jbh.account.application.accounts.ports.output.AccountMonthlyBalanceRepository;
import com.jbh.account.application.accounts.ports.output.AccountRepository;
import com.jbh.account.application.accounts.services.MonthlyBalanceSyncerAppService;
import com.jbh.account.application.accounts.usecases.utils.UnitOfWorkTest;
import com.jbh.account.application.accounts.vo.AddBasicMovementRequest;
import com.jbh.account.application.acid.UnitOfWork;
import com.jbh.account.application.async.AsyncTaskExecutorImpl;
import com.jbh.account.application.movements.ports.output.AccountMovementRepository;
import com.jbh.account.domain.entity.AccountDomain;
import com.jbh.account.domain.exceptions.GenericSpecificationException;
import com.jbh.account.domain.utils.MoneyUtils;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountMovementDTO;
import com.jbh.account.domain.vo.MovementType;
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
  @Mock private AccountRepository accountRepository;
  @Mock private AccountMovementRepository accountMovementRepository;
  @Mock private AccountMonthlyBalanceRepository accountMonthlyBalanceRepository;
  private AddBasicMovementInputPort registerSimpleMovementInputPort;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    final MonthlyBalanceSyncerAppService monthlyBalanceSyncerService =
        new MonthlyBalanceSyncerAppService(
            accountMonthlyBalanceRepository, new AsyncTaskExecutorImpl());
    registerSimpleMovementInputPort =
        new AddBasicMovementInputPort(
            accountRepository, accountMovementRepository, unitOfWork, monthlyBalanceSyncerService);
  }

  @Test
  void shouldThrowException_WhenUserIdIsNull() {
    // Given
    final UUID userId = null;
    final AccountId accountId = AccountId.generate();
    final LocalDate movementDate = LocalDate.now();
    final BigDecimal amount = new BigDecimal("100.00");

    final AddBasicMovementRequest request = new AddBasicMovementRequest(movementDate, amount);

    // When & Then
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> registerSimpleMovementInputPort.addBasicMovements(userId, accountId, request));

    assertEquals("User ID cannot be null", exception.getMessage());
  }

  @Test
  void shouldThrowException_WhenmovemenDateIsNull() {
    // Given
    final UUID userId = UUID.randomUUID();
    final AccountId accountId = AccountId.generate();
    final BigDecimal amount = new BigDecimal("100.00");
    final AddBasicMovementRequest request = new AddBasicMovementRequest(null, amount);

    // When & Then
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> registerSimpleMovementInputPort.addBasicMovements(userId, accountId, request));

    assertEquals("Entry date cannot be null", exception.getMessage());
  }

  @Test
  void shouldThrowException_WhenAmountIsNull() {
    // Given
    final UUID userId = UUID.randomUUID();
    final AccountId accountId = AccountId.generate();
    final LocalDate movementDate = LocalDate.now();

    final AddBasicMovementRequest request = new AddBasicMovementRequest(movementDate, null);

    // When & Then
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> registerSimpleMovementInputPort.addBasicMovements(userId, accountId, request));

    assertEquals("There is not any amount to add", exception.getMessage());
  }

  @Test
  void shouldThrowException_WhenAccountNotFound() {
    // Given
    final UUID userId = UUID.randomUUID();
    final AccountId accountId = AccountId.generate();
    final LocalDate movementDate = LocalDate.now();
    final BigDecimal amount = new BigDecimal("100.00");

    final AddBasicMovementRequest request = new AddBasicMovementRequest(movementDate, amount);

    when(accountRepository.findByAccountId(userId, accountId)).thenReturn(Optional.empty());

    // When & Then
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> registerSimpleMovementInputPort.addBasicMovements(userId, accountId, request));

    assertEquals("Account not found", exception.getMessage());
    verify(accountRepository).findByAccountId(userId, accountId);
  }

  @Test
  void shouldHandleZeroAmount() {
    // Given
    final UUID userId = UUID.randomUUID();
    final AccountId accountId = AccountId.generate();
    final LocalDate movementDate = LocalDate.now();
    final BigDecimal amount = BigDecimal.ZERO;

    final AddBasicMovementRequest request = new AddBasicMovementRequest(movementDate, amount);
    final AccountDomain accountDomain = AccountDomain.withId(accountId);

    when(accountRepository.findByAccountId(userId, accountId))
        .thenReturn(Optional.of(accountDomain.toDTO()));

    // When & Then
    assertDoesNotThrow(
        () -> registerSimpleMovementInputPort.addBasicMovements(userId, accountId, request));
    verify(accountRepository).findByAccountId(userId, accountId);
    verify(accountMovementRepository).save((AccountMovementDTO) any());
    verify(accountRepository).save(any());
    assertEquals(MoneyUtils.withJBHDecimals(amount), accountDomain.getMovementBalance());
  }

  @Test
  void shouldHandleNegativeAmount() {
    // Given
    final UUID userId = UUID.randomUUID();
    final AccountId accountId = AccountId.generate();
    final LocalDate movementDate = LocalDate.now();
    final BigDecimal amount = new BigDecimal("-50.00");

    final AddBasicMovementRequest request = new AddBasicMovementRequest(movementDate, amount);

    final AccountDomain accountDomain =
        AccountDomain.with(accountId, new BigDecimal("170.00"), new BigDecimal("180.00"));

    when(accountRepository.findByAccountId(userId, accountId))
        .thenReturn(Optional.of(accountDomain.toDTO()));

    // When & Then
    final AtomicReference<AddBasicMovementDTO> mvmtResponse = new AtomicReference<>();
    assertDoesNotThrow(
        () ->
            mvmtResponse.set(
                registerSimpleMovementInputPort.addBasicMovements(userId, accountId, request)));

    verify(accountRepository).findByAccountId(userId, accountId);
    verify(accountMovementRepository).save((AccountMovementDTO) any());
    verify(accountRepository).save(any());

    assertEquals(new BigDecimal("120.00"), mvmtResponse.get().account().getMovementBalance());
    assertEquals(new BigDecimal("130.00"), mvmtResponse.get().account().getCurrentBalance());

    assertEquals(amount.abs(), mvmtResponse.get().monthlyBalance().getTotalCredits());
    assertEquals(amount, mvmtResponse.get().monthlyBalance().getClosingBalance());
    assertEquals(1, mvmtResponse.get().monthlyBalance().getTotalMovements());
    assertEquals(MoneyUtils.JBH_ZERO, mvmtResponse.get().monthlyBalance().getTotalDebits());
    assertEquals(MovementType.WITHDRAWAL, mvmtResponse.get().movement().getMovementType());
  }

  @Test
  void shouldHandlePastDate() {
    // Given
    final UUID userId = UUID.randomUUID();
    final AccountId accountId = AccountId.generate();
    final LocalDate movementDate = LocalDate.now().minusDays(30);
    final BigDecimal amount = new BigDecimal("100.00");

    final AddBasicMovementRequest request = new AddBasicMovementRequest(movementDate, amount);
    final AccountDomain accountDomain = AccountDomain.withId(accountId);

    when(accountRepository.findByAccountId(userId, accountId))
        .thenReturn(Optional.of(accountDomain.toDTO()));

    // When & Then
    assertDoesNotThrow(
        () -> registerSimpleMovementInputPort.addBasicMovements(userId, accountId, request));

    verify(accountRepository).findByAccountId(userId, accountId);
    verify(accountMovementRepository).save((AccountMovementDTO) any());
    verify(accountRepository).save(any());
  }

  @Test
  void shouldNotHandleFutureDate() {
    // Given
    final UUID userId = UUID.randomUUID();
    final AccountId accountId = AccountId.generate();
    final LocalDate movementDate = LocalDate.now().plusDays(30);
    final BigDecimal amount = new BigDecimal("100.00");

    final AddBasicMovementRequest request = new AddBasicMovementRequest(movementDate, amount);
    final AccountDomain accountDomain = AccountDomain.withId(accountId);

    when(accountRepository.findByAccountId(userId, accountId))
        .thenReturn(Optional.of(accountDomain.toDTO()));

    // When & Then
    assertThrows(
        GenericSpecificationException.class,
        () -> registerSimpleMovementInputPort.addBasicMovements(userId, accountId, request));

    verify(accountRepository).findByAccountId(userId, accountId);
    verify(accountMovementRepository, never()).save((AccountMovementDTO) any());
    verify(accountRepository, never()).save(accountDomain.toDTO());
  }

  @Test
  void shouldThrowErrorWithInsufficientEffectiveBalance() {
    // Given
    final UUID userId = UUID.randomUUID();
    final AccountId accountId = AccountId.generate();
    final LocalDate movementDate = LocalDate.now();
    final BigDecimal amount = new BigDecimal("-50.00");

    final AddBasicMovementRequest request = new AddBasicMovementRequest(movementDate, amount);
    final AccountDomain accountDomain =
        AccountDomain.with(accountId, new BigDecimal("30.00"), new BigDecimal("30.00"));

    when(accountRepository.findByAccountId(userId, accountId))
        .thenReturn(Optional.of(accountDomain.toDTO()));

    // When & Then
    final GenericSpecificationException exception =
        assertThrows(
            GenericSpecificationException.class,
            () -> registerSimpleMovementInputPort.addBasicMovements(userId, accountId, request));

    assertEquals("Insufficient effective balance", exception.getMessage());

    verify(accountRepository).findByAccountId(userId, accountId);
    verify(accountMovementRepository, never()).save((AccountMovementDTO) any());
    verify(accountRepository, never()).save(accountDomain.toDTO());
  }
}
