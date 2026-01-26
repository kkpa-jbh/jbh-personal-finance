package com.jbh.account.infra;

import com.jbh.account.application.acid.UnitOfWork;
import com.jbh.account.application.async.AsyncTaskExecutorImpl;
import com.jbh.account.application.core.ports.input.AddMovementInputPort;
import com.jbh.account.application.core.ports.input.AddMovementsUploadedFileInputPort;
import com.jbh.account.application.core.ports.input.CreateProductInputPort;
import com.jbh.account.application.core.ports.input.FindMonthlyBalanceInputPort;
import com.jbh.account.application.core.ports.input.GetProductMetadataConfigInputPort;
import com.jbh.account.application.core.ports.output.AccountRepository;
import com.jbh.account.application.core.ports.output.monthlybalance.AccountMonthlyBalanceQueryRepo;
import com.jbh.account.application.core.ports.output.monthlybalance.AccountMonthlyBalanceWriterRepository;
import com.jbh.account.application.core.services.MonthlyBalanceSyncForUploadedMovements;
import com.jbh.account.application.core.services.account.AccountService;
import com.jbh.account.application.core.services.account.AccountServiceImpl;
import com.jbh.account.application.core.services.metadata.ProductMetadataConfigRegistry;
import com.jbh.account.application.core.services.monthlybalance.MonthlyBalanceService;
import com.jbh.account.application.core.services.monthlybalance.MonthlyBalanceServiceImpl;
import com.jbh.account.application.core.services.movements.AccountMovementApplicationService;
import com.jbh.account.application.core.services.movements.AccountMovementApplicationServiceImpl;
import com.jbh.account.application.core.services.movements.AccountMovementService;
import com.jbh.account.application.core.services.movements.AccountMovementServiceImpl;
import com.jbh.account.application.core.usecases.FindMonthlyBalanceUseCase;
import com.jbh.account.application.core.usecases.GetProductMetadataConfigUseCase;
import com.jbh.account.application.movements.ports.output.AccountMovementQueryRepository;
import com.jbh.account.application.movements.ports.output.AccountMovementWriterRepository;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@ApplicationScoped
@RegisterForReflection(targets = {CreateProductInputPort.class, AddMovementInputPort.class})
public class ProductUseCasesCDIConfig {

  @Inject AccountRepository accountRepository;

  @Inject AccountMovementWriterRepository accountMovementWriterRepo;

  @Inject AccountMovementQueryRepository accountMovementQueryRepo;

  @Inject UnitOfWork unitOfWork;

  @Inject
  @Named("monthlyBalanceWriterJPAAdapter")
  AccountMonthlyBalanceWriterRepository monthlyBalanceWriterRepo;

  @Inject
  @Named("monthlyBalanceJPARepository")
  AccountMonthlyBalanceQueryRepo monthlyBalanceQueryRepo;

  @Produces
  @ApplicationScoped
  public CreateProductInputPort registeringCreateAccountUseCase() {
    return new CreateProductInputPort(accountService());
  }

  @Produces
  @ApplicationScoped
  public AccountService accountService() {
    return new AccountServiceImpl(accountRepository);
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
}
