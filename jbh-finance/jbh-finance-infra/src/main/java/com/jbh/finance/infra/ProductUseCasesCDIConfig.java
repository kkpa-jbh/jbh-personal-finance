package com.jbh.finance.infra;

import com.jbh.finance.application.acid.UnitOfWork;
import com.jbh.finance.application.async.AsyncTaskExecutorImpl;
import com.jbh.finance.application.feature.monthlybalance.ports.input.FindMonthlyBalanceInputPort;
import com.jbh.finance.application.feature.monthlybalance.ports.input.RegisterMonthlyBalanceInputPort;
import com.jbh.finance.application.feature.monthlybalance.ports.output.AccountMonthlyBalanceQueryRepo;
import com.jbh.finance.application.feature.monthlybalance.ports.output.AccountMonthlyBalanceWriterRepository;
import com.jbh.finance.application.feature.monthlybalance.services.MonthlyBalanceService;
import com.jbh.finance.application.feature.monthlybalance.services.MonthlyBalanceServiceImpl;
import com.jbh.finance.application.feature.monthlybalance.services.MonthlyBalanceSyncForUploadedMovements;
import com.jbh.finance.application.feature.monthlybalance.usecases.FindMonthlyBalanceUseCase;
import com.jbh.finance.application.feature.monthlybalance.usecases.RegisterMonthlyBalanceUseCase;
import com.jbh.finance.application.feature.movement.ports.input.AddMovementInputPort;
import com.jbh.finance.application.feature.movement.ports.input.AddMovementsUploadedFileInputPort;
import com.jbh.finance.application.feature.movement.ports.input.AddTransferJbhAccountsInputPort;
import com.jbh.finance.application.feature.movement.ports.input.FindMovementsByProductInputPort;
import com.jbh.finance.application.feature.movement.ports.input.LiquidateAccountInputPort;
import com.jbh.finance.application.feature.movement.ports.output.AccountMovementWriterRepository;
import com.jbh.finance.application.feature.movement.ports.output.MovementQueryRepository;
import com.jbh.finance.application.feature.movement.services.AccountMovementServiceImpl;
import com.jbh.finance.application.feature.movement.services.MovementApplicationService;
import com.jbh.finance.application.feature.movement.services.MovementApplicationServiceImpl;
import com.jbh.finance.application.feature.movement.services.MovementService;
import com.jbh.finance.application.feature.movement.usecases.AddTransferJbhAccountsUseCase;
import com.jbh.finance.application.feature.movement.usecases.FindMovementsUseCase;
import com.jbh.finance.application.feature.movement.usecases.LiquidateAccountUseCase;
import com.jbh.finance.application.feature.product.ports.input.CreateProductInputPort;
import com.jbh.finance.application.feature.product.ports.input.DeleteProductInputPort;
import com.jbh.finance.application.feature.product.ports.input.EditProductInputPort;
import com.jbh.finance.application.feature.product.ports.input.FindActiveProductsInputPort;
import com.jbh.finance.application.feature.product.ports.input.GetProductMetadataConfigInputPort;
import com.jbh.finance.application.feature.product.ports.input.UpdateProductInputPort;
import com.jbh.finance.application.feature.product.ports.input.UpdateProductStatusInputPort;
import com.jbh.finance.application.feature.product.ports.output.ProductRepository;
import com.jbh.finance.application.feature.product.services.ProductServiceImpl;
import com.jbh.finance.application.feature.product.services.ProductsService;
import com.jbh.finance.application.feature.product.services.metadata.ProductMetadataConfigRegistry;
import com.jbh.finance.application.feature.product.usecases.DeleteProductUseCase;
import com.jbh.finance.application.feature.product.usecases.EditProductUseCase;
import com.jbh.finance.application.feature.product.usecases.FindProductsUseCase;
import com.jbh.finance.application.feature.product.usecases.GetProductMetadataConfigUseCase;
import com.jbh.finance.application.feature.product.usecases.UpdateProductStatusUseCase;
import com.jbh.finance.application.feature.product.usecases.UpdateProductUseCase;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@SuppressWarnings("PMD.CouplingBetweenObjects")
@ApplicationScoped
@RegisterForReflection(
    targets = {
      CreateProductInputPort.class,
      AddMovementInputPort.class,
      FindActiveProductsInputPort.class,
      EditProductInputPort.class,
      DeleteProductInputPort.class,
      UpdateProductStatusInputPort.class
    })
public class ProductUseCasesCDIConfig {

  @Inject ProductRepository accountRepository;

  @Inject AccountMovementWriterRepository accountMovementWriterRepo;

