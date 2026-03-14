package com.jbh.finance.test.application.feature.movement.usecases;

import static com.jbh.commons.util.JbhMoneyUtils.withJBHDecimals;
import static com.jbh.finance.test.testfixtures.builders.commands.AddMovementCommandFixtureBuilder.createPersonalExpense;
import static com.jbh.finance.test.testfixtures.utils.MonthlyBalanceITUtils.assertMonthlyBalance;
import static com.jbh.finance.test.testfixtures.utils.ProductITUtils.assertProduct;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.finance.application.feature.monthlybalance.services.MonthlyBalanceLifecycleService;
import com.jbh.finance.application.feature.movement.ports.output.MovementWriterRepository;
import com.jbh.finance.application.feature.movement.usecases.AddMovementUseCase;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.application.feature.product.mappers.ProductMapper;
import com.jbh.finance.application.feature.product.usecases.CreateProductUseCase;
import com.jbh.finance.domain.product.vo.ProductMetadata;
import com.jbh.finance.test.testfixtures.builders.EntityTestBuilder;
import com.jbh.finance.test.testfixtures.builders.ProductEntityBuilder;
import com.jbh.finance.test.testfixtures.builders.UseCaseFixtureBuilder;
import com.jbh.finance.test.testfixtures.builders.commands.AddMovementCommandFixtureBuilder;
import com.jbh.finance.test.testfixtures.builders.commands.GeneralCommandFixtureBuilder;
import com.jbh.finance.test.testfixtures.fakes.product.InMemoryProductRepository;
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
public class RegisterTCMovementTest {

  public static final BigDecimal CREDIT_LIMIT = new BigDecimal("1000.00");
  static final UUID userId = UUID.randomUUID();
  private static final Logger LOG = LoggerFactory.getLogger(RegisterTCMovementTest.class);
  private static final InMemoryProductRepository inMemoryAccountRepo =
      UseCaseFixtureBuilder.getProductRepoInMemory();
  private static final String name = "CREDIT CARD";
  private static final YearMonth period = YearMonth.of(2023, 1);
  private static CreateProductUseCase createAccountUseCase;
  private static AddMovementUseCase addMovementUseCase;
  @Mock private static MovementWriterRepository accountMovementRepository;
  private static ProductDTO creditCardAccount;
  private MonthlyBalanceLifecycleService monthlyBalanceService;

  @BeforeAll
  static void beforeAll() {
    UseCaseFixtureBuilder.resetState();
  }

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    createAccountUseCase = UseCaseFixtureBuilder.buildCreateProductUseCase();
    addMovementUseCase = UseCaseFixtureBuilder.buildAddMovementUseCase(accountMovementRepository);
    monthlyBalanceService = UseCaseFixtureBuilder.buildMonthlyBalanceService();

