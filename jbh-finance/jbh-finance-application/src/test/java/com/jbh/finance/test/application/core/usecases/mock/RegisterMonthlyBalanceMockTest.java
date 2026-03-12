package com.jbh.finance.test.application.core.usecases.mock;

import static com.jbh.commons.util.JbhMoneyUtils.JBH_ZERO;
import static com.jbh.commons.util.JbhMoneyUtils.withJBHDecimals;
import static com.jbh.finance.test.application.core.usecases.utils.MonthlyBalanceITUtils.createMonthlyBalanceCommand;
import static com.jbh.finance.test.testfixtures.builders.UseCaseBuilder.DEFAULT_ACCOUNT_TYPE;
import static com.jbh.finance.test.testfixtures.builders.UseCaseBuilder.categoryServiceMock;
import static com.jbh.finance.test.testfixtures.builders.UseCaseBuilder.movementInMemoQuery;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.async.AsyncTaskExecutorImpl;
import com.jbh.finance.application.feature.monthlybalance.commands.AddMonthlyBalanceCommand;
import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.finance.application.feature.monthlybalance.ports.input.RegisterMonthlyBalanceInputPort;
import com.jbh.finance.application.feature.monthlybalance.ports.output.MonthlyBalanceQueryRepo;
import com.jbh.finance.application.feature.monthlybalance.ports.output.MonthlyBalanceWriterRepo;
import com.jbh.finance.application.feature.monthlybalance.services.MonthlyBalanceLifecycleService;
import com.jbh.finance.application.feature.monthlybalance.services.MonthlyBalanceLifecycleServiceImpl;
import com.jbh.finance.application.feature.monthlybalance.usecases.RegisterMonthlyBalanceUseCase;
import com.jbh.finance.application.feature.movement.ports.output.MovementWriterRepository;
import com.jbh.finance.application.feature.movement.services.MovementLifecycleService;
import com.jbh.finance.application.feature.movement.services.MovementLifecycleServiceImpl;
import com.jbh.finance.application.feature.movement.services.ProcessMovementServiceImpl;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.application.feature.product.ports.output.ProductRepository;
import com.jbh.finance.application.feature.product.services.ProductLifecycleService;
import com.jbh.finance.application.feature.product.services.ProductLifecycleServiceImpl;
import com.jbh.finance.domain.product.vo.ProductId;
import com.jbh.finance.test.application.core.usecases.utils.MonthlyBalanceITUtils;
import com.jbh.finance.test.application.core.usecases.utils.UnitOfWorkTest;
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

  ProcessMovementServiceImpl accountMovementService;
  MonthlyBalanceLifecycleService monthlyBalanceService;
  ProductLifecycleService accountService;
  @Mock private ProductRepository accountRepository;
  @Mock private MovementWriterRepository accountMovementRepository;
  @Mock private MonthlyBalanceQueryRepo monthlyBalanceQueryRepoMock;
  @Mock private MonthlyBalanceWriterRepo monthlyBalanceWriterRepoMock;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    when(accountRepository.findByUserAndProductId(userId, accountId))
        .thenReturn(
            Optional.of(
                ProductDTO.defaultBuilder(userId, accountId, "DF", DEFAULT_ACCOUNT_TYPE).build()));

    accountService = new ProductLifecycleServiceImpl(accountRepository);

    final MonthlyBalanceLifecycleServiceImpl realMonthlyBalanceService =
        new MonthlyBalanceLifecycleServiceImpl(
            monthlyBalanceQueryRepoMock,
            monthlyBalanceWriterRepoMock,
            new AsyncTaskExecutorImpl(),
            accountService);

    final MovementLifecycleService coreAccountMovementService =
        new MovementLifecycleServiceImpl(accountMovementRepository, movementInMemoQuery);

    accountMovementService =
        new ProcessMovementServiceImpl(
            coreAccountMovementService,
            accountService,
            realMonthlyBalanceService,
            new UnitOfWorkTest(),
            categoryServiceMock);
    monthlyBalanceService = spy(realMonthlyBalanceService);

    useCaseInstanceTest =
        new RegisterMonthlyBalanceInputPort(
            monthlyBalanceService, accountService, accountMovementService, categoryServiceMock);
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
            .productId(accountId)
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
            .productId(accountId)
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
        BusinessException.class,
        () ->
            savedMonthlyBalance.set(
                useCaseInstanceTest.registerOfficialMonthlyBalance(
                    runningDatePast, userId, accountId, command)));

    // Then
    verify(monthlyBalanceService, never()).updateOpeningBalanceNextMonth(any());
  }
}
