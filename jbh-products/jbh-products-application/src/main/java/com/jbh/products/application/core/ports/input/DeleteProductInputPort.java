package com.jbh.products.application.core.ports.input;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.products.application.common.logging.LoggerFactory;
import com.jbh.products.application.core.dto.ProductDTO;
import com.jbh.products.application.core.exceptions.BusinessApplicationExceptionType;
import com.jbh.products.application.core.services.account.ProductsService;
import com.jbh.products.application.core.usecases.DeleteProductUseCase;
import com.jbh.products.application.core.vo.commands.DeleteProductCommand;
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
