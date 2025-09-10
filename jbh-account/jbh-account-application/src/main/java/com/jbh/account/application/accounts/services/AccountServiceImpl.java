package com.jbh.account.application.accounts.services;

import com.jbh.account.application.accounts.ports.output.AccountRepository;
import com.jbh.account.domain.entity.AccountDomain;
import com.jbh.account.domain.vo.AccountDomainDTO;

public class AccountServiceImpl implements AccountService {

  private final AccountRepository accountRepo;

  public AccountServiceImpl(final AccountRepository accountRepo) {
    this.accountRepo = accountRepo;
  }


  @Override
  public AccountDomainDTO save(final AccountDomain account) {
    return accountRepo.save(account.toDTO());
  }

  @Override
  public AccountDomainDTO save(final AccountDomainDTO account) {
    return accountRepo.save(account);
  }
}
