package com.jbh.finance.application.feature.product.ports.input;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.product.commands.UpdateMetadataProductCommand;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.application.feature.product.mappers.ProductMapper;
import com.jbh.finance.application.feature.product.services.ProductLifecycleService;
import com.jbh.finance.application.feature.product.usecases.UpdateProductUseCase;
import com.jbh.finance.domain.product.ProductDomain;
import com.jbh.finance.domain.product.vo.ProductMetadata;
import com.jbh.finance.domain.product.vo.ProductPK;

public class UpdateProductInputPort implements UpdateProductUseCase {

  private final ProductLifecycleService accountService;

  public UpdateProductInputPort(final ProductLifecycleService accountService) {
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
