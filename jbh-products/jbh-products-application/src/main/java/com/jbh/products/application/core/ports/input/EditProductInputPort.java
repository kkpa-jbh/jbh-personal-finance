package com.jbh.products.application.core.ports.input;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.products.application.common.logging.LoggerFactory;
import com.jbh.products.application.core.dto.ProductDTO;
import com.jbh.products.application.core.exceptions.BusinessApplicationExceptionType;
import com.jbh.products.application.core.mappers.AccountMapper;
import com.jbh.products.application.core.services.account.ProductsService;
import com.jbh.products.application.core.usecases.EditProductUseCase;
import com.jbh.products.application.core.vo.commands.EditProductCommand;
import com.jbh.products.domain.entity.ProductDomain;
import org.slf4j.Logger;

public class EditProductInputPort implements EditProductUseCase {

  private static final Logger LOG = LoggerFactory.getLogger(EditProductInputPort.class);

  private final ProductsService accountService;

  public EditProductInputPort(final ProductsService accountService) {
    this.accountService = accountService;
  }

  @Override
  public ProductDTO execute(final EditProductCommand command) throws BusinessException {
    if (command == null) {
      throw new GenericSpecificationException("Command cannot be null");
    }

    command.validate();

    final ProductDTO productDTO =
        accountService.findByUserAndProductId(command.userId(), command.productId());

    if (!productDTO.isActive()) {
      throw new BusinessException(BusinessApplicationExceptionType.PRODUCT_NOT_ACTIVE);
    }

    final ProductDomain productDomain = AccountMapper.toDomain(productDTO);

    if (command.name() != null && !command.name().isBlank()) {
      productDomain.setName(command.name());
    }

    if (command.metadata() != null && !command.metadata().isEmpty()) {
      productDomain.replaceAllMetadata(command.metadata());
    }

    final ProductDTO savedProduct = accountService.save(productDomain);

    LOG.info(
        "Product '{}' (id: {}) edited successfully for user {}",
        savedProduct.name(),
        savedProduct.id().value(),
        command.userId());

    return savedProduct;
  }
}
