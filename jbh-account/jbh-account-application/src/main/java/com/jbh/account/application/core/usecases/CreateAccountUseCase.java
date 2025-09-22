package com.jbh.account.application.core.usecases;

import com.jbh.account.application.core.dto.AccountDTO;
import com.jbh.account.application.core.vo.commands.CreateBasicAccountCommand;

public interface CreateAccountUseCase {

  AccountDTO execute(CreateBasicAccountCommand command);
}
