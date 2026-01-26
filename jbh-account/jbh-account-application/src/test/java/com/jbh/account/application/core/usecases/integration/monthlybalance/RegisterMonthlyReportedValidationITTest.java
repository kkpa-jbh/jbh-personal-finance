package com.jbh.account.application.core.usecases.integration.monthlybalance;

import static com.jbh.account.application.builders.CommandTestBuilder.createBasicAccountCommand;
import static com.jbh.account.application.core.usecases.utils.MonthlyBalanceITUtils.createMonthlyBalanceCommand;
import static com.jbh.commons.util.JbhMoneyUtils.withJBHDecimals;

import com.jbh.account.application.builders.UseCaseBuilder;
import com.jbh.account.application.core.dto.ProductDTO;
import com.jbh.account.application.core.ports.output.account.InMemoryAccountRepository;
import com.jbh.account.application.core.services.monthlybalance.MonthlyBalanceService;
import com.jbh.account.application.core.usecases.CreateProductUseCase;
import com.jbh.account.application.core.usecases.RegisterMonthlyBalanceUseCase;
import com.jbh.account.application.core.vo.commands.AddMonthlyBalanceCommand;
import com.jbh.account.application.core.vo.commands.MonthlyBalanceCommandVO;
import com.jbh.account.application.movements.ports.output.AccountMovementWriterRepository;
import com.jbh.account.domain.vo.ProductId;
import com.jbh.account.domain.vo.ProductType;
import com.jbh.commons.exception.BusinessException;
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
  private static final String ACCOUNT_REPORTED = "Reported";
  private static final YearMonth reportedPeriod = YearMonth.of(2024, 7);
  private static final LocalDate runningDate = LocalDate.now();
  private static MonthlyBalanceService monthlyBalanceService;
  private static InMemoryAccountRepository inMemoryAccountRepo;
  private static RegisterMonthlyBalanceUseCase useCaseTest;
  private static CreateProductUseCase createAccountUseCase;
  private static ProductId accountId;
  @Mock private AccountMovementWriterRepository accountMovementRepository;

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
            createBasicAccountCommand(userId, ACCOUNT_REPORTED, ProductType.SAVINGS));
    accountId = accountDTO.id();
    LOG.info("Account created with id {}", accountId);
    assert accountId != null;

    // Creating Initial Balance
    final var initialBalance = withJBHDecimals(new BigDecimal("900"));
    final AddMonthlyBalanceCommand previousCommand =
        createMonthlyBalanceCommand(
            reportedPeriod, new MonthlyBalanceCommandVO(withJBHDecimals(initialBalance), null));

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
            new MonthlyBalanceCommandVO(withJBHDecimals(initialBalance), null));
    final AddMonthlyBalanceCommand existingMonthlyReportCommand =
        createMonthlyBalanceCommand(
            reportedPeriod, new MonthlyBalanceCommandVO(withJBHDecimals(initialBalance), null));

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
