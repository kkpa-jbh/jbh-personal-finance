package com.jbh.account.application.core.usecases.mock;

import static com.jbh.account.application.builders.UseCaseBuilder.DEFAULT_ACCOUNT_TYPE;
import static com.jbh.account.application.builders.UseCaseBuilder.movementQueryRepository;
import static com.jbh.account.application.core.usecases.utils.MonthlyBalanceITUtils.createMonthlyBalanceCommand;
import static com.jbh.account.domain.utils.JbhMoneyUtils.JBH_ZERO;
import static com.jbh.account.domain.utils.JbhMoneyUtils.withJBHDecimals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jbh.account.application.async.AsyncTaskExecutorImpl;
import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.application.core.dto.ProductDTO;
import com.jbh.account.application.core.ports.input.RegisterMonthlyBalanceInputPort;
import com.jbh.account.application.core.ports.output.AccountRepository;
import com.jbh.account.application.core.ports.output.monthlybalance.AccountMonthlyBalanceQueryRepo;
import com.jbh.account.application.core.ports.output.monthlybalance.AccountMonthlyBalanceWriterRepository;
import com.jbh.account.application.core.services.account.AccountService;
import com.jbh.account.application.core.services.account.AccountServiceImpl;
import com.jbh.account.application.core.services.monthlybalance.MonthlyBalanceService;
import com.jbh.account.application.core.services.monthlybalance.MonthlyBalanceServiceImpl;
import com.jbh.account.application.core.services.movements.AccountMovementApplicationServiceImpl;
import com.jbh.account.application.core.services.movements.AccountMovementService;
import com.jbh.account.application.core.services.movements.AccountMovementServiceImpl;
import com.jbh.account.application.core.usecases.RegisterMonthlyBalanceUseCase;
import com.jbh.account.application.core.usecases.utils.MonthlyBalanceITUtils;
import com.jbh.account.application.core.usecases.utils.UnitOfWorkTest;
import com.jbh.account.application.core.vo.commands.AddMonthlyBalanceCommand;
import com.jbh.account.application.movements.ports.output.AccountMovementWriterRepository;
import com.jbh.account.domain.exceptions.ProductBusinessException;
import com.jbh.account.domain.vo.ProductId;
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
  final ProductId accountId = ProductId.generate();
  final YearMonth august24MonthlyPeriod = YearMonth.of(2024, 8);
  final LocalDate runningDate = LocalDate.now();
  RegisterMonthlyBalanceUseCase useCaseInstanceTest;

  AccountMovementApplicationServiceImpl accountMovementService;
  MonthlyBalanceService monthlyBalanceService;
  AccountService accountService;
  @Mock private AccountRepository accountRepository;
  @Mock private AccountMovementWriterRepository accountMovementRepository;
  @Mock private AccountMonthlyBalanceQueryRepo monthlyBalanceQueryRepoMock;
  @Mock private AccountMonthlyBalanceWriterRepository monthlyBalanceWriterRepoMock;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    when(accountRepository.findByUserAndAccountId(userId, accountId))
        .thenReturn(
            Optional.of(
                ProductDTO.defaultBuilder(userId, accountId, "DF", DEFAULT_ACCOUNT_TYPE).build()));

    accountService = new AccountServiceImpl(accountRepository);

    final MonthlyBalanceServiceImpl realMonthlyBalanceService =
        new MonthlyBalanceServiceImpl(
            monthlyBalanceQueryRepoMock,
            monthlyBalanceWriterRepoMock,
            new AsyncTaskExecutorImpl(),
            accountService);

    final AccountMovementService coreAccountMovementService =
        new AccountMovementServiceImpl(accountMovementRepository, movementQueryRepository);

    accountMovementService =
        new AccountMovementApplicationServiceImpl(
            coreAccountMovementService,
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
        createMonthlyBalanceCommand(august24MonthlyPeriod, closingBalance, null);

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

    /* The method is not returning anything. I'm not verifying it.
    // Verify the captured nextMonthlyBalance
    assertNotNull(capturedNextMonthlyBalance);
    assertEquals(
        capturedNextMonthlyBalance.get().openingBalance(), withJBHDecimals(closingBalance));
    // ... other assertions on nextMonthlyBalance

     */

    // Verify the method was called
    verify(monthlyBalanceService).updateOpeningBalanceNextMonth(any(MonthlyBalanceDTO.class));
  }

  @Test
  void shouldThrowExceptionWhenNotValidPeriod() {
    // Given

    final BigDecimal closingBalance = BigDecimal.valueOf(2105192.00);
    final AddMonthlyBalanceCommand command =
        createMonthlyBalanceCommand(august24MonthlyPeriod, closingBalance, null);
    final LocalDate runningDatePast = LocalDate.of(august24MonthlyPeriod.getYear() - 1, 8, 1);
    when(monthlyBalanceQueryRepoMock.findByAccountIdYearAndMonth(
            accountId, august24MonthlyPeriod.getYear(), august24MonthlyPeriod.getMonthValue()))
        .thenReturn(Optional.empty());

    // When
    final AtomicReference<MonthlyBalanceDTO> savedMonthlyBalance = new AtomicReference<>();
    Assertions.assertThrows(
        ProductBusinessException.class,
        () ->
            savedMonthlyBalance.set(
                useCaseInstanceTest.registerOfficialMonthlyBalance(
                    runningDatePast, userId, accountId, command)));

    // Then
    verify(monthlyBalanceService, never()).updateOpeningBalanceNextMonth(any());
  }
}
