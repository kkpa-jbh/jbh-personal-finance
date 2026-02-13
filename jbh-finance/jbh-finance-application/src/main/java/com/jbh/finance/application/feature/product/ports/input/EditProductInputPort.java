package com.jbh.finance.application.feature.product.ports.input;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.finance.application.common.logging.LoggerFactory;
import com.jbh.finance.application.feature.product.commands.EditProductCommand;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.application.feature.product.mappers.ProductMapper;
import com.jbh.finance.application.feature.product.services.ProductLifecycleService;
import com.jbh.finance.application.feature.product.usecases.EditProductUseCase;
import com.jbh.finance.application.shared.exceptions.BusinessApplicationExceptionType;
import com.jbh.finance.domain.product.ProductDomain;
import org.slf4j.Logger;

public class EditProductInputPort implements EditProductUseCase {

  private static final Logger LOG = LoggerFactory.getLogger(EditProductInputPort.class);

  private final ProductLifecycleService accountService;

  public EditProductInputPort(final ProductLifecycleService accountService) {
    this.accountService = accountService;
  }

  @Override
  public ProductDTO execute(final EditProductCommand command) throws BusinessException {
    if (command == null) {
      throw new GenericSpecificationException("Command cannot be null");
    }

    command.validate();

    final ProductDTO productDTO =
        accountService.findOrThrowByUserAndProductId(command.userId(), command.productId());

    if (!productDTO.isActive()) {
      throw new BusinessException(BusinessApplicationExceptionType.PRODUCT_NOT_ACTIVE);
    }

    final ProductDomain productDomain = ProductMapper.toDomain(productDTO);

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
