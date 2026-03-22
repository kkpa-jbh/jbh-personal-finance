package com.jbh.finance.test.application.feature.monthlybalance.usecases;

import static com.jbh.commons.util.JbhMoneyUtils.withJBHDecimals;
import static com.jbh.finance.test.testfixtures.builders.commands.GeneralCommandFixtureBuilder.createBasicAccountCommand;
import static com.jbh.finance.test.testfixtures.utils.MonthlyBalanceITUtils.createMonthlyBalanceCommand;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.monthlybalance.commands.AddMonthlyBalanceCommand;
import com.jbh.finance.application.feature.monthlybalance.services.ProcessMonthlyBalanceService;
import com.jbh.finance.application.feature.monthlybalance.usecases.RegisterMonthlyBalanceUseCase;
import com.jbh.finance.application.feature.movement.ports.output.MovementWriterRepository;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.application.feature.product.usecases.CreateProductUseCase;
import com.jbh.finance.domain.product.vo.ProductId;
import com.jbh.finance.domain.product.vo.ProductType;
import com.jbh.finance.test.testfixtures.builders.UseCaseFixtureBuilder;
import com.jbh.finance.test.testfixtures.builders.commands.MonthlyBalanceCommandFixture;
import com.jbh.finance.test.testfixtures.fakes.product.InMemoryProductRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
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
public class RegisterMonthlyReportedValidationTest {
  private static final Logger LOG =
      LoggerFactory.getLogger(RegisterMonthlyReportedValidationTest.class);
  private static final UUID userId = UUID.randomUUID();
  private static final String PRODUCT_REPORTED = "Reported";
  private static final YearMonth reportedPeriod = YearMonth.of(2024, 7);
  private static final LocalDate runningDate = LocalDate.now();
  private static ProcessMonthlyBalanceService monthlyBalanceService;
  private static InMemoryProductRepository inMemoryAccountRepo;
  private static RegisterMonthlyBalanceUseCase useCaseTest;
  private static CreateProductUseCase createAccountUseCase;
  private static ProductId accountId;
  @Mock private MovementWriterRepository accountMovementRepository;

  @BeforeAll
  static void beforeAll() {
    UseCaseFixtureBuilder.resetState();
  }

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    monthlyBalanceService = UseCaseFixtureBuilder.buildProcessMonthlyBalanceSrv();
    inMemoryAccountRepo = UseCaseFixtureBuilder.getProductRepoInMemory();

    useCaseTest =
        UseCaseFixtureBuilder.buildRegisterMonthlyBalanceUseCase(accountMovementRepository);

    createAccountUseCase = UseCaseFixtureBuilder.buildCreateProductUseCase();
  }

  @Test
  @Order(0)
  void registeringOfficialMonthlyBalance() throws BusinessException {
    // Create Account
    final ProductDTO accountDTO =
        createAccountUseCase.execute(
            createBasicAccountCommand(userId, PRODUCT_REPORTED, ProductType.SAVINGS));
    accountId = accountDTO.id();
    LOG.info("Account created with id {}", accountId);
    assert accountId != null;

    // Creating Initial Balance
    final var initialBalance = withJBHDecimals(new BigDecimal("900"));
    final AddMonthlyBalanceCommand previousCommand =
        createMonthlyBalanceCommand(
            reportedPeriod,
            new MonthlyBalanceCommandFixture(withJBHDecimals(initialBalance), null));

    useCaseTest.registerOfficialMonthlyBalance(runningDate, userId, accountId, previousCommand);
  }

  @Test
  @Order(1)
  void shouldThrowExceptionWhenRegisteringExistingOfficialReport() throws BusinessException {

    // Existing Official Report
    final var initialBalance = withJBHDecimals(new BigDecimal("923"));
    final AddMonthlyBalanceCommand nextCommand =
        createMonthlyBalanceCommand(
            reportedPeriod.plusMonths(1),
            new MonthlyBalanceCommandFixture(withJBHDecimals(initialBalance), null));
    final AddMonthlyBalanceCommand existingMonthlyReportCommand =
        createMonthlyBalanceCommand(
            reportedPeriod,
            new MonthlyBalanceCommandFixture(withJBHDecimals(initialBalance), null));

    // Should work the consecutive months
    useCaseTest.registerOfficialMonthlyBalance(runningDate, userId, accountId, nextCommand);

    // Fails when trying to register the existing report (Initial)
    Assertions.assertThrows(
        BusinessException.class,
        () ->
            useCaseTest.registerOfficialMonthlyBalance(
                runningDate, userId, accountId, existingMonthlyReportCommand));
  }
}
