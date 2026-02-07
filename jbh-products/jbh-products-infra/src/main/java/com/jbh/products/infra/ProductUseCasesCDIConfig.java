package com.jbh.products.infra;

import com.jbh.products.application.acid.UnitOfWork;
import com.jbh.products.application.async.AsyncTaskExecutorImpl;
import com.jbh.products.application.feature.movement.ports.input.AddMovementInputPort;
import com.jbh.products.application.feature.movement.ports.input.AddMovementsUploadedFileInputPort;
import com.jbh.products.application.feature.movement.ports.input.AddTransferJbhAccountsInputPort;
import com.jbh.products.application.feature.product.ports.input.CreateProductInputPort;
import com.jbh.products.application.feature.product.ports.input.DeleteProductInputPort;
import com.jbh.products.application.feature.product.ports.input.EditProductInputPort;
import com.jbh.products.application.feature.product.ports.input.FindActiveProductsInputPort;
import com.jbh.products.application.feature.monthlybalance.ports.input.FindMonthlyBalanceInputPort;
import com.jbh.products.application.feature.product.ports.input.GetProductMetadataConfigInputPort;
import com.jbh.products.application.feature.movement.ports.input.LiquidateAccountInputPort;
import com.jbh.products.application.feature.monthlybalance.ports.input.RegisterMonthlyBalanceInputPort;
import com.jbh.products.application.feature.product.ports.input.UpdateProductInputPort;
import com.jbh.products.application.feature.product.ports.input.UpdateProductStatusInputPort;
import com.jbh.products.application.feature.product.ports.output.ProductRepository;
import com.jbh.products.application.feature.monthlybalance.ports.output.AccountMonthlyBalanceQueryRepo;
import com.jbh.products.application.feature.monthlybalance.ports.output.AccountMonthlyBalanceWriterRepository;
import com.jbh.products.application.feature.monthlybalance.services.MonthlyBalanceSyncForUploadedMovements;
import com.jbh.products.application.feature.product.services.ProductServiceImpl;
import com.jbh.products.application.feature.product.services.ProductsService;
import com.jbh.products.application.feature.product.services.metadata.ProductMetadataConfigRegistry;
import com.jbh.products.application.feature.monthlybalance.services.MonthlyBalanceService;
import com.jbh.products.application.feature.monthlybalance.services.MonthlyBalanceServiceImpl;
import com.jbh.products.application.feature.movement.services.AccountMovementApplicationService;
import com.jbh.products.application.feature.movement.services.AccountMovementApplicationServiceImpl;
import com.jbh.products.application.feature.movement.services.AccountMovementService;
import com.jbh.products.application.feature.movement.services.AccountMovementServiceImpl;
import com.jbh.products.application.feature.movement.usecases.AddTransferJbhAccountsUseCase;
import com.jbh.products.application.feature.product.usecases.DeleteProductUseCase;
import com.jbh.products.application.feature.product.usecases.EditProductUseCase;
import com.jbh.products.application.feature.monthlybalance.usecases.FindMonthlyBalanceUseCase;
import com.jbh.products.application.feature.product.usecases.FindProductsUseCase;
import com.jbh.products.application.feature.product.usecases.GetProductMetadataConfigUseCase;
import com.jbh.products.application.feature.movement.usecases.LiquidateAccountUseCase;
import com.jbh.products.application.feature.monthlybalance.usecases.RegisterMonthlyBalanceUseCase;
import com.jbh.products.application.feature.product.usecases.UpdateProductStatusUseCase;
import com.jbh.products.application.feature.product.usecases.UpdateProductUseCase;
import com.jbh.products.application.feature.movement.ports.output.AccountMovementQueryRepository;
import com.jbh.products.application.feature.movement.ports.output.AccountMovementWriterRepository;
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

  @Inject AccountMovementQueryRepository accountMovementQueryRepo;

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
    return new CreateProductInputPort(accountService());
  }

  @Produces
  @ApplicationScoped
  public ProductsService accountService() {
    return new ProductServiceImpl(accountRepository);
  }

  @Produces
  @ApplicationScoped
  public AddMovementInputPort registeringAddMovementUseCase() {
    return new AddMovementInputPort(accountMovementServiceApplication(), accountService());
  }

  @Produces
  @ApplicationScoped
  public AccountMovementApplicationService accountMovementServiceApplication() {
    return new AccountMovementApplicationServiceImpl(
        accountMovementService(), accountService(), monthlyBalanceService(), unitOfWork);
  }

  @Produces
  public AccountMovementService accountMovementService() {
    return new AccountMovementServiceImpl(accountMovementWriterRepo, accountMovementQueryRepo);
  }

  @Produces
  @ApplicationScoped
  public MonthlyBalanceService monthlyBalanceService() {
    return new MonthlyBalanceServiceImpl(
        monthlyBalanceQueryRepo,
        monthlyBalanceWriterRepo,
        new AsyncTaskExecutorImpl(),
        accountService());
  }

  @Produces
  @ApplicationScoped
  public AddMovementsUploadedFileInputPort registeringAddMovementsUploadedFileUseCase() {
    return new AddMovementsUploadedFileInputPort(
        accountService(),
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
    return new FindMonthlyBalanceInputPort(monthlyBalanceService(), accountService());
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
    return new FindActiveProductsInputPort(accountService());
  }

  @Produces
  @ApplicationScoped
  public EditProductUseCase editProductUseCase() {
    return new EditProductInputPort(accountService());
  }

  @Produces
  @ApplicationScoped
  public DeleteProductUseCase deleteProductUseCase() {
    return new DeleteProductInputPort(accountService());
  }

  @Produces
  @ApplicationScoped
  public UpdateProductStatusUseCase updateProductStatusUseCase() {
    return new UpdateProductStatusInputPort(accountService());
  }

  @Produces
  @ApplicationScoped
  public AddTransferJbhAccountsUseCase addTransferJbhAccountsUseCase() {
    return new AddTransferJbhAccountsInputPort(accountService(), accountMovementServiceApplication());
  }

  @Produces
  @ApplicationScoped
  public LiquidateAccountUseCase liquidateAccountUseCase() {
    return new LiquidateAccountInputPort(accountService(), accountMovementServiceApplication());
  }

  @Produces
  @ApplicationScoped
  public RegisterMonthlyBalanceUseCase registerMonthlyBalanceUseCase() {
    return new RegisterMonthlyBalanceInputPort(
        monthlyBalanceService(), accountService(), accountMovementServiceApplication());
  }

  @Produces
  @ApplicationScoped
  public UpdateProductUseCase updateProductUseCase() {
    return new UpdateProductInputPort(accountService());
  }
}
