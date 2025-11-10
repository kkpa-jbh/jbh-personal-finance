package com.jbh.account.application.core.usecases.integration.movements;

import static com.jbh.account.application.builders.CommandTestBuilder.createInvestmentCommand;
import static com.jbh.account.application.builders.CommandTestBuilder.createLiquidateCommandToExternal;
import static com.jbh.account.application.builders.UseCaseBuilder.delayTests;
import static com.jbh.account.application.core.usecases.utils.MonthlyBalanceITUtils.assertMonthlyBalance;
import static com.jbh.account.domain.utils.JbhMoneyUtils.JBH_ZERO;
import static com.jbh.account.domain.utils.JbhMoneyUtils.withJBHDecimals;
import static com.jbh.account.domain.vo.MovementType.BALANCE_SNAPSHOT;
import static com.jbh.account.domain.vo.MovementType.WITHDRAWAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jbh.account.application.builders.UseCaseBuilder;
import com.jbh.account.application.core.dto.AccountDTO;
import com.jbh.account.application.core.dto.LiquidationResultDTO;
import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.application.core.ports.output.account.InMemoryAccountRepository;
import com.jbh.account.application.core.services.monthlybalance.MonthlyBalanceService;
import com.jbh.account.application.core.services.movements.AccountMovementApplicationServiceImpl;
import com.jbh.account.application.core.usecases.AddMovementUseCase;
import com.jbh.account.application.core.usecases.CreateAccountUseCase;
import com.jbh.account.application.core.usecases.LiquidateAccountUseCase;
import com.jbh.account.application.core.usecases.integration.monthlybalance.RegisterMonthlyReportedWithoutProfitITTest;
import com.jbh.account.application.core.vo.commands.AddMovementCommand;
import com.jbh.account.application.core.vo.commands.ExternalAccountInfoVO;
import com.jbh.account.application.core.vo.commands.LiquidateAccountCommand;
import com.jbh.account.application.movements.ports.output.AccountMovementWriterRepository;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.ExpenseCategory;
import com.jbh.account.domain.vo.IncomeCategory;
import com.jbh.account.domain.vo.MovementCategoryDTO;
import com.jbh.account.domain.vo.MovementType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TestMethodOrder(OrderAnnotation.class)
public class RegisterInvesmentMovementITTest {
  static final UUID userId = UUID.randomUUID();
  private static final Logger LOG =
      LoggerFactory.getLogger(RegisterMonthlyReportedWithoutProfitITTest.class);
  private static final InMemoryAccountRepository inMemoryAccountRepo =
      UseCaseBuilder.getAccountRepository();
  private static CreateAccountUseCase createAccountUseCase;
  private static AddMovementUseCase addMovementUseCase;
  private static LiquidateAccountUseCase liquidateAccountUseCase;
  @Mock private static AccountMovementWriterRepository accountMovementRepository;
  private static AccountDTO acciCuenta;
  private static AccountId acciCuentaId;
  private static AccountDTO fondoAcciones;
  private static AccountId fondoAccionesId;
  private static BigDecimal finalAcciBalanceSept;
  private final BigDecimal initialBalance = withJBHDecimals(new BigDecimal("5000000"));
  private MonthlyBalanceService monthlyBalanceService;
  private AccountMovementApplicationServiceImpl accountMovementApplicationService;

  @BeforeAll
  static void beforeAll() {
    UseCaseBuilder.getInMemoryMonthlyBalanceRepos().clearStorage();
    UseCaseBuilder.getAccountRepository().clearStorage();
  }

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    monthlyBalanceService = UseCaseBuilder.buildMonthlyBalanceService();

    accountMovementApplicationService =
        UseCaseBuilder.buildAccountMovementApplicationService(accountMovementRepository);

    createAccountUseCase = UseCaseBuilder.buildCreateAccountUseCase();

    addMovementUseCase = UseCaseBuilder.buildAddMovementUseCase(accountMovementRepository);

    liquidateAccountUseCase =
        UseCaseBuilder.buildLiquidateAccountUseCase(accountMovementRepository);

