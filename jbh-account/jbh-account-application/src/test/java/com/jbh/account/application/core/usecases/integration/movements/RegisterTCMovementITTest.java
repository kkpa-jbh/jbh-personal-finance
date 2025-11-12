package com.jbh.account.application.core.usecases.integration.movements;

import static com.jbh.account.application.builders.CommandTestBuilder.createDepositIncome;
import static com.jbh.account.application.builders.CommandTestBuilder.createPersonalExpense;
import static com.jbh.account.application.core.usecases.utils.AccountITUtils.assertAccount;
import static com.jbh.account.application.core.usecases.utils.MonthlyBalanceITUtils.assertMonthlyBalance;
import static com.jbh.account.domain.utils.JbhMoneyUtils.withJBHDecimals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jbh.account.application.builders.AccountEntityBuilder;
import com.jbh.account.application.builders.CommandTestBuilder;
import com.jbh.account.application.builders.EntityTestBuilder;
import com.jbh.account.application.builders.UseCaseBuilder;
import com.jbh.account.application.core.dto.AccountDTO;
import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.application.core.mappers.AccountMapper;
import com.jbh.account.application.core.ports.output.account.InMemoryAccountRepository;
import com.jbh.account.application.core.services.monthlybalance.MonthlyBalanceService;
import com.jbh.account.application.core.usecases.AddMovementUseCase;
import com.jbh.account.application.core.usecases.CreateAccountUseCase;
import com.jbh.account.application.core.usecases.integration.monthlybalance.RegisterMonthlyReportedWithoutProfitITTest;
import com.jbh.account.application.movements.ports.output.AccountMovementWriterRepository;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.vo.ProductMetadata;
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
public class RegisterTCMovementITTest {

  public static final BigDecimal CREDIT_LIMIT = new BigDecimal("1000.00");
  static final UUID userId = UUID.randomUUID();
  private static final Logger LOG =
      LoggerFactory.getLogger(RegisterMonthlyReportedWithoutProfitITTest.class);
  private static final InMemoryAccountRepository inMemoryAccountRepo =
      UseCaseBuilder.getAccountRepository();
  private static final String name = "CREDIT CARD";
  private static final YearMonth period = YearMonth.of(2023, 1);
  private static CreateAccountUseCase createAccountUseCase;
  private static AddMovementUseCase addMovementUseCase;
  @Mock private static AccountMovementWriterRepository accountMovementRepository;
  private static AccountDTO creditCardAccount;
  private MonthlyBalanceService monthlyBalanceService;

  @BeforeAll
  static void beforeAll() {
    UseCaseBuilder.getInMemoryMonthlyBalanceRepos().clearStorage();
    UseCaseBuilder.getAccountRepository().clearStorage();
  }

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    createAccountUseCase = UseCaseBuilder.buildCreateAccountUseCase();
    addMovementUseCase = UseCaseBuilder.buildAddMovementUseCase(accountMovementRepository);
    monthlyBalanceService = UseCaseBuilder.buildMonthlyBalanceService();

