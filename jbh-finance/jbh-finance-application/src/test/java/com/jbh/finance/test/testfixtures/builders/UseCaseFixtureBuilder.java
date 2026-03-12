package com.jbh.finance.test.testfixtures.builders;

import com.jbh.finance.application.async.AsyncTaskExecutorImpl;
import com.jbh.finance.application.feature.category.services.CategoryService;
import com.jbh.finance.application.feature.monthlybalance.ports.input.FindMonthlyBalanceInputPort;
import com.jbh.finance.application.feature.monthlybalance.ports.input.RegisterMonthlyBalanceInputPort;
import com.jbh.finance.application.feature.monthlybalance.ports.output.MonthlyBalanceQueryRepo;
import com.jbh.finance.application.feature.monthlybalance.ports.output.MonthlyBalanceWriterRepo;
import com.jbh.finance.application.feature.monthlybalance.services.MonthlyBalanceLifecycleService;
import com.jbh.finance.application.feature.monthlybalance.services.MonthlyBalanceLifecycleServiceImpl;
import com.jbh.finance.application.feature.monthlybalance.usecases.FindMonthlyBalanceUseCase;
import com.jbh.finance.application.feature.monthlybalance.usecases.RegisterMonthlyBalanceUseCase;
import com.jbh.finance.application.feature.movement.ports.input.AddMovementInputPort;
import com.jbh.finance.application.feature.movement.ports.input.AddTransferJbhProductsInputPort;
import com.jbh.finance.application.feature.movement.ports.input.DeleteMovementInputPort;
import com.jbh.finance.application.feature.movement.ports.input.LiquidateProductInputPort;
import com.jbh.finance.application.feature.movement.ports.output.MovementWriterRepository;
import com.jbh.finance.application.feature.movement.services.MovementLifecycleService;
import com.jbh.finance.application.feature.movement.services.MovementLifecycleServiceImpl;
import com.jbh.finance.application.feature.movement.services.ProcessMovementServiceImpl;
import com.jbh.finance.application.feature.movement.usecases.AddMovementUseCase;
import com.jbh.finance.application.feature.movement.usecases.AddTransferJbhProductsUseCase;
import com.jbh.finance.application.feature.movement.usecases.DeleteMovementUseCase;
import com.jbh.finance.application.feature.movement.usecases.LiquidateProductUseCase;
import com.jbh.finance.application.feature.product.ports.input.CreateProductInputPort;
import com.jbh.finance.application.feature.product.ports.input.UpdateProductInputPort;
import com.jbh.finance.application.feature.product.services.ProductLifecycleService;
import com.jbh.finance.application.feature.product.services.ProductLifecycleServiceImpl;
import com.jbh.finance.application.feature.product.usecases.CreateProductUseCase;
import com.jbh.finance.application.feature.product.usecases.UpdateProductUseCase;
import com.jbh.finance.domain.product.vo.ProductType;
import com.jbh.finance.test.testfixtures.CategoryServiceMock;
import com.jbh.finance.test.testfixtures.fakes.monthlybalance.InMemoryMonthlyBalanceRepositories;
import com.jbh.finance.test.testfixtures.fakes.movement.InMemoryMovementQueryRepository;
import com.jbh.finance.test.testfixtures.fakes.movement.InMemoryMovementRepository;
import com.jbh.finance.test.testfixtures.fakes.product.InMemoryProductRepository;
import com.jbh.finance.test.testfixtures.utils.UnitOfWorkTest;

public class UseCaseFixtureBuilder {

  public static final String DEFAULT_ACCOUNT_NAME = "Account 1";
  public static final ProductType DEFAULT_ACCOUNT_TYPE = ProductType.SAVINGS;
  // FIXME Centralize the constructors that are using this serviceMock.
  private static final CategoryService categoryServiceMock = buildCategoryServiceMock();
  // Account
  private static final InMemoryProductRepository inMemoryAccountRepo =
      new InMemoryProductRepository();
  private static final InMemoryMonthlyBalanceRepositories inMemoryMonthlyBalanceRepos =
      new InMemoryMonthlyBalanceRepositories();
  static final MonthlyBalanceWriterRepo monthlyBalanceInMemoWriter =
      inMemoryMonthlyBalanceRepos.getWriterRepo();
  static final MonthlyBalanceQueryRepo monthlyBalanceInMemoQuery =
      inMemoryMonthlyBalanceRepos.getQueryRepo();
  public static InMemoryMovementQueryRepository movementInMemoQuery =
      new InMemoryMovementQueryRepository();
  static final MovementWriterRepository movementInMemoWriter =
      new InMemoryMovementRepository(movementInMemoQuery);
  public static final AddMovementUseCase addMovementUseCase =
      buildAddMovementUseCase(movementInMemoWriter);

