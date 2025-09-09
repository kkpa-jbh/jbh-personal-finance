package com.jbh.account.application.accounts.ports.input;

import com.jbh.account.application.accounts.ports.output.AccountRepository;
import com.jbh.account.application.accounts.usecases.NoOperationUseCase;
import com.jbh.account.domain.accounts.AccountId;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoOperationInputPort implements NoOperationUseCase {

  private static final Logger LOG = LoggerFactory.getLogger(NoOperationInputPort.class);

  public NoOperationInputPort(final AccountRepository accountRepository) {
    LOG.info("TestingInputPort created with AccountRepository" + accountRepository);
    accountRepository.findByAccountId(UUID.randomUUID(), AccountId.of(UUID.randomUUID()));
  }

  @Override
  public void healthCheck() {
    LOG.info("Health check from TestingInputPort");
  }

}
