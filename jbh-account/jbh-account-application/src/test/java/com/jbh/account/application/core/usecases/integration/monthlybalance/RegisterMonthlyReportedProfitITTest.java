package com.jbh.account.application.core.usecases.integration.monthlybalance;

import static com.jbh.account.application.core.ports.output.account.InMemoryAccountRepository.DEFAULT_ACCOUNT_NAME;
import static com.jbh.account.application.core.usecases.utils.MonthlyBalanceITUtils.assertMonthlyBalance;
import static com.jbh.account.domain.utils.MoneyUtils.withJBHDecimals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.jbh.account.application.core.dto.AccountDTO;
import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.application.core.ports.input.CreateAccountInputPort;
import com.jbh.account.application.core.ports.input.RegisterMonthlyBalanceInputPort;
import com.jbh.account.application.core.ports.output.account.InMemoryAccountRepository;
import com.jbh.account.application.core.ports.output.monthlybalance.AccountMonthlyBalanceQueryRepo;
import com.jbh.account.application.core.ports.output.monthlybalance.AccountMonthlyBalanceWriterRepository;
import com.jbh.account.application.core.ports.output.monthlybalance.InMemoryMonthlyBalanceRepositories;
import com.jbh.account.application.core.services.account.AccountService;
import com.jbh.account.application.core.services.account.AccountServiceImpl;
import com.jbh.account.application.core.services.monthlybalance.MonthlyBalanceService;
import com.jbh.account.application.core.services.monthlybalance.MonthlyBalanceServiceImpl;
import com.jbh.account.application.core.usecases.CreateAccountUseCase;
import com.jbh.account.application.core.usecases.RegisterMonthlyBalanceUseCase;
import com.jbh.account.application.core.vo.commands.AddMonthlyBalanceCommand;
import com.jbh.account.application.core.vo.commands.CreateBasicAccountCommand;
import com.jbh.account.application.core.vo.commands.MonthlyBalanceCommandVO;
import com.jbh.account.application.exceptions.JbhSpecificationApplication;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegisterMonthlyReportedProfitITTest {
  static final UUID userId = UUID.randomUUID();
  private static final Logger LOG =
      LoggerFactory.getLogger(RegisterMonthlyReportedWithoutProfitITTest.class);
  private static final InMemoryAccountRepository inMemoryAccountRepo =
      new InMemoryAccountRepository();
  static InMemoryMonthlyBalanceRepositories inMemoryMonthlyBalanceRepos =
      new InMemoryMonthlyBalanceRepositories();
  static CreateAccountUseCase createAccountUseCase;
  static AccountDTO createdAccount;
  static AccountId accountId;
  static int commandIndex = -1;
  RegisterMonthlyBalanceUseCase useCase;
  YearMonth initialPeriod = YearMonth.of(2024, 10);
  LocalDate runningDate = LocalDate.now();
  BigDecimal initialBalance = withJBHDecimals(new BigDecimal("1000"));
  List<AddMonthlyBalanceCommand> accountCommands =
      List.of(
          new AddMonthlyBalanceCommand(
              initialPeriod, new MonthlyBalanceCommandVO(new BigDecimal("1000"), null)),
          new AddMonthlyBalanceCommand(
              initialPeriod.plusMonths(1),
              new MonthlyBalanceCommandVO(new BigDecimal("1009"), new BigDecimal("9"))));

  @BeforeAll
  static void beforeAll() {
    inMemoryMonthlyBalanceRepos.clearStorage();
    inMemoryAccountRepo.clearStorage();
  }

  @BeforeEach
  public void setUp() {
    final AccountMonthlyBalanceWriterRepository monthlyBalanceInMemoWriter =
        inMemoryMonthlyBalanceRepos.getWriterRepo();
    final AccountMonthlyBalanceQueryRepo monthlyBalanceInMemoQuery =
        inMemoryMonthlyBalanceRepos.getQueryRepo();
    final MonthlyBalanceService monthlyBalanceService =
        new MonthlyBalanceServiceImpl(monthlyBalanceInMemoQuery, monthlyBalanceInMemoWriter);
    final AccountService accountService = new AccountServiceImpl(inMemoryAccountRepo);
    useCase = new RegisterMonthlyBalanceInputPort(monthlyBalanceService, accountService);

    createAccountUseCase = new CreateAccountInputPort(accountService);
  }

  @Test
  @Order(1)
  void settingInitialBalance() throws JbhSpecificationApplication {
    createdAccount =
        createAccountUseCase.execute(
            new CreateBasicAccountCommand(userId, DEFAULT_ACCOUNT_NAME, AccountType.SAVINGS));
    accountId = createdAccount.id();
    LOG.info("Account created with id {}", accountId);
    assertNotNull(accountId);

    // Creating Initial Balance
    final AddMonthlyBalanceCommand command = accountCommands.get(++commandIndex);
    final AtomicReference<MonthlyBalanceDTO> savedInitialMonthlyBalance = new AtomicReference<>();

    savedInitialMonthlyBalance.set(
        useCase.registerOfficialMonthlyBalance(runningDate, userId, accountId, command));

    // Then
    final MonthlyBalanceDTO expectedInitialBalance =
        MonthlyBalanceDTO.defaultBuilder()
            .closingBalance(initialBalance)
            .accountId(accountId)
            .period(initialPeriod)
            .year(initialPeriod.getYear())
            .month(initialPeriod.getMonthValue())
            .officialMonthlyReport(true)
            .build();
    assertMonthlyBalance(expectedInitialBalance, savedInitialMonthlyBalance.get());
  }
}
