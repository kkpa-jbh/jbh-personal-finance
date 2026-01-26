package com.jbh.account.application.core.usecases;

import com.jbh.account.application.core.dto.ProductDTO;
import com.jbh.account.application.core.vo.commands.CreateProductCommand;
import com.jbh.commons.exception.BusinessException;

public interface CreateProductUseCase {

  ProductDTO execute(CreateProductCommand command) throws BusinessException;
}
