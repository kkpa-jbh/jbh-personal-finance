package com.jbh.products.application.core.usecases;

import com.jbh.products.application.core.vo.commands.UpdateMetadataProductCommand;
import com.jbh.products.domain.vo.ProductPK;
import com.jbh.commons.exception.BusinessException;

public interface UpdateProductUseCase {

  void replaceMetadata(ProductPK accountPK, UpdateMetadataProductCommand command)
      throws BusinessException;
}
