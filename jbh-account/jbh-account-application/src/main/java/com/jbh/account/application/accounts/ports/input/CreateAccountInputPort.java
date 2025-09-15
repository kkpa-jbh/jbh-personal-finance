package com.jbh.account.application.accounts.ports.input;

import com.jbh.account.application.accounts.services.AccountService;
import com.jbh.account.application.accounts.usecases.CreateAccountUseCase;
import com.jbh.account.application.accounts.vo.commands.CreateBasicAccountCommand;
import com.jbh.account.application.common.logging.LoggerFactory;
import com.jbh.account.domain.entity.AccountDomain;
import com.jbh.account.domain.vo.AccountDomainDTO;
import java.util.UUID;
import org.slf4j.Logger;

public class CreateAccountInputPort implements CreateAccountUseCase {

  private static final Logger LOG = LoggerFactory.getLogger(CreateAccountInputPort.class);

  private final AccountService accountService;

  public CreateAccountInputPort(final AccountService accountService) {
    this.accountService = accountService;
  }

  @Override
  public AccountDomainDTO execute(final CreateBasicAccountCommand command) {

    if (command == null) {
      throw new IllegalArgumentException("Command cannot be null");
    }

    command.validate();

    final UUID userId = command.userId();
    final AccountDomain accountDomain =
        AccountDomain.withCommand(command.name(), command.type(), userId);

    final AccountDomainDTO accountDTO = accountService.save(accountDomain.toDTO());
    LOG.info("Account for user {} created successfully ", userId);

    return accountDTO;
  }
}