    UseCaseFixtureBuilder.delayTests();
  }

  @Test
  @Order(0)
  void createAccount() throws BusinessException {
    final LocalDate mvmDate = period.atDay(1);
    final ProductMetadata metadata = ProductMetadata.empty();
    metadata.findCreditCardMetadata().putCreditLimit(CREDIT_LIMIT);
    metadata.findCreditCardMetadata().putPaymentDueDay(15);
    creditCardAccount =
        createAccountUseCase.execute(
            GeneralCommandFixtureBuilder.createCreditCardCommand(userId, name, metadata));
    assertNotNull(creditCardAccount);
    assertNotNull(creditCardAccount.id());

    final var personalExpense = new BigDecimal("100.00");
    addMovementUseCase.addMovement(
        userId, creditCardAccount.id(), createPersonalExpense(mvmDate, personalExpense));

    final ProductDTO updatedAccount =
        inMemoryAccountRepo.findByProductId(creditCardAccount.id()).get();
    assertNotNull(updatedAccount);

    final var expectedAccountBuilder =
        ProductEntityBuilder.withBuilder(
            creditCardAccount.id(),
            userId,
            personalExpense.negate(),
            personalExpense.negate(),
            creditCardAccount.name(),
            creditCardAccount.type());

    final ProductDTO expectedAccount = ProductMapper.toDTO(expectedAccountBuilder.build());

    assertProduct(expectedAccount, updatedAccount);
  }

  @Test
  @Order(1)
  void registerExpense202301() throws BusinessException {
    final LocalDate mvmDate = period.atDay(10);
    final var personalExpense = new BigDecimal("800.00");
    addMovementUseCase.addMovement(
        userId, creditCardAccount.id(), createPersonalExpense(mvmDate, personalExpense));

    final ProductDTO updatedAccount =
        inMemoryAccountRepo.findByProductId(creditCardAccount.id()).get();
    assertNotNull(updatedAccount);

    final var expectedAccountBuilder =
        ProductEntityBuilder.withBuilder(
            creditCardAccount.id(),
            userId,
            new BigDecimal("-900.00"),
            new BigDecimal("-900.00"),
            creditCardAccount.name(),
            creditCardAccount.type());

    final ProductDTO expectedAccount = ProductMapper.toDTO(expectedAccountBuilder.build());

    assertProduct(expectedAccount, updatedAccount);
  }

  @Test
  @Order(2)
  void registerDeposit202301() throws BusinessException {
    final LocalDate mvmDate = period.atDay(10);
    final var personalExpense = new BigDecimal("200.00");
    addMovementUseCase.addMovement(
        userId,
        creditCardAccount.id(),
        AddMovementCommandFixtureBuilder.createDepositIncome(mvmDate, personalExpense));

    final ProductDTO updatedAccount =
        inMemoryAccountRepo.findByProductId(creditCardAccount.id()).get();
    assertNotNull(updatedAccount);

    final var expectedAccountBuilder =
        ProductEntityBuilder.withBuilder(
            creditCardAccount.id(),
            userId,
            new BigDecimal("-700.00"),
            new BigDecimal("-700.00"),
            creditCardAccount.name(),
            creditCardAccount.type());

    final ProductDTO expectedAccount = ProductMapper.toDTO(expectedAccountBuilder.build());

    assertProduct(expectedAccount, updatedAccount);

    UseCaseFixtureBuilder.delayTests();
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
  void registerExpenses202302() throws BusinessException {
    final YearMonth currentPeriod = period.plusMonths(1);
    final LocalDate mvmDate = currentPeriod.atDay(10);
    final var personalExpense = new BigDecimal("100.00");
    addMovementUseCase.addMovement(
        userId, creditCardAccount.id(), createPersonalExpense(mvmDate, personalExpense));

    final ProductDTO updatedAccount =
        inMemoryAccountRepo.findByProductId(creditCardAccount.id()).get();
    assertNotNull(updatedAccount);

    final var expectedAccountBuilder =
        ProductEntityBuilder.withBuilder(
            creditCardAccount.id(),
            userId,
            new BigDecimal("-800.00"),
            new BigDecimal("-800.00"),
            creditCardAccount.name(),
            creditCardAccount.type());

    final ProductDTO expectedAccount = ProductMapper.toDTO(expectedAccountBuilder.build());

    assertProduct(expectedAccount, updatedAccount);

    UseCaseFixtureBuilder.delayTests();
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
        BusinessException.class,
        () -> {
          addMovementUseCase.addMovement(
              userId, creditCardAccount.id(), createPersonalExpense(mvmDate, personalExpense));
        });
  }

  @Test
  @Order(5)
  void payExpenses202302() throws BusinessException {
    final YearMonth currentPeriod = period.plusMonths(1);
    final LocalDate mvmDate = currentPeriod.atDay(10);
    final var personalExpense = new BigDecimal("800.00");
    addMovementUseCase.addMovement(
        userId,
        creditCardAccount.id(),
        AddMovementCommandFixtureBuilder.createDepositIncome(mvmDate, personalExpense));

    final ProductDTO updatedAccount =
        inMemoryAccountRepo.findByProductId(creditCardAccount.id()).get();
    assertNotNull(updatedAccount);

    final var expectedAccountBuilder =
        ProductEntityBuilder.withBuilder(
            creditCardAccount.id(),
            userId,
            new BigDecimal("0.00"),
            new BigDecimal("0.00"),
            creditCardAccount.name(),
            creditCardAccount.type());

    final ProductDTO expectedAccount = ProductMapper.toDTO(expectedAccountBuilder.build());

    assertProduct(expectedAccount, updatedAccount);

    UseCaseFixtureBuilder.delayTests();
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
