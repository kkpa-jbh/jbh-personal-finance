package com.jbh.finance.test.testfixtures.builders;

import com.jbh.finance.application.async.AsyncTaskExecutorImpl;
import com.jbh.finance.application.feature.category.services.CategoryService;
import com.jbh.finance.application.feature.monthlybalance.ports.input.FindMonthlyBalanceInputPort;
import com.jbh.finance.application.feature.monthlybalance.ports.input.RegisterMonthlyBalanceInputPort;
import com.jbh.finance.application.feature.monthlybalance.ports.output.MonthlyBalanceQueryRepo;
import com.jbh.finance.application.feature.monthlybalance.ports.output.MonthlyBalanceWriterRepo;
import com.jbh.finance.application.feature.monthlybalance.services.MonthlyBalanceLifecycleService;
import com.jbh.finance.application.feature.monthlybalance.services.MonthlyBalanceLifecycleServiceImpl;
import com.jbh.finance.application.feature.monthlybalance.services.ProcessMonthlyBalanceService;
import com.jbh.finance.application.feature.monthlybalance.services.ProcessMonthlyBalanceServiceImpl;
import com.jbh.finance.application.feature.monthlybalance.usecases.FindMonthlyBalanceUseCase;
import com.jbh.finance.application.feature.monthlybalance.usecases.RegisterMonthlyBalanceUseCase;
import com.jbh.finance.application.feature.movement.ports.input.AddMovementInputPort;
import com.jbh.finance.application.feature.movement.ports.input.AddTransferJbhProductsInputPort;
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
import com.jbh.finance.test.testfixtures.usecases.DeleteMovementUCFixture;
import com.jbh.finance.test.testfixtures.utils.UnitOfWorkTest;

public class UseCaseFixtureBuilder {

  public static final String DEFAULT_ACCOUNT_NAME = "Account 1";
  public static final ProductType DEFAULT_ACCOUNT_TYPE = ProductType.SAVINGS;
  // FIXME Centralize the constructors that are using this serviceMock.
  private static final CategoryService categoryServiceMock = buildCategoryServiceMock();
  // Account
  private static final InMemoryProductRepository inMemoryProductRepo =
      new InMemoryProductRepository();
  private static final InMemoryMonthlyBalanceRepositories inMemoryMonthlyBalanceRepos =
      new InMemoryMonthlyBalanceRepositories();
  private static final MonthlyBalanceWriterRepo monthlyBalanceInMemoWriter =
      inMemoryMonthlyBalanceRepos.getWriterRepo();
  private static final MonthlyBalanceQueryRepo monthlyBalanceInMemoQuery =
      inMemoryMonthlyBalanceRepos.getQueryRepo();
  private static final InMemoryMovementQueryRepository movementInMemoQuery =
      new InMemoryMovementQueryRepository();
  private static final MovementWriterRepository movementInMemoWriter =
      new InMemoryMovementRepository(movementInMemoQuery);

  private static CategoryService buildCategoryServiceMock() {
    return new CategoryServiceMock();
  }

  public static CategoryService getCategoryServiceMock() {
    return categoryServiceMock;
  }

  public static InMemoryMovementQueryRepository getMovementInMemoQuery() {
    return movementInMemoQuery;
  }

  // Use Cases

  public static CreateProductUseCase buildCreateProductUseCase() {
    return new CreateProductInputPort(buildProductLifecycleSrv());
  }

  public static ProductLifecycleService buildProductLifecycleSrv() {
    return new ProductLifecycleServiceImpl(getProductRepoInMemory());
  }

  public static InMemoryProductRepository getProductRepoInMemory() {
    return inMemoryProductRepo;
  }

  public static MovementWriterRepository getAccountMovementWriterRepository() {
    return movementInMemoWriter;
  }

  public static RegisterMonthlyBalanceUseCase buildRegisterMonthlyBalanceUseCase(
      final MovementWriterRepository accountMovementRepository) {
    return new RegisterMonthlyBalanceInputPort(
        buildMonthlyBalanceLifecycleSrv(),
        buildProductLifecycleSrv(),
        buildProcessMovementService(accountMovementRepository),
        categoryServiceMock);
  }

  public static MonthlyBalanceLifecycleService buildMonthlyBalanceLifecycleSrv() {
    return new MonthlyBalanceLifecycleServiceImpl(
        monthlyBalanceInMemoQuery, monthlyBalanceInMemoWriter);
  }

  public static ProcessMovementServiceImpl buildProcessMovementService(
      final MovementWriterRepository accountMovementRepository) {
    return new ProcessMovementServiceImpl(
        buildMovementLifeCycleSrv(accountMovementRepository),
        buildProductLifecycleSrv(),
        buildProcessMonthlyBalanceSrv(),
        new UnitOfWorkTest(),
        categoryServiceMock);
  }

  public static MovementLifecycleService buildMovementLifeCycleSrv(
      final MovementWriterRepository accountMovementRepository) {
    return new MovementLifecycleServiceImpl(accountMovementRepository, movementInMemoQuery);
  }

  public static ProcessMonthlyBalanceService buildProcessMonthlyBalanceSrv() {
    return new ProcessMonthlyBalanceServiceImpl(
        buildMonthlyBalanceLifecycleSrv(), new AsyncTaskExecutorImpl(), buildProductLifecycleSrv());
  }

  public static FindMonthlyBalanceUseCase buildFindMonthlyBalanceUseCase() {
    return new FindMonthlyBalanceInputPort(
        buildMonthlyBalanceLifecycleSrv(), buildProductLifecycleSrv());
  }

  public static InMemoryMonthlyBalanceRepositories getInMemoryMonthlyBalanceRepos() {
    return inMemoryMonthlyBalanceRepos;
  }

  public static void delayTests() {
    try {
      Thread.sleep(500);
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

  public static MovementLifecycleService buildMovementLifeCycleSrv() {
    return new MovementLifecycleServiceImpl(movementInMemoWriter, movementInMemoQuery);
  }

  public static LiquidateProductUseCase buildLiquidateAccountUseCase(
      final MovementWriterRepository accountMovementRepository) {
    return new LiquidateProductInputPort(
        buildProductLifecycleSrv(),
        buildProcessMovementService(accountMovementRepository),
        categoryServiceMock);
  }

  public static DeleteMovementUseCase buildDeleteMovementUseCase() {
    return new DeleteMovementUCFixture(
        buildMovementLifeCycleSrv(movementInMemoWriter),
        buildProductLifecycleSrv(),
        buildProcessMovementService(movementInMemoWriter));
  }

  public static AddMovementUseCase buildAddMovementUseCase() {
    return new AddMovementInputPort(
        buildProcessMovementService(movementInMemoWriter), buildProductLifecycleSrv());
  }

  public static AddMovementUseCase buildAddMovementUseCase(
      final MovementWriterRepository accountMovementRepository) {
    return new AddMovementInputPort(
        buildProcessMovementService(accountMovementRepository), buildProductLifecycleSrv());
  }

  public static UpdateProductUseCase buildUpdateProductUseCase() {
    return new UpdateProductInputPort(buildProductLifecycleSrv());
  }

  public static MonthlyBalanceQueryRepo getInMemoryMonthlyBalanceQueryRepo() {
    return monthlyBalanceInMemoQuery;
  }

  public static void resetState() {
    inMemoryProductRepo.clearStorage();
    inMemoryMonthlyBalanceRepos.clearStorage();
    movementInMemoQuery.clearStorage();
  }
}
