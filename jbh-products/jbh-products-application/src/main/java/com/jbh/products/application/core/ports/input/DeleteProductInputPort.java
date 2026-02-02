package com.jbh.products.application.core.ports.input;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.products.application.common.logging.LoggerFactory;
import com.jbh.products.application.core.dto.ProductDTO;
import com.jbh.products.application.core.exceptions.BusinessApplicationExceptionType;
import com.jbh.products.application.core.mappers.AccountMapper;
import com.jbh.products.application.core.services.account.ProductsService;
import com.jbh.products.application.core.usecases.DeleteProductUseCase;
import com.jbh.products.application.core.vo.commands.DeleteProductCommand;
import com.jbh.products.domain.entity.ProductDomain;
import org.slf4j.Logger;

public class DeleteProductInputPort implements DeleteProductUseCase {

  private static final Logger LOG = LoggerFactory.getLogger(DeleteProductInputPort.class);

  private final ProductsService accountService;

  public DeleteProductInputPort(final ProductsService accountService) {
    this.accountService = accountService;
  }

  @Override
  public void execute(final DeleteProductCommand command) throws BusinessException {
    if (command == null) {
      throw new GenericSpecificationException("Command cannot be null");
    }

    command.validate();

    final ProductDTO productDTO =
        accountService.findByUserAndProductId(command.userId(), command.productId());

    if (!productDTO.isActive()) {
      throw new BusinessException(BusinessApplicationExceptionType.PRODUCT_ALREADY_DELETED);
    }

    final ProductDomain productDomain = AccountMapper.toDomain(productDTO);
    productDomain.deactivate();

    accountService.save(productDomain);

    LOG.info(
        "Product '{}' (id: {}) soft deleted successfully for user {}",
        productDTO.name(),
        productDTO.id().value(),
        command.userId());
  }
}
