package com.jbh.account.application.core.ports.input;

import com.jbh.account.application.common.logging.LoggerFactory;
import com.jbh.account.application.core.dto.AccountDTO;
import com.jbh.account.application.core.services.account.AccountService;
import com.jbh.account.application.core.usecases.CreateAccountUseCase;
import com.jbh.account.application.core.vo.commands.CreateAccountCommand;
import com.jbh.account.domain.entity.AccountDomain;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.exceptions.GenericSpecificationException;
import com.jbh.account.domain.vo.AccountMetadataKey;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;

public class CreateAccountInputPort implements CreateAccountUseCase {

  private static final Logger LOG = LoggerFactory.getLogger(CreateAccountInputPort.class);

  private final AccountService accountService;

  public CreateAccountInputPort(final AccountService accountService) {
    this.accountService = accountService;
  }

  @Override
  public AccountDTO execute(final CreateAccountCommand command) {

    if (command == null) {
      throw new GenericSpecificationException("Command cannot be null");
    }

    // Note: No need to call command.validate() - already validated in record constructor

    try {
      // Convert metadata from command format (AccountMetadataKey -> Object)
      // to domain format (String -> Object)
      final Map<String, Object> domainMetadata = convertMetadata(command.metadata());

      // The application layer (Input Port) is responsible for orchestrating the use case. Creating
      // domain objects is part of that orchestration.

      // Domain validates itself during construction using Strategy Pattern
      // This ensures type-specific metadata requirements are enforced
      final AccountDomain accountDomain =
          AccountDomain.withMinimumDataForCreation(
              command.name(), command.type(), command.userId(), domainMetadata);

      // Persist the validated domain entity
      final AccountDTO accountDTO = accountService.save(accountDomain);

      LOG.info(
          "Account '{}' (type: {}) created successfully for user {}",
          accountDTO.name(),
          accountDTO.type(),
          command.userId());

      return accountDTO;

    } catch (final AccountBusinessException e) {
      LOG.error(
          "Business validation failed while creating account '{}' for user {}: {}",
          command.name(),
          command.userId(),
          e.getMessage());
      // Wrap domain exception in application exception
      throw new GenericSpecificationException("Failed to create account: " + e.getMessage());
    }
  }

  /**
   * Converts metadata from command format (AccountMetadataKey -> Object) to domain format (String
   * -> Object).
   *
   * @param commandMetadata The metadata from the command
   * @return A map with string keys (AccountMetadataKey.name()) and original values
   */
  private Map<String, Object> convertMetadata(
      final Map<AccountMetadataKey, Object> commandMetadata) {
    if (commandMetadata == null || commandMetadata.isEmpty()) {
      return new HashMap<>();
    }

    final Map<String, Object> domainMetadata = new HashMap<>();
    commandMetadata.forEach((key, value) -> domainMetadata.put(key.name(), value));
    return domainMetadata;
  }
}
