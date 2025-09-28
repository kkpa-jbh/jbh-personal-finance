package com.jbh.account.application.core.usecases.mock;

import static com.jbh.account.domain.utils.MoneyUtils.JBH_ZERO;
import static com.jbh.account.domain.utils.MoneyUtils.withJBHDecimals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jbh.account.application.async.AsyncTaskExecutorImpl;
import com.jbh.account.application.core.dto.AccountDTO;
import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.application.core.ports.input.RegisterMonthlyBalanceInputPort;
import com.jbh.account.application.core.ports.output.AccountRepository;
import com.jbh.account.application.core.ports.output.monthlybalance.AccountMonthlyBalanceQueryRepo;
import com.jbh.account.application.core.ports.output.monthlybalance.AccountMonthlyBalanceWriterRepository;
import com.jbh.account.application.core.services.account.AccountService;
import com.jbh.account.application.core.services.account.AccountServiceImpl;
import com.jbh.account.application.core.services.monthlybalance.MonthlyBalanceService;
import com.jbh.account.application.core.services.monthlybalance.MonthlyBalanceServiceImpl;
import com.jbh.account.application.core.services.movements.AccountMovementServiceImpl;
import com.jbh.account.application.core.usecases.RegisterMonthlyBalanceUseCase;
import com.jbh.account.application.core.usecases.utils.MonthlyBalanceITUtils;
import com.jbh.account.application.core.usecases.utils.UnitOfWorkTest;
import com.jbh.account.application.core.vo.commands.AddMonthlyBalanceCommand;
import com.jbh.account.application.exceptions.JbhSpecificationApplication;
import com.jbh.account.application.movements.ports.output.AccountMovementRepository;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class RegisterMonthlyBalanceMockTest {

  final UUID userId = UUID.randomUUID();
  final AccountId accountId = AccountId.generate();
  final YearMonth august24MonthlyPeriod = YearMonth.of(2024, 8);
  final LocalDate runningDate = LocalDate.now();
  RegisterMonthlyBalanceUseCase useCaseInstanceTest;

  AccountMovementServiceImpl accountMovementService;
  MonthlyBalanceService monthlyBalanceService;
  AccountService accountService;
  @Mock private AccountRepository accountRepository;
  @Mock private AccountMovementRepository accountMovementRepository;
  @Mock private AccountMonthlyBalanceQueryRepo monthlyBalanceQueryRepoMock;
  @Mock private AccountMonthlyBalanceWriterRepository monthlyBalanceWriterRepoMock;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    when(accountRepository.findByUserAndAccountId(userId, accountId))
        .thenReturn(
            Optional.of(
                AccountDTO.defaultBuilder(userId, accountId, "DF", AccountType.OTHER).build()));

    final MonthlyBalanceServiceImpl realMonthlyBalanceService =
        new MonthlyBalanceServiceImpl(
            monthlyBalanceQueryRepoMock, monthlyBalanceWriterRepoMock, new AsyncTaskExecutorImpl());
    accountService = new AccountServiceImpl(accountRepository);
    accountMovementService =
        new AccountMovementServiceImpl(
            accountMovementRepository,
            accountService,
            realMonthlyBalanceService,
            new UnitOfWorkTest());
    monthlyBalanceService = spy(realMonthlyBalanceService);

    useCaseInstanceTest =
        new RegisterMonthlyBalanceInputPort(
            monthlyBalanceService, accountService, accountMovementService);
  }

  @Test
  void shouldRegisterMonthlyBalanceWhenItDoesNotExist() {
    // Given
    final BigDecimal movementsBalance = BigDecimal.ZERO;
    final BigDecimal closingBalance = BigDecimal.valueOf(2105192.00);
    final AddMonthlyBalanceCommand command =
        new AddMonthlyBalanceCommand(august24MonthlyPeriod, closingBalance, null);

    final MonthlyBalanceDTO monthlyBalanceDTO =
        MonthlyBalanceDTO.defaultBuilder()
            .accountId(accountId)
            .period(august24MonthlyPeriod)
            .month(8)
            .year(2024)
            .closingBalance(closingBalance)
            .movementBalance(closingBalance)
            .totalDebits(JBH_ZERO)
            .totalCredits(JBH_ZERO)
            .monthlyNetProfit(JBH_ZERO)
            .totalMovements(1)
            .build();

    when(monthlyBalanceService.findByAccountIdYearAndMonth(accountId, 2024, 8))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(monthlyBalanceDTO));
    ;

    // Spy on the specific method to capture its return value
    final AtomicReference<MonthlyBalanceDTO> capturedNextMonthlyBalance = new AtomicReference<>();
    doAnswer(
            invocation -> {
              // Call the real method
              final MonthlyBalanceDTO result = (MonthlyBalanceDTO) invocation.callRealMethod();
              // Capture the result
              capturedNextMonthlyBalance.set(result);
              return result;
            })
        .when(monthlyBalanceService)
        .updateOpeningBalanceNextMonth(any(MonthlyBalanceDTO.class));

    // When
    final AtomicReference<MonthlyBalanceDTO> savedMonthlyBalance = new AtomicReference<>();
    Assertions.assertDoesNotThrow(
        () ->
            savedMonthlyBalance.set(
                useCaseInstanceTest.registerOfficialMonthlyBalance(
                    runningDate, userId, accountId, command)));

    // Then
    final var actualMonthlyBalance = savedMonthlyBalance.get();
    final MonthlyBalanceDTO expectedMonthBalance =
        MonthlyBalanceDTO.defaultBuilder()
            .accountId(accountId)
            .totalMovements(1)
            .period(august24MonthlyPeriod)
            .month(8)
            .year(2024)
            .closingBalance(withJBHDecimals(closingBalance))
            .movementBalance(withJBHDecimals(movementsBalance))
            .totalDebits(JBH_ZERO)
            .totalCredits(JBH_ZERO)
            .monthlyNetProfit(JBH_ZERO)
            .officialMonthlyReport(true)
            .build();
    MonthlyBalanceITUtils.assertMonthlyBalance(expectedMonthBalance, actualMonthlyBalance);

    // Verify the captured nextMonthlyBalance
    assertNotNull(capturedNextMonthlyBalance);
    assertEquals(
        capturedNextMonthlyBalance.get().openingBalance(), withJBHDecimals(closingBalance));
    // ... other assertions on nextMonthlyBalance

    // Verify the method was called
    verify(monthlyBalanceService).updateOpeningBalanceNextMonth(any(MonthlyBalanceDTO.class));
  }

  @Test
  void shouldThrowExceptionWhenNotValidPeriod() {
    // Given

    final BigDecimal movementsBalance = BigDecimal.ZERO;
    final BigDecimal closingBalance = BigDecimal.valueOf(2105192.00);
    final AddMonthlyBalanceCommand command =
        new AddMonthlyBalanceCommand(august24MonthlyPeriod, closingBalance, null);
    final LocalDate runningDatePast = LocalDate.of(august24MonthlyPeriod.getYear() - 1, 8, 1);
    when(monthlyBalanceQueryRepoMock.findByAccountIdYearAndMonth(
            accountId, august24MonthlyPeriod.getYear(), august24MonthlyPeriod.getMonthValue()))
        .thenReturn(Optional.empty());

    // When
    final AtomicReference<MonthlyBalanceDTO> savedMonthlyBalance = new AtomicReference<>();
    Assertions.assertThrows(
        JbhSpecificationApplication.class,
        () ->
            savedMonthlyBalance.set(
                useCaseInstanceTest.registerOfficialMonthlyBalance(
                    runningDatePast, userId, accountId, command)));

    // Then
    verify(monthlyBalanceService, never()).updateOpeningBalanceNextMonth(any());
  }
}
