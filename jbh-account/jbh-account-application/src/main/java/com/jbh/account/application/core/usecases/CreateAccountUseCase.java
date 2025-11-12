package com.jbh.account.application.core.usecases;

import com.jbh.account.application.core.dto.AccountDTO;
import com.jbh.account.application.core.vo.commands.CreateProductCommand;
import com.jbh.account.domain.exceptions.AccountBusinessException;

public interface CreateAccountUseCase {

  AccountDTO execute(CreateProductCommand command) throws AccountBusinessException;
}
