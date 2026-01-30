package com.jbh.products.application.builders;

import com.jbh.products.application.async.AsyncTaskExecutorImpl;
import com.jbh.products.application.core.ports.input.AddMovementInputPort;
import com.jbh.products.application.core.ports.input.AddTransferJbhAccountsInputPort;
import com.jbh.products.application.core.ports.input.CreateProductInputPort;
import com.jbh.products.application.core.ports.input.FindMonthlyBalanceInputPort;
import com.jbh.products.application.core.ports.input.LiquidateAccountInputPort;
import com.jbh.products.application.core.ports.input.RegisterMonthlyBalanceInputPort;
import com.jbh.products.application.core.ports.input.UpdateProductInputPort;
import com.jbh.products.application.core.ports.output.account.InMemoryAccountRepository;
import com.jbh.products.application.core.ports.output.monthlybalance.AccountMonthlyBalanceQueryRepo;
import com.jbh.products.application.core.ports.output.monthlybalance.AccountMonthlyBalanceWriterRepository;
import com.jbh.products.application.core.ports.output.monthlybalance.InMemoryMonthlyBalanceRepositories;
import com.jbh.products.application.core.ports.output.movement.InMemoryAccountMovementQueryRepository;
import com.jbh.products.application.core.ports.output.movement.InMemoryAccountMovementRepository;
import com.jbh.products.application.core.services.account.AccountService;
import com.jbh.products.application.core.services.account.AccountServiceImpl;
import com.jbh.products.application.core.services.monthlybalance.MonthlyBalanceService;
import com.jbh.products.application.core.services.monthlybalance.MonthlyBalanceServiceImpl;
import com.jbh.products.application.core.services.movements.AccountMovementApplicationServiceImpl;
import com.jbh.products.application.core.services.movements.AccountMovementService;
import com.jbh.products.application.core.services.movements.AccountMovementServiceImpl;
import com.jbh.products.application.core.usecases.AddMovementUseCase;
import com.jbh.products.application.core.usecases.AddTransferJbhAccountsUseCase;
import com.jbh.products.application.core.usecases.CreateProductUseCase;
import com.jbh.products.application.core.usecases.FindMonthlyBalanceUseCase;
import com.jbh.products.application.core.usecases.LiquidateAccountUseCase;
import com.jbh.products.application.core.usecases.RegisterMonthlyBalanceUseCase;
import com.jbh.products.application.core.usecases.UpdateProductUseCase;
import com.jbh.products.application.core.usecases.utils.UnitOfWorkTest;
import com.jbh.products.application.movements.ports.output.AccountMovementWriterRepository;
import com.jbh.products.domain.vo.ProductType;

public class UseCaseBuilder {

  public static final String DEFAULT_ACCOUNT_NAME = "Account 1";
  public static final ProductType DEFAULT_ACCOUNT_TYPE = ProductType.SAVINGS;

  // Account
  private static final InMemoryAccountRepository inMemoryAccountRepo =
      new InMemoryAccountRepository();
  private static final InMemoryMonthlyBalanceRepositories inMemoryMonthlyBalanceRepos =
      new InMemoryMonthlyBalanceRepositories();

  static final AccountMonthlyBalanceWriterRepository monthlyBalanceInMemoWriter =
      inMemoryMonthlyBalanceRepos.getWriterRepo();
  static final AccountMonthlyBalanceQueryRepo monthlyBalanceInMemoQuery =
      inMemoryMonthlyBalanceRepos.getQueryRepo();

  public static InMemoryAccountMovementQueryRepository movementQueryRepository =
      new InMemoryAccountMovementQueryRepository();

  static final AccountMovementWriterRepository movementInMemoWriter =
      new InMemoryAccountMovementRepository(movementQueryRepository);

  public static final AddMovementUseCase addMovementUseCase =
      buildAddMovementUseCase(movementInMemoWriter);

  // Use Cases

  public static CreateProductUseCase buildCreateAccountUseCase() {
    return new CreateProductInputPort(buildAccountService());
  }

  public static AccountService buildAccountService() {
    return new AccountServiceImpl(getAccountRepository());
  }

  public static InMemoryAccountRepository getAccountRepository() {
    return inMemoryAccountRepo;
  }

  public static AccountMovementWriterRepository getAccountMovementWriterRepository() {
    return movementInMemoWriter;
  }

  public static RegisterMonthlyBalanceUseCase buildRegisterMonthlyBalanceUseCase(
      final AccountMovementWriterRepository accountMovementRepository) {
    return new RegisterMonthlyBalanceInputPort(
        buildMonthlyBalanceService(),
        buildAccountService(),
        buildAccountMovementApplicationService(accountMovementRepository));
  }

  public static AddMovementUseCase buildAddMovementUseCase(
      final AccountMovementWriterRepository accountMovementRepository) {
    return new AddMovementInputPort(
        buildAccountMovementApplicationService(accountMovementRepository), buildAccountService());
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

  public static AddTransferJbhAccountsUseCase buildAddTransferUseCase(
      final AccountMovementWriterRepository accountMovementRepository) {
    return new AddTransferJbhAccountsInputPort(
        buildAccountService(), buildAccountMovementApplicationService(accountMovementRepository));
  }

  public static AccountMovementApplicationServiceImpl buildAccountMovementApplicationService(
      final AccountMovementWriterRepository accountMovementRepository) {
    return new AccountMovementApplicationServiceImpl(
        buildAccountMovementService(accountMovementRepository),
        buildAccountService(),
        buildMonthlyBalanceService(),
        new UnitOfWorkTest());
  }

  public static AccountMovementService buildAccountMovementService(
      final AccountMovementWriterRepository accountMovementRepository) {
    return new AccountMovementServiceImpl(accountMovementRepository, movementQueryRepository);
  }

  public static MonthlyBalanceService buildMonthlyBalanceService() {
    return new MonthlyBalanceServiceImpl(
        monthlyBalanceInMemoQuery,
        monthlyBalanceInMemoWriter,
        new AsyncTaskExecutorImpl(),
        buildAccountService());
  }

  public static LiquidateAccountUseCase buildLiquidateAccountUseCase(
      final AccountMovementWriterRepository accountMovementRepository) {
    return new LiquidateAccountInputPort(
        buildAccountService(), buildAccountMovementApplicationService(accountMovementRepository));
  }

  public static UpdateProductUseCase buildUpdateProductUseCase() {
    return new UpdateProductInputPort(buildAccountService());
  }
}
