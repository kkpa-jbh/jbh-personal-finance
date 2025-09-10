package com.jbh.account.infra;

import com.jbh.account.application.accounts.ports.input.CreateAccountInputPort;
import com.jbh.account.application.accounts.ports.input.NoOperationInputPort;
import com.jbh.account.application.accounts.ports.output.AccountRepository;
import com.jbh.account.application.accounts.services.AccountService;
import com.jbh.account.application.accounts.services.AccountServiceImpl;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

@ApplicationScoped
@RegisterForReflection(targets = {
    NoOperationInputPort.class,
    CreateAccountInputPort.class,
})
public class AccountCDIConfiguration {

  @Inject
  AccountRepository accountRepository;

  private AccountService accountService;

  @Produces
  @ApplicationScoped
  public CreateAccountInputPort registeringCreateAccountUseCase() {
    final AccountService accountService = new AccountServiceImpl(accountRepository);
    return new CreateAccountInputPort(accountService);
  }

  @Produces
  @ApplicationScoped
  public NoOperationInputPort registeringTestingUseCase() {
    return new NoOperationInputPort(accountRepository);
  }
}
