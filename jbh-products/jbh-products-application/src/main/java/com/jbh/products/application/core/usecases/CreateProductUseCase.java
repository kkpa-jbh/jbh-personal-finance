package com.jbh.products.application.core.usecases;

import com.jbh.products.application.core.dto.ProductDTO;
import com.jbh.products.application.core.vo.commands.CreateProductCommand;
import com.jbh.commons.exception.BusinessException;

public interface CreateProductUseCase {

  ProductDTO execute(CreateProductCommand command) throws BusinessException;
}
