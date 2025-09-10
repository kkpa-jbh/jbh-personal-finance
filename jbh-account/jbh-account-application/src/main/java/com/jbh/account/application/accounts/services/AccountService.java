package com.jbh.account.application.accounts.services;

import com.jbh.account.domain.entity.AccountDomain;
import com.jbh.account.domain.vo.AccountDomainDTO;

/**
 * Account Service Interface for CRUD operations
 */
public interface AccountService {

  AccountDomainDTO save(AccountDomain account);

  AccountDomainDTO save(AccountDomainDTO account);

}
