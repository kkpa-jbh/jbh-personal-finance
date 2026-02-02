package com.jbh.products.application.core.ports.input;

import com.jbh.commons.exception.BusinessException;
import com.jbh.products.application.core.dto.ProductDTO;
import com.jbh.products.application.core.mappers.AccountMapper;
import com.jbh.products.application.core.services.account.ProductsService;
import com.jbh.products.application.core.usecases.UpdateProductUseCase;
import com.jbh.products.application.core.vo.commands.UpdateMetadataProductCommand;
import com.jbh.products.domain.entity.ProductDomain;
import com.jbh.products.domain.vo.ProductMetadata;
import com.jbh.products.domain.vo.ProductPK;

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

    final ProductDomain loanProductDomain = AccountMapper.toDomain(loanProductDTO);
    loanProductDomain.replaceAllMetadata(productMetadata);

    accountService.save(loanProductDomain);
  }
}
