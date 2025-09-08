package com.jbh.account.application.accounts.ports.input;

import com.jbh.account.application.accounts.ports.output.AccountRepository;
import com.jbh.account.application.accounts.usecases.TestingUseCase;
import com.jbh.account.domain.accounts.AccountId;
import java.util.UUID;

public class TestingInputPort implements TestingUseCase {

  public TestingInputPort(AccountRepository accountRepository) {
    System.out.println("TestingInputPort created with AccountRepository" + accountRepository);
    accountRepository.findByAccountId(UUID.randomUUID(), AccountId.of(UUID.randomUUID()));
  }

  @Override
  public void healthCheck() {
    System.out.println("Health check from TestingInputPort");
  }

}