    UseCaseBuilder.delayTests();
  }

  @Test
  @Order(0)
  void createAccount() throws AccountBusinessException {
    final LocalDate mvmDate = period.atDay(1);
    final ProductMetadata metadata = ProductMetadata.empty();
    metadata.putCreditLimit(CREDIT_LIMIT);
    metadata.putPaymentDueDay(15);
    creditCardAccount =
        createAccountUseCase.execute(
            CommandTestBuilder.createCreditCardCommand(userId, name, metadata));
    assertNotNull(creditCardAccount);
    assertNotNull(creditCardAccount.id());

    final var personalExpense = new BigDecimal("100.00");
    addMovementUseCase.addMovement(
        userId, creditCardAccount.id(), createPersonalExpense(mvmDate, personalExpense));

    final AccountDTO updatedAccount =
        inMemoryAccountRepo.findByAccountId(creditCardAccount.id()).get();
    assertNotNull(updatedAccount);

    final var expectedAccountBuilder =
        AccountEntityBuilder.withBuilder(
            creditCardAccount.id(),
            userId,
            personalExpense.negate(),
            personalExpense.negate(),
            creditCardAccount.name(),
            creditCardAccount.type());

    final AccountDTO expectedAccount = AccountMapper.toDTO(expectedAccountBuilder.build());

    assertAccount(expectedAccount, updatedAccount);
  }

  @Test
  @Order(1)
  void registerExpense202301() throws AccountBusinessException {
    final LocalDate mvmDate = period.atDay(10);
    final var personalExpense = new BigDecimal("800.00");
    addMovementUseCase.addMovement(
        userId, creditCardAccount.id(), createPersonalExpense(mvmDate, personalExpense));

    final AccountDTO updatedAccount =
        inMemoryAccountRepo.findByAccountId(creditCardAccount.id()).get();
    assertNotNull(updatedAccount);

    final var expectedAccountBuilder =
        AccountEntityBuilder.withBuilder(
            creditCardAccount.id(),
            userId,
            new BigDecimal("-900.00"),
            new BigDecimal("-900.00"),
            creditCardAccount.name(),
            creditCardAccount.type());

    final AccountDTO expectedAccount = AccountMapper.toDTO(expectedAccountBuilder.build());

    assertAccount(expectedAccount, updatedAccount);
  }

  @Test
  @Order(2)
  void registerDeposit202301() throws AccountBusinessException {
    final LocalDate mvmDate = period.atDay(10);
    final var personalExpense = new BigDecimal("200.00");
    addMovementUseCase.addMovement(
        userId, creditCardAccount.id(), createDepositIncome(mvmDate, personalExpense));

    final AccountDTO updatedAccount =
        inMemoryAccountRepo.findByAccountId(creditCardAccount.id()).get();
    assertNotNull(updatedAccount);

    final var expectedAccountBuilder =
        AccountEntityBuilder.withBuilder(
            creditCardAccount.id(),
            userId,
            new BigDecimal("-700.00"),
            new BigDecimal("-700.00"),
            creditCardAccount.name(),
            creditCardAccount.type());

    final AccountDTO expectedAccount = AccountMapper.toDTO(expectedAccountBuilder.build());

    assertAccount(expectedAccount, updatedAccount);

    UseCaseBuilder.delayTests();
    LOG.info("Checking Monthly Balance for period in test {} ", period);

    final MonthlyBalanceDTO actualMonthlyBalance =
        monthlyBalanceService.findByAccountIdAndPeriod(creditCardAccount.id(), period).get();
    final MonthlyBalanceDTO expectedMonthlyBalance =
        EntityTestBuilder.withClosingBalance(
                creditCardAccount.id(), period, withJBHDecimals(new BigDecimal("-700.00")))
            .totalDebits(withJBHDecimals(new BigDecimal("200.00")))
            .totalCredits(withJBHDecimals(new BigDecimal("900.00")))
            .movementBalance(withJBHDecimals(new BigDecimal("-700")))
            .openingBalance(withJBHDecimals(new BigDecimal("0.00")))
            .monthlyReportedProfit(withJBHDecimals(new BigDecimal("0.00")))
            .incomeWithholdingTaxAmount(withJBHDecimals(new BigDecimal("0.00")))
            .monthlyNetProfit(withJBHDecimals(new BigDecimal("0.00")))
            .totalMovements(3)
            .build();

    assertMonthlyBalance(expectedMonthlyBalance, actualMonthlyBalance);
  }

  @Test
  @Order(3)
  void registerExpenses202302() throws AccountBusinessException {
    final YearMonth currentPeriod = period.plusMonths(1);
    final LocalDate mvmDate = currentPeriod.atDay(10);
    final var personalExpense = new BigDecimal("100.00");
    addMovementUseCase.addMovement(
        userId, creditCardAccount.id(), createPersonalExpense(mvmDate, personalExpense));

    final AccountDTO updatedAccount =
        inMemoryAccountRepo.findByAccountId(creditCardAccount.id()).get();
    assertNotNull(updatedAccount);

    final var expectedAccountBuilder =
        AccountEntityBuilder.withBuilder(
            creditCardAccount.id(),
            userId,
            new BigDecimal("-800.00"),
            new BigDecimal("-800.00"),
            creditCardAccount.name(),
            creditCardAccount.type());

    final AccountDTO expectedAccount = AccountMapper.toDTO(expectedAccountBuilder.build());

    assertAccount(expectedAccount, updatedAccount);

    UseCaseBuilder.delayTests();
    LOG.info("Checking Monthly Balance for period in test {} ", period);

    final MonthlyBalanceDTO actualMonthlyBalance =
        monthlyBalanceService.findByAccountIdAndPeriod(creditCardAccount.id(), currentPeriod).get();
    final MonthlyBalanceDTO expectedMonthlyBalance =
        EntityTestBuilder.withClosingBalance(
                creditCardAccount.id(), currentPeriod, withJBHDecimals(new BigDecimal("-100.00")))
            .totalDebits(withJBHDecimals(new BigDecimal("0.00")))
            .totalCredits(withJBHDecimals(new BigDecimal("100.00")))
            .movementBalance(withJBHDecimals(new BigDecimal("-100")))
            .openingBalance(withJBHDecimals(new BigDecimal("-700.00")))
            .monthlyNetProfit(withJBHDecimals(new BigDecimal("700.00")))
            .totalMovements(1)
            .build();

    assertMonthlyBalance(expectedMonthlyBalance, actualMonthlyBalance);
  }

  @Test
  @Order(4)
  void registerMoreThanCreditLimit202301() {
    final LocalDate mvmDate = period.atDay(10);
    final var personalExpense = new BigDecimal("300.00");

    assertThrows(
        AccountBusinessException.class,
        () -> {
          addMovementUseCase.addMovement(
              userId, creditCardAccount.id(), createPersonalExpense(mvmDate, personalExpense));
        });
  }

  @Test
  @Order(5)
  void payExpenses202302() throws AccountBusinessException {
    final YearMonth currentPeriod = period.plusMonths(1);
    final LocalDate mvmDate = currentPeriod.atDay(10);
    final var personalExpense = new BigDecimal("800.00");
    addMovementUseCase.addMovement(
        userId, creditCardAccount.id(), createDepositIncome(mvmDate, personalExpense));

    final AccountDTO updatedAccount =
        inMemoryAccountRepo.findByAccountId(creditCardAccount.id()).get();
    assertNotNull(updatedAccount);

    final var expectedAccountBuilder =
        AccountEntityBuilder.withBuilder(
            creditCardAccount.id(),
            userId,
            new BigDecimal("0.00"),
            new BigDecimal("0.00"),
            creditCardAccount.name(),
            creditCardAccount.type());

    final AccountDTO expectedAccount = AccountMapper.toDTO(expectedAccountBuilder.build());

    assertAccount(expectedAccount, updatedAccount);

    UseCaseBuilder.delayTests();
    LOG.info("Checking Monthly Balance for period in test {} ", period);

    final MonthlyBalanceDTO actualMonthlyBalance =
        monthlyBalanceService.findByAccountIdAndPeriod(creditCardAccount.id(), currentPeriod).get();
    final MonthlyBalanceDTO expectedMonthlyBalance =
        EntityTestBuilder.withClosingBalance(
                creditCardAccount.id(), currentPeriod, withJBHDecimals(new BigDecimal("700.00")))
            .totalDebits(withJBHDecimals(new BigDecimal("800.00")))
            .totalCredits(withJBHDecimals(new BigDecimal("100.00")))
            .movementBalance(withJBHDecimals(new BigDecimal("700")))
            .openingBalance(withJBHDecimals(new BigDecimal("-700.00")))
            .monthlyNetProfit(withJBHDecimals(new BigDecimal("700.00")))
            .totalMovements(2)
            .build();

    assertMonthlyBalance(expectedMonthlyBalance, actualMonthlyBalance);
  }
}