  @Inject MovementQueryRepository accountMovementQueryRepo;

  @Inject UnitOfWork unitOfWork;

  @Inject
  @Named("monthlyBalanceWriterJPAAdapter")
  AccountMonthlyBalanceWriterRepository monthlyBalanceWriterRepo;

  @Inject
  @Named("monthlyBalanceJPARepository")
  AccountMonthlyBalanceQueryRepo monthlyBalanceQueryRepo;

  @Inject ProductsService productsService;

  @Produces
  @ApplicationScoped
  public CreateProductInputPort registeringCreateAccountUseCase() {
    return new CreateProductInputPort(productsService());
  }

  @Produces
  @ApplicationScoped
  public ProductsService productsService() {
    return new ProductServiceImpl(accountRepository);
  }

  @Produces
  @ApplicationScoped
  public AddMovementInputPort registeringAddMovementUseCase() {
    return new AddMovementInputPort(accountMovementServiceApplication(), productsService());
  }

  @Produces
  @ApplicationScoped
  public MovementApplicationService accountMovementServiceApplication() {
    return new MovementApplicationServiceImpl(
        accountMovementService(), productsService(), monthlyBalanceService(), unitOfWork);
  }

  @Produces
  public MovementService accountMovementService() {
    return new AccountMovementServiceImpl(accountMovementWriterRepo, accountMovementQueryRepo);
  }

  @Produces
  @ApplicationScoped
  public MonthlyBalanceService monthlyBalanceService() {
    return new MonthlyBalanceServiceImpl(
        monthlyBalanceQueryRepo,
        monthlyBalanceWriterRepo,
        new AsyncTaskExecutorImpl(),
        productsService());
  }

  @Produces
  @ApplicationScoped
  public AddMovementsUploadedFileInputPort registeringAddMovementsUploadedFileUseCase() {
    return new AddMovementsUploadedFileInputPort(
        productsService(),
        accountMovementWriterRepo,
        unitOfWork,
        uploadedMovementsBalanceSynchronizer());
  }

  @Produces
  @ApplicationScoped
  public MonthlyBalanceSyncForUploadedMovements uploadedMovementsBalanceSynchronizer() {
    return new MonthlyBalanceSyncForUploadedMovements(monthlyBalanceService());
  }

  @Produces
  @ApplicationScoped
  public FindMonthlyBalanceUseCase findMonthlyBalanceUseCase() {
    return new FindMonthlyBalanceInputPort(monthlyBalanceService(), productsService());
  }

  @Produces
  @ApplicationScoped
  public GetProductMetadataConfigUseCase getProductMetadataConfigUseCase() {
    return new GetProductMetadataConfigInputPort(productMetadataConfigRegistry());
  }

  @Produces
  @ApplicationScoped
  public ProductMetadataConfigRegistry productMetadataConfigRegistry() {
    return new ProductMetadataConfigRegistry();
  }

  @Produces
  @ApplicationScoped
  public FindProductsUseCase findActiveProductsUseCase() {
    return new FindActiveProductsInputPort(productsService());
  }

  @Produces
  @ApplicationScoped
  public EditProductUseCase editProductUseCase() {
    return new EditProductInputPort(productsService());
  }

  @Produces
  @ApplicationScoped
  public DeleteProductUseCase deleteProductUseCase() {
    return new DeleteProductInputPort(productsService());
  }

  @Produces
  @ApplicationScoped
  public UpdateProductStatusUseCase updateProductStatusUseCase() {
    return new UpdateProductStatusInputPort(productsService());
  }

  @Produces
  @ApplicationScoped
  public AddTransferJbhAccountsUseCase addTransferJbhAccountsUseCase() {
    return new AddTransferJbhAccountsInputPort(
        productsService(), accountMovementServiceApplication());
  }

  @Produces
  @ApplicationScoped
  public LiquidateAccountUseCase liquidateAccountUseCase() {
    return new LiquidateAccountInputPort(productsService(), accountMovementServiceApplication());
  }

  @Produces
  @ApplicationScoped
  public RegisterMonthlyBalanceUseCase registerMonthlyBalanceUseCase() {
    return new RegisterMonthlyBalanceInputPort(
        monthlyBalanceService(), productsService(), accountMovementServiceApplication());
  }

  @Produces
  @ApplicationScoped
  public UpdateProductUseCase updateProductUseCase() {
    return new UpdateProductInputPort(productsService());
  }

  @Produces
  @ApplicationScoped
  public FindMovementsUseCase findMovementsByProductUseCase() {
    return new FindMovementsByProductInputPort(accountMovementQueryRepo, productsService());
  }
}
