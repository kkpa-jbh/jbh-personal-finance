package com.jbh.finance.application.core.usecases.integration.monthlybalance;

import static com.jbh.commons.util.JbhMoneyUtils.withJBHDecimals;
import static com.jbh.finance.application.builders.CommandTestBuilder.createBasicAccountCommand;
import static com.jbh.finance.application.core.usecases.utils.MonthlyBalanceITUtils.createMonthlyBalanceCommand;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.builders.UseCaseBuilder;
import com.jbh.finance.application.builders.commands.MonthlyBalanceCommandTest;
import com.jbh.finance.application.core.ports.output.product.InMemoryProductRepository;
import com.jbh.finance.application.feature.monthlybalance.commands.AddMonthlyBalanceCommand;
import com.jbh.finance.application.feature.monthlybalance.services.MonthlyBalanceLifecycleService;
import com.jbh.finance.application.feature.monthlybalance.usecases.RegisterMonthlyBalanceUseCase;
import com.jbh.finance.application.feature.movement.ports.output.MovementWriterRepository;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.application.feature.product.usecases.CreateProductUseCase;
import com.jbh.finance.domain.product.vo.ProductId;
import com.jbh.finance.domain.product.vo.ProductType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
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
public class RegisterMonthlyReportedValidationITTest {
  private static final Logger LOG =
      LoggerFactory.getLogger(RegisterMonthlyReportedValidationITTest.class);
  private static final UUID userId = UUID.randomUUID();
  private static final String PRODUCT_REPORTED = "Reported";
  private static final YearMonth reportedPeriod = YearMonth.of(2024, 7);
  private static final LocalDate runningDate = LocalDate.now();
  private static MonthlyBalanceLifecycleService monthlyBalanceService;
  private static InMemoryProductRepository inMemoryAccountRepo;
  private static RegisterMonthlyBalanceUseCase useCaseTest;
  private static CreateProductUseCase createAccountUseCase;
  private static ProductId accountId;
  @Mock private MovementWriterRepository accountMovementRepository;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    monthlyBalanceService = UseCaseBuilder.buildMonthlyBalanceService();
    inMemoryAccountRepo = UseCaseBuilder.getAccountRepository();

    useCaseTest = UseCaseBuilder.buildRegisterMonthlyBalanceUseCase(accountMovementRepository);

    createAccountUseCase = UseCaseBuilder.buildCreateAccountUseCase();
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
            reportedPeriod, new MonthlyBalanceCommandTest(withJBHDecimals(initialBalance), null));

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
            new MonthlyBalanceCommandTest(withJBHDecimals(initialBalance), null));
    final AddMonthlyBalanceCommand existingMonthlyReportCommand =
        createMonthlyBalanceCommand(
            reportedPeriod, new MonthlyBalanceCommandTest(withJBHDecimals(initialBalance), null));

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
