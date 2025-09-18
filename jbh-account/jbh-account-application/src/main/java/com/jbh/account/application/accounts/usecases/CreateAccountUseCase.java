package com.jbh.account.application.accounts.usecases;

import com.jbh.account.application.accounts.dto.AccountDTO;
import com.jbh.account.application.accounts.vo.commands.CreateBasicAccountCommand;

public interface CreateAccountUseCase {

  AccountDTO execute(CreateBasicAccountCommand command);
}
