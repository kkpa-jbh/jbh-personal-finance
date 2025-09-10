package com.jbh.account.application.accounts.usecases;

import com.jbh.account.application.accounts.vo.commands.CreateBasicAccountCommand;
import com.jbh.account.domain.vo.AccountDomainDTO;

public interface CreateAccountUseCase {

  AccountDomainDTO execute(CreateBasicAccountCommand command);
}