  private static CategoryService buildCategoryServiceMock() {
    return new CategoryServiceMock();
  }

  public static CategoryService getCategoryServiceMock() {
    return categoryServiceMock;
  }

  // Use Cases

  public static CreateProductUseCase buildCreateAccountUseCase() {
    return new CreateProductInputPort(buildProductLifecycleSrv());
  }

  public static ProductLifecycleService buildProductLifecycleSrv() {
    return new ProductLifecycleServiceImpl(getProductRepoInMemory());
  }

  public static InMemoryProductRepository getProductRepoInMemory() {
    return inMemoryAccountRepo;
  }

  public static MovementWriterRepository getAccountMovementWriterRepository() {
    return movementInMemoWriter;
  }

  public static RegisterMonthlyBalanceUseCase buildRegisterMonthlyBalanceUseCase(
      final MovementWriterRepository accountMovementRepository) {
    return new RegisterMonthlyBalanceInputPort(
        buildMonthlyBalanceService(),
        buildProductLifecycleSrv(),
        buildProcessMovementService(accountMovementRepository),
        categoryServiceMock);
  }

  public static AddMovementUseCase buildAddMovementUseCase(
      final MovementWriterRepository accountMovementRepository) {
    return new AddMovementInputPort(
        buildProcessMovementService(accountMovementRepository), buildProductLifecycleSrv());
  }

  public static FindMonthlyBalanceUseCase buildFindMonthlyBalanceUseCase() {
    return new FindMonthlyBalanceInputPort(
        buildMonthlyBalanceService(), buildProductLifecycleSrv());
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

  public static AddTransferJbhProductsUseCase buildAddTransferUseCase(
      final MovementWriterRepository accountMovementRepository) {
    return new AddTransferJbhProductsInputPort(
        buildProductLifecycleSrv(),
        buildProcessMovementService(accountMovementRepository),
        categoryServiceMock);
  }

  public static ProcessMovementServiceImpl buildProcessMovementService(
      final MovementWriterRepository accountMovementRepository) {
    return new ProcessMovementServiceImpl(
        buildMovementLifeCycleSrv(accountMovementRepository),
        buildProductLifecycleSrv(),
        buildMonthlyBalanceService(),
        new UnitOfWorkTest(),
        categoryServiceMock);
  }

  public static MovementLifecycleService buildMovementLifeCycleSrv(
      final MovementWriterRepository accountMovementRepository) {
    return new MovementLifecycleServiceImpl(accountMovementRepository, movementInMemoQuery);
  }

  public static MonthlyBalanceLifecycleService buildMonthlyBalanceService() {
    return new MonthlyBalanceLifecycleServiceImpl(
        monthlyBalanceInMemoQuery,
        monthlyBalanceInMemoWriter,
        new AsyncTaskExecutorImpl(),
        buildProductLifecycleSrv());
  }

  public static LiquidateProductUseCase buildLiquidateAccountUseCase(
      final MovementWriterRepository accountMovementRepository) {
    return new LiquidateProductInputPort(
        buildProductLifecycleSrv(),
        buildProcessMovementService(accountMovementRepository),
        categoryServiceMock);
  }

  public static DeleteMovementUseCase buildDeleteMovementUseCase() {
    return new DeleteMovementInputPort(
        buildMovementLifeCycleSrv(movementInMemoWriter),
        buildProductLifecycleSrv(),
        buildProcessMovementService(movementInMemoWriter));
  }

  public static UpdateProductUseCase buildUpdateProductUseCase() {
    return new UpdateProductInputPort(buildProductLifecycleSrv());
  }
}
