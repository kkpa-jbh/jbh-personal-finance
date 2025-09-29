package com.jbh.account.application.core.usecases.integration.monthlybalance;

import static com.jbh.account.domain.utils.MoneyUtils.withJBHDecimals;

import com.jbh.account.application.core.dto.AccountDTO;
import com.jbh.account.application.core.ports.output.account.InMemoryAccountRepository;
import com.jbh.account.application.core.services.monthlybalance.MonthlyBalanceService;
import com.jbh.account.application.core.usecases.AddMovementUseCase;
import com.jbh.account.application.core.usecases.CreateAccountUseCase;
import com.jbh.account.application.core.usecases.RegisterMonthlyBalanceUseCase;
import com.jbh.account.application.core.usecases.UseCaseBuilder;
import com.jbh.account.application.core.vo.commands.AddMonthlyBalanceCommand;
import com.jbh.account.application.core.vo.commands.CreateBasicAccountCommand;
import com.jbh.account.application.core.vo.commands.MonthlyBalanceCommandVO;
import com.jbh.account.application.exceptions.JbhSpecificationApplication;
import com.jbh.account.application.movements.ports.output.AccountMovementRepository;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountType;
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
  private static CreateAccountUseCase createAccountUseCase;
  private static AddMovementUseCase addMovementUseCase;
  private static AccountId accountId;
  @Mock private AccountMovementRepository accountMovementRepository;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    monthlyBalanceService = UseCaseBuilder.buildMonthlyBalanceService();
    inMemoryAccountRepo = UseCaseBuilder.getAccountRepository();

    useCaseTest = UseCaseBuilder.buildRegisterMonthlyBalanceUseCase(accountMovementRepository);

    createAccountUseCase = UseCaseBuilder.buildCreateAccountUseCase();

    addMovementUseCase = UseCaseBuilder.buildAddMovementUseCase(accountMovementRepository);
  }

  @Test
  @Order(0)
  void registeringOfficialMonthlyBalance() throws JbhSpecificationApplication {
    // Create Account
    final AccountDTO accountDTO =
        createAccountUseCase.execute(
            new CreateBasicAccountCommand(userId, ACCOUNT_REPORTED, AccountType.SAVINGS));
    accountId = accountDTO.id();
    LOG.info("Account created with id {}", accountId);
    assert accountId != null;

    // Creating Initial Balance
    final var initialBalance = withJBHDecimals(new BigDecimal("900"));
    final AddMonthlyBalanceCommand previousCommand =
        new AddMonthlyBalanceCommand(
            reportedPeriod, new MonthlyBalanceCommandVO(withJBHDecimals(initialBalance), null));

    useCaseTest.registerOfficialMonthlyBalance(runningDate, userId, accountId, previousCommand);
  }

  @Test
  @Order(1)
  void shouldThrowExceptionWhenRegisteringExistingOfficialReport()
      throws JbhSpecificationApplication {

    // Existing Official Report
    final var initialBalance = withJBHDecimals(new BigDecimal("92300"));
    final AddMonthlyBalanceCommand nextCommand =
        new AddMonthlyBalanceCommand(
            reportedPeriod.plusMonths(1),
            new MonthlyBalanceCommandVO(withJBHDecimals(initialBalance), null));
    final AddMonthlyBalanceCommand existingMonthlyReportCommand =
        new AddMonthlyBalanceCommand(
            reportedPeriod, new MonthlyBalanceCommandVO(withJBHDecimals(initialBalance), null));

    // Should work the consecutive months
    useCaseTest.registerOfficialMonthlyBalance(runningDate, userId, accountId, nextCommand);

    // Fails when trying to register the existing report (Initial)
    Assertions.assertThrows(
        JbhSpecificationApplication.class,
        () ->
            useCaseTest.registerOfficialMonthlyBalance(
                runningDate, userId, accountId, existingMonthlyReportCommand));
  }
}
