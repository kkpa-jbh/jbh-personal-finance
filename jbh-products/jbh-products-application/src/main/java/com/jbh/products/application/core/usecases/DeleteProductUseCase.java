package com.jbh.products.application.core.usecases;

import com.jbh.commons.exception.BusinessException;
import com.jbh.products.application.core.vo.commands.DeleteProductCommand;

public interface DeleteProductUseCase {

  void execute(DeleteProductCommand command) throws BusinessException;
}
