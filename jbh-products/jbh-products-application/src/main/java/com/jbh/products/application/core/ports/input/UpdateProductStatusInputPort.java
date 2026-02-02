package com.jbh.products.application.core.ports.input;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.products.application.common.logging.LoggerFactory;
import com.jbh.products.application.core.dto.ProductDTO;
import com.jbh.products.application.core.mappers.AccountMapper;
import com.jbh.products.application.core.services.account.ProductsService;
import com.jbh.products.application.core.usecases.UpdateProductStatusUseCase;
import com.jbh.products.application.core.vo.commands.UpdateProductStatusCommand;
import com.jbh.products.domain.entity.ProductDomain;
import org.slf4j.Logger;

public class UpdateProductStatusInputPort implements UpdateProductStatusUseCase {

  private static final Logger LOG = LoggerFactory.getLogger(UpdateProductStatusInputPort.class);

  private final ProductsService accountService;

  public UpdateProductStatusInputPort(final ProductsService accountService) {
    this.accountService = accountService;
  }

  @Override
  public ProductDTO execute(final UpdateProductStatusCommand command) throws BusinessException {
    if (command == null) {
      throw new GenericSpecificationException("Command cannot be null");
    }

    command.validate();

    final ProductDTO productDTO =
        accountService.findByUserAndProductId(command.userId(), command.productId());

    if (productDTO.isActive() == command.active()) {
      LOG.info(
          "Product '{}' (id: {}) is already {} for user {}",
          productDTO.name(),
          productDTO.id().value(),
          command.active() ? "active" : "inactive",
          command.userId());
      return productDTO;
    }

    final ProductDomain productDomain = AccountMapper.toDomain(productDTO);
    productDomain.setActive(command.active());

    final ProductDTO savedProduct = accountService.save(productDomain);

    LOG.info(
        "Product '{}' (id: {}) status updated to {} for user {}",
        savedProduct.name(),
        savedProduct.id().value(),
        command.active() ? "active" : "inactive",
        command.userId());

    return savedProduct;
  }
}
