package com.jbh.account.infra;

import com.jbh.account.application.acid.UnitOfWork;
import com.jbh.account.application.async.AsyncTaskExecutorImpl;
import com.jbh.account.application.core.ports.input.AddMovementInputPort;
import com.jbh.account.application.core.ports.input.AddMovementsUploadedFileInputPort;
import com.jbh.account.application.core.ports.input.CreateAccountInputPort;
import com.jbh.account.application.core.ports.output.AccountRepository;
import com.jbh.account.application.core.ports.output.monthlybalance.AccountMonthlyBalanceQueryRepo;
import com.jbh.account.application.core.ports.output.monthlybalance.AccountMonthlyBalanceWriterRepository;
import com.jbh.account.application.core.services.MonthlyBalanceSyncerAppService;
import com.jbh.account.application.core.services.account.AccountService;
import com.jbh.account.application.core.services.account.AccountServiceImpl;
import com.jbh.account.application.core.services.monthlybalance.MonthlyBalanceServiceImpl;
import com.jbh.account.application.movements.ports.output.AccountMovementRepository;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

@ApplicationScoped
@RegisterForReflection(targets = {CreateAccountInputPort.class, AddMovementInputPort.class})
public class AccountUseCasesCDIConfig {

  @Inject AccountRepository accountRepository;

  @Inject AccountMovementRepository accountMovementRepo;

  @Inject AccountMonthlyBalanceQueryRepo monthlyBalanceQueryRepo;
  @Inject AccountMonthlyBalanceWriterRepository monthlyBalanceWriterRepo;

  @Inject UnitOfWork unitOfWork;

  @Produces
  @ApplicationScoped
  public CreateAccountInputPort registeringCreateAccountUseCase() {
    final AccountService accountService = new AccountServiceImpl(accountRepository);
    return new CreateAccountInputPort(accountService);
  }

  @Produces
  @ApplicationScoped
  public AddMovementInputPort registeringAddMovementUseCase() {
    return new AddMovementInputPort(
        accountService(), accountMovementRepo, unitOfWork, monthlyBalanceSyncerAppService());
  }

  @Produces
  @ApplicationScoped
  public AccountService accountService() {
    return new AccountServiceImpl(accountRepository);
  }

  @Produces
  @ApplicationScoped
  public MonthlyBalanceSyncerAppService monthlyBalanceSyncerAppService() {
    return new MonthlyBalanceSyncerAppService(
        new MonthlyBalanceServiceImpl(monthlyBalanceQueryRepo, monthlyBalanceWriterRepo),
        new AsyncTaskExecutorImpl());
  }

  @Produces
  @ApplicationScoped
  public AddMovementsUploadedFileInputPort registeringAddMovementsUploadedFileUseCase() {
    return new AddMovementsUploadedFileInputPort(
        accountRepository, accountMovementRepo, unitOfWork, monthlyBalanceSyncerAppService());
  }
}
