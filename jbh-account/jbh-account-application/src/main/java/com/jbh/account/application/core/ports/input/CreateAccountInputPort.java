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
import com.jbh.account.domain.vo.ProductMetadataKey;
import java.util.Map;
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

    // Convert metadata from command format (AccountMetadataKey -> Object)
    // to domain format (String -> Object)
    final ProductMetadata domainMetadata = convertMetadata(command.metadata());

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

  /**
   * Converts metadata from command format (AccountMetadataKey -> Object) to domain format (String
   * -> Object).
   *
   * @param commandMetadata The metadata from the command
   * @return A map with string keys (AccountMetadataKey.name()) and original values
   */
  private ProductMetadata convertMetadata(final Map<ProductMetadataKey, Object> commandMetadata) {
    if (commandMetadata == null || commandMetadata.isEmpty()) {
      return ProductMetadata.empty();
    }

    return ProductMetadata.of(commandMetadata);
  }
}
