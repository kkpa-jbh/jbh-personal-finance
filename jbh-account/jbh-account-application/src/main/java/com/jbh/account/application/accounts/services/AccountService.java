package com.jbh.account.application.accounts.services;

import com.jbh.account.application.accounts.dto.AccountDTO;
import com.jbh.account.domain.entity.AccountDomain;

/** Account Service Interface for CRUD operations */
public interface AccountService {

  AccountDTO save(AccountDomain account);

  AccountDTO save(AccountDTO account);
}
