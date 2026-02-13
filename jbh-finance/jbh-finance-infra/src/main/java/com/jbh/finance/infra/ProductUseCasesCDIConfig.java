package com.jbh.finance.infra;

import com.jbh.finance.application.acid.UnitOfWork;
import com.jbh.finance.application.async.AsyncTaskExecutorImpl;
import com.jbh.finance.application.feature.monthlybalance.ports.input.FindMonthlyBalanceInputPort;
import com.jbh.finance.application.feature.monthlybalance.ports.input.RegisterMonthlyBalanceInputPort;
import com.jbh.finance.application.feature.monthlybalance.ports.output.MonthlyBalanceQueryRepo;
import com.jbh.finance.application.feature.monthlybalance.ports.output.MonthlyBalanceWriterRepo;
import com.jbh.finance.application.feature.monthlybalance.services.MonthlyBalanceLifecycleService;
import com.jbh.finance.application.feature.monthlybalance.services.MonthlyBalanceLifecycleServiceImpl;
import com.jbh.finance.application.feature.monthlybalance.services.MonthlyBalanceSyncForUploadedMovements;
import com.jbh.finance.application.feature.monthlybalance.usecases.FindMonthlyBalanceUseCase;
import com.jbh.finance.application.feature.monthlybalance.usecases.RegisterMonthlyBalanceUseCase;
import com.jbh.finance.application.feature.movement.ports.input.AddMovementInputPort;
import com.jbh.finance.application.feature.movement.ports.input.AddMovementsUploadedFileInputPort;
import com.jbh.finance.application.feature.movement.ports.input.AddTransferJbhProductsInputPort;
import com.jbh.finance.application.feature.movement.ports.input.FindMovementsInputPort;
import com.jbh.finance.application.feature.movement.ports.input.LiquidateProductInputPort;
import com.jbh.finance.application.feature.movement.ports.output.MovementQueryRepository;
import com.jbh.finance.application.feature.movement.ports.output.MovementWriterRepository;
import com.jbh.finance.application.feature.movement.services.MovementLifecycleService;
import com.jbh.finance.application.feature.movement.services.MovementLifecycleServiceImpl;
import com.jbh.finance.application.feature.movement.services.ProcessMovementService;
import com.jbh.finance.application.feature.movement.services.ProcessMovementServiceImpl;
import com.jbh.finance.application.feature.movement.usecases.AddTransferJbhProductsUseCase;
import com.jbh.finance.application.feature.movement.usecases.FindMovementsUseCase;
import com.jbh.finance.application.feature.movement.usecases.LiquidateProductUseCase;
import com.jbh.finance.application.feature.product.ports.input.CreateProductInputPort;
import com.jbh.finance.application.feature.product.ports.input.DeleteProductInputPort;
import com.jbh.finance.application.feature.product.ports.input.EditProductInputPort;
import com.jbh.finance.application.feature.product.ports.input.FindActiveProductsInputPort;
import com.jbh.finance.application.feature.product.ports.input.GetProductMetadataConfigInputPort;
import com.jbh.finance.application.feature.product.ports.input.UpdateProductInputPort;
import com.jbh.finance.application.feature.product.ports.input.UpdateProductStatusInputPort;
import com.jbh.finance.application.feature.product.ports.output.ProductRepository;
import com.jbh.finance.application.feature.product.services.ProductLifecycleService;
import com.jbh.finance.application.feature.product.services.ProductLifecycleServiceImpl;
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

  @Inject MovementWriterRepository accountMovementWriterRepo;

  @Inject MovementQueryRepository accountMovementQueryRepo;

  @Inject UnitOfWork unitOfWork;

  @Inject
  @Named("monthlyBalanceWriterJPAAdapter")
  MonthlyBalanceWriterRepo monthlyBalanceWriterRepo;

  @Inject
  @Named("monthlyBalanceJPARepository")
  MonthlyBalanceQueryRepo monthlyBalanceQueryRepo;

  @Inject ProductLifecycleService productsService;

  @Produces
  @ApplicationScoped
  public CreateProductInputPort registeringCreateAccountUseCase() {
    return new CreateProductInputPort(productsService());
  }

  @Produces
  @ApplicationScoped
  public ProductLifecycleService productsService() {
    return new ProductLifecycleServiceImpl(accountRepository);
  }

  @Produces
  @ApplicationScoped
  public AddMovementInputPort registeringAddMovementUseCase() {
    return new AddMovementInputPort(accountMovementServiceApplication(), productsService());
  }

  @Produces
  @ApplicationScoped
  public ProcessMovementService accountMovementServiceApplication() {
    return new ProcessMovementServiceImpl(
        movementService(), productsService(), monthlyBalanceService(), unitOfWork);
  }

  @Produces
  public MovementLifecycleService movementService() {
    return new MovementLifecycleServiceImpl(accountMovementWriterRepo, accountMovementQueryRepo);
  }

  @Produces
  @ApplicationScoped
  public MonthlyBalanceLifecycleService monthlyBalanceService() {
    return new MonthlyBalanceLifecycleServiceImpl(
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
  public AddTransferJbhProductsUseCase addTransferJbhAccountsUseCase() {
    return new AddTransferJbhProductsInputPort(
        productsService(), accountMovementServiceApplication());
  }

  @Produces
  @ApplicationScoped
  public LiquidateProductUseCase liquidateAccountUseCase() {
    return new LiquidateProductInputPort(productsService(), accountMovementServiceApplication());
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
    return new FindMovementsInputPort(movementService(), productsService());
  }
}