    delayTests();
  }

  @Test
  @Order(0)
  void createInvestmentAccount() throws AccountBusinessException {
    acciCuenta =
        createAccountUseCase.execute(createInvestmentCommand(userId, "ACCICUENTA", "TRII"));
    acciCuentaId = acciCuenta.id();
    LOG.info("Account created with id {}", acciCuentaId);
    assertNotNull(acciCuentaId);

    fondoAcciones =
        createAccountUseCase.execute(createInvestmentCommand(userId, "FONDOACCIONES", "TRII"));
    fondoAccionesId = fondoAcciones.id();
  }

  @Test
  @Order(1)
  void initialBalance() throws AccountBusinessException {
    final AddMovementCommand movement =
        new AddMovementCommand(
            LocalDate.of(2025, 8, 20),
            initialBalance,
            initialBalance,
            MovementType.DEPOSIT,
            MovementCategoryDTO.withType(IncomeCategory.INITIAL_BALANCE));

    addMovementUseCase.addMovement(userId, acciCuentaId, movement);
    addMovementUseCase.addMovement(userId, fondoAccionesId, movement);
  }

  @Test
  @Order(2)
  void registerBalanceSnapshotSept1() throws AccountBusinessException {
    final BigDecimal acciCuentaBalance = withJBHDecimals(new BigDecimal("5022458.19"));
    final var entryDate = LocalDate.of(2025, 9, 22);

    final AddMovementCommand acciCuentaUpdate =
        new AddMovementCommand(entryDate, null, acciCuentaBalance, BALANCE_SNAPSHOT, null);

    addMovementUseCase.addMovement(userId, acciCuentaId, acciCuentaUpdate);

    inMemoryAccountRepo
        .findByAccountId(acciCuentaId)
        .ifPresent(
            updatedAcciCuenta -> {
              assertEquals(withJBHDecimals(acciCuentaBalance), updatedAcciCuenta.currentBalance());
              assertEquals(withJBHDecimals("22458.19"), updatedAcciCuenta.netProfitBalance());
              assertEquals(withJBHDecimals("0.45"), updatedAcciCuenta.netGrowthRate());
            });

    // Fondo Acciones
    final var fondoAccionesBalance = withJBHDecimals(new BigDecimal("4978356.37"));
    final AddMovementCommand fondoAccionesUpdate =
        new AddMovementCommand(entryDate, null, fondoAccionesBalance, BALANCE_SNAPSHOT, null);
    addMovementUseCase.addMovement(userId, fondoAccionesId, fondoAccionesUpdate);

    inMemoryAccountRepo
        .findByAccountId(fondoAccionesId)
        .ifPresent(
            updatedFondoAcciones -> {
              assertEquals(
                  withJBHDecimals(fondoAccionesBalance), updatedFondoAcciones.currentBalance());
              assertEquals(withJBHDecimals("-21643.63"), updatedFondoAcciones.netProfitBalance());
              assertEquals(withJBHDecimals("-0.43"), updatedFondoAcciones.netGrowthRate());
            });
  }

  @Test
  @Order(2)
  void registerBalanceSnapshotSept2() throws AccountBusinessException {
    finalAcciBalanceSept = withJBHDecimals(new BigDecimal("5075628.00"));
    final var entryDate = LocalDate.of(2025, 9, 25);

    final AddMovementCommand acciCuentaUpdate =
        new AddMovementCommand(entryDate, null, finalAcciBalanceSept, BALANCE_SNAPSHOT, null);

    addMovementUseCase.addMovement(userId, acciCuentaId, acciCuentaUpdate);

    inMemoryAccountRepo
        .findByAccountId(acciCuentaId)
        .ifPresent(
            updatedAcciCuenta -> {
              assertEquals(
                  withJBHDecimals(finalAcciBalanceSept), updatedAcciCuenta.currentBalance());
              assertEquals(withJBHDecimals("75628.00"), updatedAcciCuenta.netProfitBalance());
              assertEquals(withJBHDecimals("1.51"), updatedAcciCuenta.netGrowthRate());
            });

    // Fondo Acciones
    final var fondoAccionesBalance = withJBHDecimals(new BigDecimal("5037174.00"));
    final AddMovementCommand fondoAccionesUpdate =
        new AddMovementCommand(entryDate, null, fondoAccionesBalance, BALANCE_SNAPSHOT, null);
    addMovementUseCase.addMovement(userId, fondoAccionesId, fondoAccionesUpdate);

    inMemoryAccountRepo
        .findByAccountId(fondoAccionesId)
        .ifPresent(
            updatedFondoAcciones -> {
              assertEquals(
                  withJBHDecimals(fondoAccionesBalance), updatedFondoAcciones.currentBalance());
              assertEquals(withJBHDecimals("37174.00"), updatedFondoAcciones.netProfitBalance());
              assertEquals(withJBHDecimals("0.74"), updatedFondoAcciones.netGrowthRate());
            });
  }

  @Test
  @Order(4)
  void registerBalanceSnapshotOct1() throws AccountBusinessException {
    final var entryDate = LocalDate.of(2025, 10, 16);

    // Check previous monthly balance
    final MonthlyBalanceDTO previousMonthlyBalance =
        monthlyBalanceService
            .findByAccountIdAndPeriod(
                acciCuentaId, YearMonth.of(2025, entryDate.getMonthValue() - 1))
            .orElse(null);
    assertNotNull(previousMonthlyBalance);
    assertEquals(
        withJBHDecimals(new BigDecimal("5000000")), previousMonthlyBalance.openingBalance());
    assertEquals(withJBHDecimals(new BigDecimal("1.51")), previousMonthlyBalance.netGrowthRate());
    assertEquals(
        withJBHDecimals(new BigDecimal("75628")), previousMonthlyBalance.monthlyNetProfit());

    // Check current monthly balance
    MonthlyBalanceDTO currentMonthlyBalance =
        monthlyBalanceService
            .findByAccountIdAndPeriod(acciCuentaId, YearMonth.of(2025, entryDate.getMonthValue()))
            .orElse(null);
    assertNotNull(currentMonthlyBalance);
    assertEquals(withJBHDecimals(finalAcciBalanceSept), currentMonthlyBalance.openingBalance());
    assertEquals(withJBHDecimals(new BigDecimal("0")), currentMonthlyBalance.netGrowthRate());
    assertEquals(withJBHDecimals(new BigDecimal("0")), currentMonthlyBalance.monthlyNetProfit());

    final BigDecimal acciCuentaBalance = withJBHDecimals(new BigDecimal("5065484"));

    final AddMovementCommand acciCuentaUpdate =
        new AddMovementCommand(entryDate, null, acciCuentaBalance, BALANCE_SNAPSHOT, null);

    addMovementUseCase.addMovement(userId, acciCuentaId, acciCuentaUpdate);

    inMemoryAccountRepo
        .findByAccountId(acciCuentaId)
        .ifPresent(
            updatedAcciCuenta -> {
              assertEquals(withJBHDecimals(acciCuentaBalance), updatedAcciCuenta.currentBalance());
              assertEquals(withJBHDecimals("65484.00"), updatedAcciCuenta.netProfitBalance());
              assertEquals(withJBHDecimals("1.31"), updatedAcciCuenta.netGrowthRate());
            });

    delayTests();
    currentMonthlyBalance =
        monthlyBalanceService
            .findByAccountIdAndPeriod(acciCuentaId, YearMonth.of(2025, entryDate.getMonthValue()))
            .orElse(null);
    final MonthlyBalanceDTO expectedAcciCuentaBalance =
        MonthlyBalanceDTO.defaultBuilder()
            .accountId(acciCuentaId)
            .period(YearMonth.of(2025, entryDate.getMonthValue()))
            .openingBalance(withJBHDecimals(finalAcciBalanceSept))
            .closingBalance(withJBHDecimals(acciCuentaBalance))
            .monthlyNetProfit(withJBHDecimals(new BigDecimal("-10144.00")))
            .netGrowthRate(withJBHDecimals(new BigDecimal("-0.20")))
            .build();
    assertMonthlyBalance(expectedAcciCuentaBalance, currentMonthlyBalance);

    // Fondo Acciones
    final var fondoAccionesBalance = withJBHDecimals(new BigDecimal("5072052"));
    final AddMovementCommand fondoAccionesUpdate =
        new AddMovementCommand(entryDate, null, fondoAccionesBalance, BALANCE_SNAPSHOT, null);
    addMovementUseCase.addMovement(userId, fondoAccionesId, fondoAccionesUpdate);

    inMemoryAccountRepo
        .findByAccountId(fondoAccionesId)
        .ifPresent(
            updatedFondoAcciones -> {
              assertEquals(
                  withJBHDecimals(fondoAccionesBalance), updatedFondoAcciones.currentBalance());
              assertEquals(withJBHDecimals("72052.00"), updatedFondoAcciones.netProfitBalance());
              assertEquals(withJBHDecimals("1.44"), updatedFondoAcciones.netGrowthRate());
            });
  }

  @Test
  @Order(99)
  void shouldWithDrawalAllMoneySuccessfully() throws AccountBusinessException {

    final AccountDTO account = inMemoryAccountRepo.findByAccountId(acciCuentaId).orElse(null);
    final BigDecimal currentBalance = account.currentBalance();

    final AddMovementCommand withdrawal =
        new AddMovementCommand(
            LocalDate.now(),
            currentBalance,
            BigDecimal.ZERO,
            WITHDRAWAL,
            MovementCategoryDTO.withType(ExpenseCategory.PERSONAL));

    assertThrows(
        AccountBusinessException.class,
        () -> addMovementUseCase.addMovement(userId, acciCuentaId, withdrawal));

    final var latestEarning = new BigDecimal("120.00");
    final LiquidateAccountCommand liquidateCommand =
        createLiquidateCommandToExternal(
            new ExternalAccountInfoVO("External Account"),
            currentBalance.add(latestEarning),
            LocalDate.now());

    final LiquidationResultDTO result =
        liquidateAccountUseCase.liquidateAccount(userId, acciCuentaId, liquidateCommand);
    assertTrue(result.valid());

    delayTests();
    delayTests();

    final AccountDTO updatedAccount =
        inMemoryAccountRepo.findByAccountId(acciCuentaId).orElse(null);
    assertNotNull(updatedAccount);
    // assertEquals(new BigDecimal("1.31"), updatedAccount.netGrowthRate());
    assertTrue(updatedAccount.netProfitBalance().compareTo(BigDecimal.ZERO) > 0);
    assertEquals(JBH_ZERO, updatedAccount.currentBalance());
    assertFalse(updatedAccount.isActive());
    assertTrue(updatedAccount.movementBalance().compareTo(BigDecimal.ZERO) < 0);

    final MonthlyBalanceDTO lastMonthBalance =
        monthlyBalanceService.findAllByAccountIdUntilNow(acciCuentaId).getLast();
    assertNotNull(lastMonthBalance);
    assertEquals(latestEarning, lastMonthBalance.monthlyNetProfit());
    assertEquals(JBH_ZERO, lastMonthBalance.netGrowthRate());
    assertEquals(JBH_ZERO, lastMonthBalance.closingBalance());
  }
}
