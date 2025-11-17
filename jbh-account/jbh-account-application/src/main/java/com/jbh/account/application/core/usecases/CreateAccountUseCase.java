package com.jbh.account.application.core.usecases;

import com.jbh.account.application.core.dto.ProductDTO;
import com.jbh.account.application.core.vo.commands.CreateProductCommand;
import com.jbh.account.domain.exceptions.AccountBusinessException;

public interface CreateAccountUseCase {

  ProductDTO execute(CreateProductCommand command) throws AccountBusinessException;
}
