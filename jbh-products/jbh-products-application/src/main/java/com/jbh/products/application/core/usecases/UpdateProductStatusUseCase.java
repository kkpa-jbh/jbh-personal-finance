package com.jbh.products.application.core.usecases;

import com.jbh.commons.exception.BusinessException;
import com.jbh.products.application.core.dto.ProductDTO;
import com.jbh.products.application.core.vo.commands.UpdateProductStatusCommand;

public interface UpdateProductStatusUseCase {

  ProductDTO execute(UpdateProductStatusCommand command) throws BusinessException;
}
