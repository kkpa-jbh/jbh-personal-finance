package com.jbh.account.application.core.usecases.mock;

import static com.jbh.account.application.core.usecases.mock.RegisterMovementExecutionMockTest.OTHER_INCOME_CATEGORY;
import static com.jbh.account.domain.utils.MoneyUtils.JBH_ZERO;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jbh.account.application.acid.UnitOfWork;
import com.jbh.account.application.async.AsyncTaskExecutorImpl;
import com.jbh.account.application.core.dto.MovementDTO;
import com.jbh.account.application.core.mappers.AccountMapper;
import com.jbh.account.application.core.ports.input.AddMovementInputPort;
import com.jbh.account.application.core.ports.output.AccountRepository;
import com.jbh.account.application.core.ports.output.monthlybalance.AccountMonthlyBalanceQueryRepo;
import com.jbh.account.application.core.ports.output.monthlybalance.AccountMonthlyBalanceWriterRepository;
import com.jbh.account.application.core.services.MonthlyBalanceSyncForUploadedMovements;
import com.jbh.account.application.core.services.account.AccountService;
import com.jbh.account.application.core.services.account.AccountServiceImpl;
import com.jbh.account.application.core.services.monthlybalance.MonthlyBalanceService;
import com.jbh.account.application.core.services.monthlybalance.MonthlyBalanceServiceImpl;
import com.jbh.account.application.core.services.movements.AccountMovementService;
import com.jbh.account.application.core.services.movements.AccountMovementServiceImpl;
import com.jbh.account.application.core.usecases.utils.UnitOfWorkTest;
import com.jbh.account.application.core.vo.commands.AddMovementCommand;
import com.jbh.account.application.movements.ports.output.AccountMovementRepository;
import com.jbh.account.domain.entity.AccountDomain;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.utils.MoneyUtils;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.ExpenseCategory;
import com.jbh.account.domain.vo.IncomeCategory;
import com.jbh.account.domain.vo.MovementCategoryDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class RegisterMovementValidationMockTest {

  public static final MovementCategoryDTO PERSONAL_EXPENSE =
      MovementCategoryDTO.withType(ExpenseCategory.PERSONAL);
  private final UnitOfWork unitOfWork = new UnitOfWorkTest();
  MonthlyBalanceService monthlyBalanceService;
  @Mock private AccountRepository accountRepository;
  @Mock private AccountMovementRepository accountMovementRepository;
  @Mock private AccountMonthlyBalanceQueryRepo accountMonthlyBalanceRepository;
  @Mock private AccountMonthlyBalanceWriterRepository monthlyBalanceWriterRepoMock;
  private AddMovementInputPort registerSimpleMovementInputPort;
  private AccountMovementService accountMovementService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    monthlyBalanceService =
        new MonthlyBalanceServiceImpl(
            accountMonthlyBalanceRepository,
            monthlyBalanceWriterRepoMock,
            new AsyncTaskExecutorImpl());

    final MonthlyBalanceSyncForUploadedMovements monthlyBalanceSyncerService =
        new MonthlyBalanceSyncForUploadedMovements(monthlyBalanceService);

    final AccountService accountService = new AccountServiceImpl(accountRepository);

    accountMovementService =
        new AccountMovementServiceImpl(
            accountMovementRepository, accountService, monthlyBalanceService, new UnitOfWorkTest());

    registerSimpleMovementInputPort = new AddMovementInputPort(accountMovementService);
  }

  @Test
  void shouldThrowException_WhenUserIdIsNull() {
    // Given
    final UUID userId = null;
    final AccountId accountId = AccountId.generate();
    final LocalDate movementDate = LocalDate.now();
    final BigDecimal amount = new BigDecimal("100.00");

    final AddMovementCommand request =
        new AddMovementCommand(movementDate, amount, PERSONAL_EXPENSE);

    // When & Then
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> registerSimpleMovementInputPort.addMovement(userId, accountId, request));

    assertEquals("User ID cannot be null", exception.getMessage());
  }

  @Test
  void shouldThrowException_WhenmovemenDateIsNull() {
    // Given
    final UUID userId = UUID.randomUUID();
    final AccountId accountId = AccountId.generate();
    final BigDecimal amount = new BigDecimal("100.00");
    final AddMovementCommand request = new AddMovementCommand(null, amount, PERSONAL_EXPENSE);

    // When & Then
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> registerSimpleMovementInputPort.addMovement(userId, accountId, request));
  }

  @Test
  void shouldThrowException_WhenAmountIsNegative() {
    // Given
    final UUID userId = UUID.randomUUID();
    final AccountId accountId = AccountId.generate();
    final BigDecimal amount = new BigDecimal("-100.00");
    final AddMovementCommand request =
        new AddMovementCommand(
            LocalDate.now(), amount, MovementCategoryDTO.withType(ExpenseCategory.PERSONAL));

    // When & Then
    assertThrows(
        IllegalArgumentException.class,
        () -> registerSimpleMovementInputPort.addMovement(userId, accountId, request));
  }

  @Test
  void shouldThrowException_WhenAmountIsNull() {
    // Given
    final UUID userId = UUID.randomUUID();
    final AccountId accountId = AccountId.generate();
    final LocalDate movementDate = LocalDate.now();

    final AddMovementCommand request =
        new AddMovementCommand(movementDate, null, OTHER_INCOME_CATEGORY);

    // When & Then
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> registerSimpleMovementInputPort.addMovement(userId, accountId, request));

    assertEquals("There is not any amount to add", exception.getMessage());
  }

  @Test
  void shouldThrowException_WhenAccountNotFound() {
    // Given
    final UUID userId = UUID.randomUUID();
    final AccountId accountId = AccountId.generate();
    final LocalDate movementDate = LocalDate.now();
    final BigDecimal amount = new BigDecimal("100.00");

    final AddMovementCommand request =
        new AddMovementCommand(movementDate, amount, OTHER_INCOME_CATEGORY);

    when(accountRepository.findByUserAndAccountId(userId, accountId)).thenReturn(Optional.empty());

    // When & Then
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> registerSimpleMovementInputPort.addMovement(userId, accountId, request));

    assertEquals("Account not found", exception.getMessage());
    verify(accountRepository).findByUserAndAccountId(userId, accountId);
  }

  @Test
  void shouldHandleZeroAmount() {
    // Given
    final UUID userId = UUID.randomUUID();
    final AccountId accountId = AccountId.generate();
    final LocalDate movementDate = LocalDate.now();
    final BigDecimal amount = BigDecimal.ZERO;

    final AddMovementCommand request =
        new AddMovementCommand(
            movementDate, amount, MovementCategoryDTO.withType(IncomeCategory.OTHER));
    final AccountDomain accountDomain =
        AccountDomain.withBasicMovementForExisting(accountId, userId, JBH_ZERO, JBH_ZERO);

    when(accountRepository.findByUserAndAccountId(userId, accountId))
        .thenReturn(Optional.of(AccountMapper.toDTO(accountDomain)));

    // When & Then
    assertDoesNotThrow(
        () -> registerSimpleMovementInputPort.addMovement(userId, accountId, request));
    verify(accountRepository).findByUserAndAccountId(userId, accountId);
    verify(accountMovementRepository).save((MovementDTO) any());
    verify(accountRepository).save(any());
    assertEquals(MoneyUtils.withJBHDecimals(amount), accountDomain.getMovementBalance());
  }

  @Test
  void shouldHandlePastDate() {
    // Given
    final UUID userId = UUID.randomUUID();
    final AccountId accountId = AccountId.generate();
    final LocalDate movementDate = LocalDate.now().minusDays(30);
    final BigDecimal amount = new BigDecimal("100.00");

    final AddMovementCommand request =
        new AddMovementCommand(movementDate, amount, OTHER_INCOME_CATEGORY);
    final AccountDomain accountDomain =
        AccountDomain.withBasicMovementForExisting(accountId, userId, JBH_ZERO, JBH_ZERO);

    when(accountRepository.findByUserAndAccountId(userId, accountId))
        .thenReturn(Optional.of(AccountMapper.toDTO(accountDomain)));

    // When & Then
    assertDoesNotThrow(
        () -> registerSimpleMovementInputPort.addMovement(userId, accountId, request));

    verify(accountRepository).findByUserAndAccountId(userId, accountId);
    verify(accountMovementRepository).save((MovementDTO) any());
    verify(accountRepository).save(any());
  }

  @Test
  void shouldNotHandleFutureDate() {
    // Given
    final UUID userId = UUID.randomUUID();
    final AccountId accountId = AccountId.generate();
    final LocalDate movementDate = LocalDate.now().plusDays(30);
    final BigDecimal amount = new BigDecimal("100.00");

    final AddMovementCommand request =
        new AddMovementCommand(movementDate, amount, PERSONAL_EXPENSE);
    final AccountDomain accountDomain =
        AccountDomain.withBasicMovementForExisting(accountId, userId, JBH_ZERO, JBH_ZERO);

    when(accountRepository.findByUserAndAccountId(userId, accountId))
        .thenReturn(Optional.of(AccountMapper.toDTO(accountDomain)));

    // When & Then
    assertThrows(
        AccountBusinessException.class,
        () -> registerSimpleMovementInputPort.addMovement(userId, accountId, request));

    verify(accountMovementRepository, never()).save((MovementDTO) any());
    verify(accountRepository, never()).save(AccountMapper.toDTO(accountDomain));
  }

  @Test
  void shouldThrowErrorWithInsufficientEffectiveBalance() {
    // Given
    final UUID userId = UUID.randomUUID();
    final AccountId accountId = AccountId.generate();
    final LocalDate movementDate = LocalDate.now();
    final BigDecimal amount = new BigDecimal("50.00");

    final AddMovementCommand request =
        new AddMovementCommand(movementDate, amount, PERSONAL_EXPENSE);
    final AccountDomain accountDomain =
        AccountDomain.withBasicMovementForExisting(
            accountId, userId, new BigDecimal("30.00"), new BigDecimal("30.00"));

    when(accountRepository.findByUserAndAccountId(userId, accountId))
        .thenReturn(Optional.of(AccountMapper.toDTO(accountDomain)));

    // When & Then
    assertThrows(
        AccountBusinessException.class,
        () -> registerSimpleMovementInputPort.addMovement(userId, accountId, request));

    verify(accountRepository).findByUserAndAccountId(userId, accountId);
    verify(accountMovementRepository, never()).save((MovementDTO) any());
    verify(accountRepository, never()).save(AccountMapper.toDTO(accountDomain));
  }
}
