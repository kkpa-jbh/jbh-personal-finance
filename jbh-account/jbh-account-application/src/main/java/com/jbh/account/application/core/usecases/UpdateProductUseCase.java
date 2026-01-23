package com.jbh.account.application.core.usecases;

import com.jbh.account.application.core.vo.commands.UpdateMetadataProductCommand;
import com.jbh.account.domain.exceptions.ProductBusinessException;
import com.jbh.account.domain.vo.ProductPK;

public interface UpdateProductUseCase {

  void replaceMetadata(ProductPK accountPK, UpdateMetadataProductCommand command)
      throws ProductBusinessException;
}
