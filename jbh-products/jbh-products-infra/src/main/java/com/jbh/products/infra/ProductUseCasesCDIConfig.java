package com.jbh.products.infra;

import com.jbh.products.application.acid.UnitOfWork;
import com.jbh.products.application.async.AsyncTaskExecutorImpl;
import com.jbh.products.application.core.ports.input.AddMovementInputPort;
import com.jbh.products.application.core.ports.input.AddMovementsUploadedFileInputPort;
import com.jbh.products.application.core.ports.input.CreateProductInputPort;
import com.jbh.products.application.core.ports.input.DeleteProductInputPort;
import com.jbh.products.application.core.ports.input.EditProductInputPort;
import com.jbh.products.application.core.ports.input.FindActiveProductsInputPort;
import com.jbh.products.application.core.ports.input.FindMonthlyBalanceInputPort;
import com.jbh.products.application.core.ports.input.GetProductMetadataConfigInputPort;
import com.jbh.products.application.core.ports.input.UpdateProductStatusInputPort;
import com.jbh.products.application.core.ports.output.ProductRepository;
import com.jbh.products.application.core.ports.output.monthlybalance.AccountMonthlyBalanceQueryRepo;
import com.jbh.products.application.core.ports.output.monthlybalance.AccountMonthlyBalanceWriterRepository;
import com.jbh.products.application.core.services.MonthlyBalanceSyncForUploadedMovements;
import com.jbh.products.application.core.services.account.ProductServiceImpl;
import com.jbh.products.application.core.services.account.ProductsService;
import com.jbh.products.application.core.services.metadata.ProductMetadataConfigRegistry;
import com.jbh.products.application.core.services.monthlybalance.MonthlyBalanceService;
import com.jbh.products.application.core.services.monthlybalance.MonthlyBalanceServiceImpl;
import com.jbh.products.application.core.services.movements.AccountMovementApplicationService;
import com.jbh.products.application.core.services.movements.AccountMovementApplicationServiceImpl;
import com.jbh.products.application.core.services.movements.AccountMovementService;
import com.jbh.products.application.core.services.movements.AccountMovementServiceImpl;
import com.jbh.products.application.core.usecases.DeleteProductUseCase;
import com.jbh.products.application.core.usecases.EditProductUseCase;
import com.jbh.products.application.core.usecases.FindMonthlyBalanceUseCase;
import com.jbh.products.application.core.usecases.FindProductsUseCase;
import com.jbh.products.application.core.usecases.GetProductMetadataConfigUseCase;
import com.jbh.products.application.core.usecases.UpdateProductStatusUseCase;
import com.jbh.products.application.movements.ports.output.AccountMovementQueryRepository;
import com.jbh.products.application.movements.ports.output.AccountMovementWriterRepository;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;

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
}
