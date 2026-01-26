package com.jbh.account.application.core.ports.input;

import com.jbh.account.application.core.dto.ProductDTO;
import com.jbh.account.application.core.mappers.AccountMapper;
import com.jbh.account.application.core.services.account.AccountService;
import com.jbh.account.application.core.usecases.UpdateProductUseCase;
import com.jbh.account.application.core.vo.commands.UpdateMetadataProductCommand;
import com.jbh.account.domain.entity.ProductDomain;
import com.jbh.account.domain.vo.ProductMetadata;
import com.jbh.account.domain.vo.ProductPK;
import com.jbh.commons.exception.BusinessException;

public class UpdateProductInputPort implements UpdateProductUseCase {

  private final AccountService accountService;

  public UpdateProductInputPort(final AccountService accountService) {
    this.accountService = accountService;
  }

  @Override
  public void replaceMetadata(final ProductPK accountPK, final UpdateMetadataProductCommand command)
      throws BusinessException {

    final ProductDTO loanProductDTO = accountService.findAccountOrThrow(accountPK.accountId());

    final ProductMetadata productMetadata = command.metadata();

    final ProductDomain loanProductDomain = AccountMapper.toDomain(loanProductDTO);
    loanProductDomain.replaceAllMetadata(productMetadata);

    accountService.save(loanProductDomain);
  }
}
