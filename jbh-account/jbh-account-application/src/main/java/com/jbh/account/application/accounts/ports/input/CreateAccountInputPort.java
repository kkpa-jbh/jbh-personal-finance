package com.jbh.account.application.accounts.ports.input;

import com.jbh.account.application.accounts.dto.AccountDTO;
import com.jbh.account.application.accounts.services.AccountService;
import com.jbh.account.application.accounts.usecases.CreateAccountUseCase;
import com.jbh.account.application.accounts.vo.commands.CreateBasicAccountCommand;
import com.jbh.account.application.common.logging.LoggerFactory;
import com.jbh.account.domain.entity.AccountDomain;
import java.util.UUID;
import org.slf4j.Logger;

public class CreateAccountInputPort implements CreateAccountUseCase {

  private static final Logger LOG = LoggerFactory.getLogger(CreateAccountInputPort.class);

  private final AccountService accountService;

  public CreateAccountInputPort(final AccountService accountService) {
    this.accountService = accountService;
  }

  @Override
  public AccountDTO execute(final CreateBasicAccountCommand command) {

    if (command == null) {
      throw new IllegalArgumentException("Command cannot be null");
    }

    command.validate();

    final UUID userId = command.userId();
    final AccountDomain accountDomain =
        AccountDomain.withMinimumDataForCreation(command.name(), command.type(), userId);

    final AccountDTO accountDTO = accountService.save(accountDomain);
    LOG.info("Account for user {} created successfully ", userId);

    return accountDTO;
  }
}
