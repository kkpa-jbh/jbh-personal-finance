package com.jbh.account.application.core.ports.input;

import com.jbh.account.application.common.logging.LoggerFactory;
import com.jbh.account.application.core.dto.AccountDTO;
import com.jbh.account.application.core.services.account.AccountService;
import com.jbh.account.application.core.usecases.CreateAccountUseCase;
import com.jbh.account.application.core.vo.commands.CreateAccountCommand;
import com.jbh.account.domain.entity.ProductDomain;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.exceptions.GenericSpecificationException;
import com.jbh.account.domain.vo.ProductMetadata;
import org.slf4j.Logger;

public class CreateAccountInputPort implements CreateAccountUseCase {

  private static final Logger LOG = LoggerFactory.getLogger(CreateAccountInputPort.class);

  private final AccountService accountService;

  public CreateAccountInputPort(final AccountService accountService) {
    this.accountService = accountService;
  }

  @Override
  public AccountDTO execute(final CreateAccountCommand command) throws AccountBusinessException {

    if (command == null) {
      throw new GenericSpecificationException("Command cannot be null");
    }

    command.validate();
    // Note: No need to call command.validate() - already validated in record constructor

    // Get metadata from command
    final ProductMetadata domainMetadata = command.productMetadata();

    // The application layer (Input Port) is responsible for orchestrating the use case. Creating
    // domain objects is part of that orchestration.

    // Domain validates itself during construction using Strategy Pattern
    // This ensures type-specific metadata requirements are enforced
    final ProductDomain accountDomain =
        ProductDomain.withMinimumDataForCreation(
            command.name(), command.type(), command.userId(), domainMetadata);

    // Persist the validated domain entity
    final AccountDTO accountDTO = accountService.save(accountDomain);

    LOG.info(
        "Account '{}' (type: {}) created successfully for user {}",
        accountDTO.name(),
        accountDTO.type(),
        command.userId());

    return accountDTO;
  }
}
