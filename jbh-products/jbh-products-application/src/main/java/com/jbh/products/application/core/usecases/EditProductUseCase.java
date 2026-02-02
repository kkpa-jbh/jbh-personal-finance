package com.jbh.products.application.core.usecases;

import com.jbh.commons.exception.BusinessException;
import com.jbh.products.application.core.dto.ProductDTO;
import com.jbh.products.application.core.vo.commands.EditProductCommand;

public interface EditProductUseCase {

  ProductDTO execute(EditProductCommand command) throws BusinessException;
}
