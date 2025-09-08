package com.jbh.account.infra;

import com.jbh.account.application.accounts.ports.input.TestingInputPort;
import com.jbh.account.application.accounts.ports.output.AccountRepository;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

@ApplicationScoped
@RegisterForReflection(targets = {
    TestingInputPort.class
})
public class AccountCDIConfiguration {

  @Inject
  AccountRepository accountRepository;


  @Produces
  @ApplicationScoped
  public TestingInputPort registeringTestingUseCase() {
    return new TestingInputPort(accountRepository);
  }
}
