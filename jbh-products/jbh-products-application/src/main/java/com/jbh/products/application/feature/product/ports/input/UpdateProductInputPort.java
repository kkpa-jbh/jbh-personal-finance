package com.jbh.products.application.feature.product.ports.input;

import com.jbh.commons.exception.BusinessException;
import com.jbh.products.application.feature.product.dto.ProductDTO;
import com.jbh.products.application.feature.product.mappers.ProductMapper;
import com.jbh.products.application.feature.product.services.ProductsService;
import com.jbh.products.application.feature.product.usecases.UpdateProductUseCase;
import com.jbh.products.application.feature.product.commands.UpdateMetadataProductCommand;
import com.jbh.products.domain.product.ProductDomain;
import com.jbh.products.domain.product.vo.ProductMetadata;
import com.jbh.products.domain.product.vo.ProductPK;

public class UpdateProductInputPort implements UpdateProductUseCase {

  private final ProductsService accountService;

  public UpdateProductInputPort(final ProductsService accountService) {
    this.accountService = accountService;
  }

  @Override
  public void replaceMetadata(final ProductPK accountPK, final UpdateMetadataProductCommand command)
      throws BusinessException {

    final ProductDTO loanProductDTO = accountService.findProductOrThrow(accountPK.accountId());

    final ProductMetadata productMetadata = command.metadata();

    final ProductDomain loanProductDomain = ProductMapper.toDomain(loanProductDTO);
    loanProductDomain.replaceAllMetadata(productMetadata);

    accountService.save(loanProductDomain);
  }
}
