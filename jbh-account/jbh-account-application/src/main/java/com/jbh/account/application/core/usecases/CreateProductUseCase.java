package com.jbh.account.application.core.usecases;

import com.jbh.account.application.core.dto.ProductDTO;
import com.jbh.account.application.core.vo.commands.CreateProductCommand;
import com.jbh.account.domain.exceptions.ProductBusinessException;

public interface CreateProductUseCase {

  ProductDTO execute(CreateProductCommand command) throws ProductBusinessException;
}
