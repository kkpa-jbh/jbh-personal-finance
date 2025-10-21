package com.jbh.account.application.core.usecases;

import com.jbh.account.application.async.AsyncTaskExecutorImpl;
import com.jbh.account.application.core.ports.input.AddMovementInputPort;
import com.jbh.account.application.core.ports.input.CreateAccountInputPort;
import com.jbh.account.application.core.ports.input.FindMonthlyBalanceInputPort;
import com.jbh.account.application.core.ports.input.RegisterMonthlyBalanceInputPort;
import com.jbh.account.application.core.ports.output.account.InMemoryAccountRepository;
import com.jbh.account.application.core.ports.output.monthlybalance.AccountMonthlyBalanceQueryRepo;
import com.jbh.account.application.core.ports.output.monthlybalance.AccountMonthlyBalanceWriterRepository;
import com.jbh.account.application.core.ports.output.monthlybalance.InMemoryMonthlyBalanceRepositories;
import com.jbh.account.application.core.services.account.AccountService;
import com.jbh.account.application.core.services.account.AccountServiceImpl;
import com.jbh.account.application.core.services.monthlybalance.MonthlyBalanceService;
import com.jbh.account.application.core.services.monthlybalance.MonthlyBalanceServiceImpl;
import com.jbh.account.application.core.services.movements.AccountMovementServiceImpl;
import com.jbh.account.application.core.usecases.utils.UnitOfWorkTest;
import com.jbh.account.application.movements.ports.output.AccountMovementRepository;

public class UseCaseBuilder {

  // Account
  private static final InMemoryAccountRepository inMemoryAccountRepo =
      new InMemoryAccountRepository();
  private static final InMemoryMonthlyBalanceRepositories inMemoryMonthlyBalanceRepos =
      new InMemoryMonthlyBalanceRepositories();

  static final AccountMonthlyBalanceWriterRepository monthlyBalanceInMemoWriter =
      inMemoryMonthlyBalanceRepos.getWriterRepo();
  static final AccountMonthlyBalanceQueryRepo monthlyBalanceInMemoQuery =
      inMemoryMonthlyBalanceRepos.getQueryRepo();

  // Use Cases

  public static CreateAccountUseCase buildCreateAccountUseCase() {
    return new CreateAccountInputPort(buildAccountService());
  }

  public static AccountService buildAccountService() {
    return new AccountServiceImpl(getAccountRepository());
  }

  public static InMemoryAccountRepository getAccountRepository() {
    return inMemoryAccountRepo;
  }

  public static RegisterMonthlyBalanceUseCase buildRegisterMonthlyBalanceUseCase(
      final AccountMovementRepository accountMovementRepository) {
    return new RegisterMonthlyBalanceInputPort(
        buildMonthlyBalanceService(),
        buildAccountService(),
        buildAccountMovementService(accountMovementRepository));
  }

  public static AddMovementUseCase buildAddMovementUseCase(
      final AccountMovementRepository accountMovementRepository) {
    return new AddMovementInputPort(buildAccountMovementService(accountMovementRepository));
  }

  public static AccountMovementServiceImpl buildAccountMovementService(
      final AccountMovementRepository accountMovementRepository) {
    return new AccountMovementServiceImpl(
        accountMovementRepository,
        buildAccountService(),
        buildMonthlyBalanceService(),
        new UnitOfWorkTest());
  }

  public static MonthlyBalanceService buildMonthlyBalanceService() {
    return new MonthlyBalanceServiceImpl(
        monthlyBalanceInMemoQuery,
        monthlyBalanceInMemoWriter,
        new AsyncTaskExecutorImpl(),
        buildAccountService());
  }

  public static FindMonthlyBalanceUseCase buildFindMonthlyBalanceUseCase() {
    return new FindMonthlyBalanceInputPort(buildMonthlyBalanceService(), buildAccountService());
  }

  public static InMemoryMonthlyBalanceRepositories getInMemoryMonthlyBalanceRepos() {
    return inMemoryMonthlyBalanceRepos;
  }

  public static void delayTests() {
    try {
      Thread.sleep(200);
    } catch (final InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
