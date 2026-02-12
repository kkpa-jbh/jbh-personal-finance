package com.jbh.finance.application.feature.product.ports.input;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.finance.application.common.logging.LoggerFactory;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.application.shared.exceptions.BusinessApplicationExceptionType;
import com.jbh.finance.application.feature.product.services.ProductsService;
import com.jbh.finance.application.feature.product.usecases.DeleteProductUseCase;
import com.jbh.finance.application.feature.product.commands.DeleteProductCommand;
import org.slf4j.Logger;

public class DeleteProductInputPort implements DeleteProductUseCase {

  private static final Logger LOG = LoggerFactory.getLogger(DeleteProductInputPort.class);

  private final ProductsService productsService;

  public DeleteProductInputPort(final ProductsService productsService) {
    this.productsService = productsService;
  }

  @Override
  public void execute(final DeleteProductCommand command) throws BusinessException {
    if (command == null) {
      throw new GenericSpecificationException("Command cannot be null");
    }

    command.validate();

    final ProductDTO productDTO =
        productsService.findByUserAndProductId(command.userId(), command.productId());

    if (productDTO == null) {
      throw new BusinessException(BusinessApplicationExceptionType.PRODUCT_NOT_FOUND);
    }

    productsService.deleteProduct(productDTO.id());

    LOG.info(
        "Product '{}' (id: {}) soft deleted successfully for user {}",
        productDTO.name(),
        productDTO.id().value(),
        command.userId());
  }
}
