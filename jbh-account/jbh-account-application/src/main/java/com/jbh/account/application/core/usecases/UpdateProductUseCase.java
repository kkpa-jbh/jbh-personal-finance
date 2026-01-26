package com.jbh.account.application.core.usecases;

import com.jbh.account.application.core.vo.commands.UpdateMetadataProductCommand;
import com.jbh.account.domain.vo.ProductPK;
import com.jbh.commons.exception.BusinessException;

public interface UpdateProductUseCase {

  void replaceMetadata(ProductPK accountPK, UpdateMetadataProductCommand command)
      throws BusinessException;
}
