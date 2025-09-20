package com.jbh.account.application.accounts.usecases.mock;

import static com.jbh.account.domain.utils.MoneyUtils.JBH_ZERO;
import static com.jbh.account.domain.vo.MovementType.BALANCE_SNAPSHOT;
import static com.jbh.account.domain.vo.MovementType.DEPOSIT;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jbh.account.application.accounts.dto.AccountDTO;
import com.jbh.account.application.accounts.dto.AccountMonthlyBalanceDTO;
import com.jbh.account.application.accounts.dto.AddBasicMovementDTO;
import com.jbh.account.application.accounts.dto.MovementDTO;
import com.jbh.account.application.accounts.mappers.AccountMapper;
import com.jbh.account.application.accounts.ports.input.AddMovementInputPort;
import com.jbh.account.application.accounts.ports.output.AccountRepository;
import com.jbh.account.application.accounts.ports.output.monthlybalance.AccountMonthlyBalanceQueryRepo;
import com.jbh.account.application.accounts.ports.output.monthlybalance.AccountMonthlyBalanceWriterRepository;
import com.jbh.account.application.accounts.services.MonthlyBalanceSyncerAppService;
import com.jbh.account.application.accounts.services.monthlybalance.MonthlyBalanceServiceImpl;
import com.jbh.account.application.accounts.usecases.utils.UnitOfWorkTest;
import com.jbh.account.application.accounts.vo.AddMovementCommand;
import com.jbh.account.application.acid.UnitOfWork;
import com.jbh.account.application.async.AsyncTaskExecutorImpl;
import com.jbh.account.application.movements.ports.output.AccountMovementRepository;
import com.jbh.account.domain.entity.AccountDomain;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.IncomeCategory;
import com.jbh.account.domain.vo.MovementCategoryDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegisterMovementExecutionMockTest {

  public static final MovementCategoryDTO OTHER_INCOME_CATEGORY =
      MovementCategoryDTO.withType(IncomeCategory.OTHER);
  private final UnitOfWork unitOfWork = new UnitOfWorkTest();
  private final Logger log = LoggerFactory.getLogger(RegisterMovementExecutionMockTest.class);
  MonthlyBalanceSyncerAppService monthlyBalanceSyncerService;
  LocalDate movementDate = LocalDate.now();
  @Mock private AccountRepository accountRepository;
  @Mock private AccountMovementRepository accountMovementRepository;
  @Mock private AccountMonthlyBalanceQueryRepo accountMonthlyBalanceRepository;
  @Mock private AccountMonthlyBalanceWriterRepository monthlyBalanceWriterRepoMock;
  private AddMovementInputPort useCaseInstanceTest;
  private MonthlyBalanceServiceImpl monthlyBalanceService;

  @BeforeEach
  void setUp() {

    MockitoAnnotations.openMocks(this);
    monthlyBalanceService =
        new MonthlyBalanceServiceImpl(
            accountMonthlyBalanceRepository, monthlyBalanceWriterRepoMock);
    monthlyBalanceSyncerService =
        new MonthlyBalanceSyncerAppService(monthlyBalanceService, new AsyncTaskExecutorImpl());
    useCaseInstanceTest =
        new AddMovementInputPort(
            accountRepository, accountMovementRepository, unitOfWork, monthlyBalanceSyncerService);
  }

  @Test
  void shouldAddMovement_WhenNotSnapshotProvided() throws ExecutionException, InterruptedException {
    // Given
    final UUID userId = UUID.randomUUID();
    final AccountId accountId = AccountId.generate();

    final BigDecimal amount = new BigDecimal("100.00");

    final AddMovementCommand request =
        new AddMovementCommand(movementDate, amount, DEPOSIT, OTHER_INCOME_CATEGORY);
    final AccountDomain accountDomain = withId(accountId);

    when(accountRepository.findByAccountId(userId, accountId))
        .thenReturn(Optional.of(AccountMapper.toDTO(accountDomain)));

    // When & Then
    final AtomicReference<AddBasicMovementDTO> mvmtResponse = new AtomicReference<>();
    assertDoesNotThrow(
        () -> mvmtResponse.set(useCaseInstanceTest.addMovement(userId, accountId, request)));

    verify(accountRepository).findByAccountId(userId, accountId);
    verify(accountMovementRepository).save((MovementDTO) any());
    verify(accountRepository).save(any());

    assertEquals(mvmtResponse.get().account().movementBalance(), amount);
    final AccountMonthlyBalanceDTO actualMonthlyBalances = mvmtResponse.get().monthlyBalance();
    assertEquals(amount, actualMonthlyBalances.totalDebits());
    assertEquals(amount, actualMonthlyBalances.closingBalance());
    assertEquals(JBH_ZERO, actualMonthlyBalances.openingBalance());
    assertEquals(JBH_ZERO, actualMonthlyBalances.monthlyProfit());

    when(accountMonthlyBalanceRepository.findNextBalancesFromPeriodInclusive(
            accountId, YearMonth.of(movementDate.getYear(), movementDate.getMonthValue())))
        .thenReturn(Collections.singletonList(actualMonthlyBalances));

    final List<AccountMonthlyBalanceDTO> futureResponse =
        monthlyBalanceSyncerService
            .persistBalancesAndSyncThemASYNC(
                accountId, Collections.singletonList(actualMonthlyBalances))
            .get();

    assertEquals(2, futureResponse.size());

    // Year and month are not the same
    assertEquals(movementDate.plusMonths(1).getMonthValue(), futureResponse.get(1).month());
    assertEquals(amount, futureResponse.get(1).openingBalance());
    assertEquals(JBH_ZERO, futureResponse.get(0).monthlyProfit());
    assertEquals(JBH_ZERO, futureResponse.get(1).monthlyProfit());
  }

  private AccountDomain withId(final AccountId accountId) {
    return AccountDomain.withBasicMovementForExisting(accountId, JBH_ZERO, JBH_ZERO);
  }

  @Test
  void shouldAddMovement_WhenOnlySnapshotProvided() {
    // Given
    final UUID userId = UUID.randomUUID();
    final AccountId accountId = AccountId.generate();
    final LocalDate movementDate = LocalDate.now();
    final BigDecimal balanceSnashot = new BigDecimal("200.00");
    final BigDecimal existingAccountPpalBalance = new BigDecimal("100.00");
    final AddMovementCommand request =
        new AddMovementCommand(movementDate, null, balanceSnashot, BALANCE_SNAPSHOT, null);

    final AccountDomain accountDomain =
        AccountDomain.withBasicMovementForExisting(
            accountId, existingAccountPpalBalance, new BigDecimal("190.00"));

    when(accountRepository.findByAccountId(userId, accountId))
        .thenReturn(Optional.of(AccountMapper.toDTO(accountDomain)));

    // When & Then
    final AtomicReference<AddBasicMovementDTO> mvmtResponse = new AtomicReference<>();
    assertDoesNotThrow(
        () -> mvmtResponse.set(useCaseInstanceTest.addMovement(userId, accountId, request)));

    verify(accountRepository).findByAccountId(userId, accountId);
    verify(accountMovementRepository).save((MovementDTO) any());
    verify(accountRepository).save(any());

    assertEquals(balanceSnashot, mvmtResponse.get().account().currentBalance());
    assertEquals(existingAccountPpalBalance, mvmtResponse.get().account().movementBalance());
    assertEquals(JBH_ZERO, mvmtResponse.get().monthlyBalance().totalDebits());
    assertEquals(0, mvmtResponse.get().monthlyBalance().totalMovements());
    assertEquals(JBH_ZERO, mvmtResponse.get().monthlyBalance().totalCredits());
    assertEquals(balanceSnashot, mvmtResponse.get().monthlyBalance().closingBalance());
    assertEquals(JBH_ZERO, mvmtResponse.get().monthlyBalance().monthlyProfit());
    assertEquals(JBH_ZERO, mvmtResponse.get().monthlyBalance().openingBalance());
  }

  @Test
  void shouldAddMovement_WhenAmountAndSnapshotProvided() {
    // Given
    final UUID userId = UUID.randomUUID();
    final AccountId accountId = AccountId.generate();
    final LocalDate movementDate = LocalDate.now();
    final BigDecimal amount = new BigDecimal("22685312.00");
    final BigDecimal balanceSnapshot = new BigDecimal("35693653.00");

    final AddMovementCommand request =
        new AddMovementCommand(
            movementDate, amount, balanceSnapshot, DEPOSIT, OTHER_INCOME_CATEGORY);

    final BigDecimal existingMovBalance = new BigDecimal("12591000.00");

    final AccountDomain accountDomain =
        AccountDomain.withBasicMovementForExisting(
            accountId, existingMovBalance, new BigDecimal("12689712.00"));

    when(accountRepository.findByAccountId(userId, accountId))
        .thenReturn(Optional.of(AccountMapper.toDTO(accountDomain)));

    // When & Then
    final AtomicReference<AddBasicMovementDTO> mvmtResponse = new AtomicReference<>();
    assertDoesNotThrow(
        () -> mvmtResponse.set(useCaseInstanceTest.addMovement(userId, accountId, request)));

    verify(accountRepository).findByAccountId(userId, accountId);
    verify(accountMovementRepository).save((MovementDTO) any());
    verify(accountRepository).save((AccountDTO) any());

    final AccountDTO accountResponse = mvmtResponse.get().account();
    assertEquals(amount.add(existingMovBalance), accountResponse.movementBalance());
    assertEquals(balanceSnapshot, accountResponse.currentBalance());

    final AccountMonthlyBalanceDTO monthlyBalanceResponse = mvmtResponse.get().monthlyBalance();
    final YearMonth expectedYearMonth =
        YearMonth.of(movementDate.getYear(), movementDate.getMonthValue());
    assertEquals(
        expectedYearMonth,
        YearMonth.of(monthlyBalanceResponse.year(), monthlyBalanceResponse.month()));
    assertEquals(expectedYearMonth, monthlyBalanceResponse.period());
    assertEquals(balanceSnapshot, monthlyBalanceResponse.closingBalance());
    assertEquals(amount, monthlyBalanceResponse.totalDebits());
    assertEquals(JBH_ZERO, monthlyBalanceResponse.totalCredits());
    assertEquals(1, monthlyBalanceResponse.totalMovements());
    assertEquals(DEPOSIT, mvmtResponse.get().movement().movementType());
  }

  @Test
  void shouldAddMvmtWithExistingMonthlyEntries() {
    // Given
    final UUID userId = UUID.randomUUID();
    final AccountId accountId = AccountId.generate();
    final LocalDate movementDate = LocalDate.now();

    final AccountDomain accountDomain = withId(accountId);

    when(accountRepository.findByAccountId(userId, accountId))
        .thenReturn(Optional.of(AccountMapper.toDTO(accountDomain)));

    final int existingEntries = 10;
    final BigDecimal existingTotalDebits = new BigDecimal("1000.00");
    final BigDecimal existingClosingBalance = new BigDecimal("600.00");
    final BigDecimal existingTotalCredits = new BigDecimal("200.00");
    final BigDecimal existingOpeningBalance = new BigDecimal("500.00");
    final AccountMonthlyBalanceDTO existingMonthlyBalance =
        AccountMonthlyBalanceDTO.builder()
            .id(1L)
            .accountId(accountId)
            .year(movementDate.getYear())
            .month(movementDate.getMonthValue())
            .period(YearMonth.of(movementDate.getYear(), movementDate.getMonthValue()))
            .openingBalance(existingOpeningBalance)
            .closingBalance(existingClosingBalance)
            .totalCredits(existingTotalCredits)
            .totalDebits(existingTotalDebits)
            .totalMovements(existingEntries)
            .build();

    when(accountMonthlyBalanceRepository.findByAccountIdYearAndMonth(
            accountId, movementDate.getYear(), movementDate.getMonthValue()))
        .thenReturn(Optional.of(existingMonthlyBalance));

    // When & Then
    final BigDecimal amount = new BigDecimal("100.00");
    final AddMovementCommand request =
        new AddMovementCommand(movementDate, amount, DEPOSIT, OTHER_INCOME_CATEGORY);
    final AtomicReference<AddBasicMovementDTO> processedResponse = new AtomicReference<>();
    assertDoesNotThrow(
        () -> processedResponse.set(useCaseInstanceTest.addMovement(userId, accountId, request)));

    verify(accountRepository).findByAccountId(userId, accountId);
    verify(accountMovementRepository).save((MovementDTO) any());
    verify(accountRepository).save(any());
    verify(monthlyBalanceWriterRepoMock, atMostOnce())
        .saveBalance((AccountMonthlyBalanceDTO) any());

    assertEquals(amount, processedResponse.get().account().movementBalance());
    assertEquals(existingEntries + 1, processedResponse.get().monthlyBalance().totalMovements());
    assertEquals(
        existingTotalDebits.add(amount), processedResponse.get().monthlyBalance().totalDebits());
    assertEquals(existingOpeningBalance, processedResponse.get().monthlyBalance().openingBalance());
    assertEquals(
        existingClosingBalance.add(amount),
        processedResponse.get().monthlyBalance().closingBalance());
    assertEquals(existingTotalCredits, processedResponse.get().monthlyBalance().totalCredits());
    assertEquals(DEPOSIT, processedResponse.get().movement().movementType());
  }
}
