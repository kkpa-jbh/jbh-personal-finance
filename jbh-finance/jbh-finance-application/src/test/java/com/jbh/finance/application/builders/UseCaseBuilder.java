package com.jbh.finance.application.builders;

import com.jbh.finance.application.async.AsyncTaskExecutorImpl;
import com.jbh.finance.application.core.ports.output.account.InMemoryAccountRepository;
import com.jbh.finance.application.core.ports.output.monthlybalance.InMemoryMonthlyBalanceRepositories;
import com.jbh.finance.application.core.ports.output.movement.InMemoryAccountMovementQueryRepository;
import com.jbh.finance.application.core.ports.output.movement.InMemoryAccountMovementRepository;
import com.jbh.finance.application.core.usecases.utils.UnitOfWorkTest;
import com.jbh.finance.application.feature.monthlybalance.ports.input.FindMonthlyBalanceInputPort;
import com.jbh.finance.application.feature.monthlybalance.ports.input.RegisterMonthlyBalanceInputPort;
import com.jbh.finance.application.feature.monthlybalance.ports.output.AccountMonthlyBalanceQueryRepo;
import com.jbh.finance.application.feature.monthlybalance.ports.output.AccountMonthlyBalanceWriterRepository;
import com.jbh.finance.application.feature.monthlybalance.services.MonthlyBalanceService;
import com.jbh.finance.application.feature.monthlybalance.services.MonthlyBalanceServiceImpl;
import com.jbh.finance.application.feature.monthlybalance.usecases.FindMonthlyBalanceUseCase;
import com.jbh.finance.application.feature.monthlybalance.usecases.RegisterMonthlyBalanceUseCase;
import com.jbh.finance.application.feature.movement.ports.input.AddMovementInputPort;
import com.jbh.finance.application.feature.movement.ports.input.AddTransferJbhAccountsInputPort;
import com.jbh.finance.application.feature.movement.ports.input.LiquidateAccountInputPort;
import com.jbh.finance.application.feature.movement.ports.output.AccountMovementWriterRepository;
import com.jbh.finance.application.feature.movement.services.AccountMovementApplicationServiceImpl;
import com.jbh.finance.application.feature.movement.services.AccountMovementService;
import com.jbh.finance.application.feature.movement.services.AccountMovementServiceImpl;
import com.jbh.finance.application.feature.movement.usecases.AddMovementUseCase;
import com.jbh.finance.application.feature.movement.usecases.AddTransferJbhAccountsUseCase;
import com.jbh.finance.application.feature.movement.usecases.LiquidateAccountUseCase;
import com.jbh.finance.application.feature.product.ports.input.CreateProductInputPort;
import com.jbh.finance.application.feature.product.ports.input.UpdateProductInputPort;
import com.jbh.finance.application.feature.product.services.ProductServiceImpl;
import com.jbh.finance.application.feature.product.services.ProductsService;
import com.jbh.finance.application.feature.product.usecases.CreateProductUseCase;
import com.jbh.finance.application.feature.product.usecases.UpdateProductUseCase;
import com.jbh.finance.domain.product.vo.ProductType;

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

  public static ProductsService buildAccountService() {
    return new ProductServiceImpl(getAccountRepository());
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
