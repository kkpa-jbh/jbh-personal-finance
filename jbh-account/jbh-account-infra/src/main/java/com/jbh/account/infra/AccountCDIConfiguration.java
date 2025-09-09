package com.jbh.account.infra;

import com.jbh.account.application.accounts.ports.input.NoOperationInputPort;
import com.jbh.account.application.accounts.ports.output.AccountRepository;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

@ApplicationScoped
@RegisterForReflection(targets = {
    NoOperationInputPort.class
})
public class AccountCDIConfiguration {

  @Inject
  AccountRepository accountRepository;


  @Produces
  @ApplicationScoped
  public NoOperationInputPort registeringTestingUseCase() {
    return new NoOperationInputPort(accountRepository);
  }
}
