package com.jbh.products.application.core.ports.input;

import com.jbh.products.application.common.logging.LoggerFactory;
import com.jbh.products.application.core.dto.ProductDTO;
import com.jbh.products.application.core.services.account.AccountService;
import com.jbh.products.application.core.usecases.CreateProductUseCase;
import com.jbh.products.application.core.vo.commands.CreateProductCommand;
import com.jbh.products.domain.entity.ProductDomain;
import com.jbh.products.domain.vo.ProductMetadata;
import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.GenericSpecificationException;
import org.slf4j.Logger;

public class CreateProductInputPort implements CreateProductUseCase {

  private static final Logger LOG = LoggerFactory.getLogger(CreateProductInputPort.class);

  private final AccountService accountService;

  public CreateProductInputPort(final AccountService accountService) {
    this.accountService = accountService;
  }

  @Override
  public ProductDTO execute(final CreateProductCommand command) throws BusinessException {

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
    final ProductDTO accountDTO = accountService.save(accountDomain);

    LOG.info(
        "Account '{}' (type: {}) created successfully for user {}",
        accountDTO.name(),
        accountDTO.type(),
        command.userId());

    return accountDTO;
  }
}
