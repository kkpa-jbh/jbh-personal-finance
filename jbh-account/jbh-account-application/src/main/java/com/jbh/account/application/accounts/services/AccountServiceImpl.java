package com.jbh.account.application.accounts.services;

import com.jbh.account.application.accounts.dto.AccountDTO;
import com.jbh.account.application.accounts.mappers.AccountMapper;
import com.jbh.account.application.accounts.ports.output.AccountRepository;
import com.jbh.account.domain.entity.AccountDomain;

public class AccountServiceImpl implements AccountService {

  private final AccountRepository accountRepo;
  private final AccountMapper accountMapper;

  public AccountServiceImpl(final AccountRepository accountRepo) {
    this.accountRepo = accountRepo;
    this.accountMapper = new AccountMapper();
  }

  @Override
  public AccountDTO save(final AccountDomain account) {
    return accountRepo.save(accountMapper.toDTO(account));
  }

  @Override
  public AccountDTO save(final AccountDTO account) {
    return accountRepo.save(account);
  }
}
