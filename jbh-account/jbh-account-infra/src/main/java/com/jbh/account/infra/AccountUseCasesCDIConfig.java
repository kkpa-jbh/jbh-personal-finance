package com.jbh.account.infra;

import com.jbh.account.application.acid.UnitOfWork;
import com.jbh.account.application.async.AsyncTaskExecutorImpl;
import com.jbh.account.application.core.ports.input.AddMovementInputPort;
import com.jbh.account.application.core.ports.input.AddMovementsUploadedFileInputPort;
import com.jbh.account.application.core.ports.input.CreateAccountInputPort;
import com.jbh.account.application.core.ports.output.AccountRepository;
import com.jbh.account.application.core.ports.output.monthlybalance.AccountMonthlyBalanceQueryRepo;
import com.jbh.account.application.core.ports.output.monthlybalance.AccountMonthlyBalanceWriterRepository;
import com.jbh.account.application.core.services.MonthlyBalanceSyncForUploadedMovements;
import com.jbh.account.application.core.services.account.AccountService;
import com.jbh.account.application.core.services.account.AccountServiceImpl;
import com.jbh.account.application.core.services.monthlybalance.MonthlyBalanceService;
import com.jbh.account.application.core.services.monthlybalance.MonthlyBalanceServiceImpl;
import com.jbh.account.application.core.services.movements.AccountMovementService;
import com.jbh.account.application.core.services.movements.AccountMovementServiceImpl;
import com.jbh.account.application.movements.ports.output.AccountMovementRepository;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@ApplicationScoped
@RegisterForReflection(targets = {CreateAccountInputPort.class, AddMovementInputPort.class})
public class AccountUseCasesCDIConfig {

  @Inject AccountRepository accountRepository;

  @Inject AccountMovementRepository accountMovementRepo;

  @Inject UnitOfWork unitOfWork;

  @Inject
  @Named("monthlyBalanceWriterJPAAdapter")
  AccountMonthlyBalanceWriterRepository monthlyBalanceWriterRepo;

  @Inject
  @Named("monthlyBalanceJPARepository")
  AccountMonthlyBalanceQueryRepo monthlyBalanceQueryRepo;

  @Produces
  @ApplicationScoped
  public CreateAccountInputPort registeringCreateAccountUseCase() {
    return new CreateAccountInputPort(accountService());
  }

  @Produces
  @ApplicationScoped
  public AccountService accountService() {
    return new AccountServiceImpl(accountRepository);
  }

  @Produces
  @ApplicationScoped
  public AddMovementInputPort registeringAddMovementUseCase() {
    return new AddMovementInputPort(accountMovementService());
  }

  @Produces
  @ApplicationScoped
  public AccountMovementService accountMovementService() {
    return new AccountMovementServiceImpl(
        accountMovementRepo, accountService(), monthlyBalanceService(), unitOfWork);
  }

  @Produces
  @ApplicationScoped
  public MonthlyBalanceService monthlyBalanceService() {
    return new MonthlyBalanceServiceImpl(
        monthlyBalanceQueryRepo, monthlyBalanceWriterRepo, new AsyncTaskExecutorImpl());
  }

  @Produces
  @ApplicationScoped
  public AddMovementsUploadedFileInputPort registeringAddMovementsUploadedFileUseCase() {
    return new AddMovementsUploadedFileInputPort(
        accountRepository, accountMovementRepo, unitOfWork, uploadedMovementsBalanceSynchronizer());
  }

  @Produces
  @ApplicationScoped
  public MonthlyBalanceSyncForUploadedMovements uploadedMovementsBalanceSynchronizer() {
    return new MonthlyBalanceSyncForUploadedMovements(monthlyBalanceService());
  }
}
