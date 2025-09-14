package com.jbh.account.infra;

import com.jbh.account.application.accounts.ports.input.AddBasicMovementInputPort;
import com.jbh.account.application.accounts.ports.input.CreateAccountInputPort;
import com.jbh.account.application.accounts.ports.input.NoOperationInputPort;
import com.jbh.account.application.accounts.ports.output.AccountMonthlyBalanceRepository;
import com.jbh.account.application.accounts.ports.output.AccountRepository;
import com.jbh.account.application.accounts.services.AccountService;
import com.jbh.account.application.accounts.services.AccountServiceImpl;
import com.jbh.account.application.accounts.services.MonthlyBalanceSyncerAppService;
import com.jbh.account.application.acid.UnitOfWork;
import com.jbh.account.application.async.AsyncTaskExecutorImpl;
import com.jbh.account.application.movements.ports.output.AccountMovementRepository;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

@ApplicationScoped
@RegisterForReflection(
    targets = {
      NoOperationInputPort.class,
      CreateAccountInputPort.class,
      AddBasicMovementInputPort.class
    })
public class AccountUseCasesCDIConfig {

  @Inject AccountRepository accountRepository;

  @Inject AccountMovementRepository accountMovementRepo;

  @Inject AccountMonthlyBalanceRepository accountMonthlyBalanceRepo;

  @Inject UnitOfWork unitOfWork;

  @Produces
  @ApplicationScoped
  public CreateAccountInputPort registeringCreateAccountUseCase() {
    final AccountService accountService = new AccountServiceImpl(accountRepository);
    return new CreateAccountInputPort(accountService);
  }

  @Produces
  @ApplicationScoped
  public AddBasicMovementInputPort registeringAddMovementUseCase() {
    return new AddBasicMovementInputPort(
        accountRepository, accountMovementRepo, unitOfWork, monthlyBalanceSyncerAppService());
  }

  @Produces
  @ApplicationScoped
  public MonthlyBalanceSyncerAppService monthlyBalanceSyncerAppService() {
    return new MonthlyBalanceSyncerAppService(
        accountMonthlyBalanceRepo, new AsyncTaskExecutorImpl());
  }

  @Produces
  @ApplicationScoped
  public NoOperationInputPort registeringTestingUseCase() {
    return new NoOperationInputPort(accountRepository);
  }
}
