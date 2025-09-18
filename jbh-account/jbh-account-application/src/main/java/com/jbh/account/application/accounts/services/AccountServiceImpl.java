package com.jbh.account.application.accounts.services;

import static com.jbh.account.application.accounts.mappers.AccountMapper.toDTO;

import com.jbh.account.application.accounts.dto.AccountDTO;
import com.jbh.account.application.accounts.ports.output.AccountRepository;
import com.jbh.account.domain.entity.AccountDomain;

public class AccountServiceImpl implements AccountService {

  private final AccountRepository accountRepo;

  public AccountServiceImpl(final AccountRepository accountRepo) {
    this.accountRepo = accountRepo;
  }

  @Override
  public AccountDTO save(final AccountDomain account) {
    return accountRepo.save(toDTO(account));
  }

  @Override
  public AccountDTO save(final AccountDTO account) {
    return accountRepo.save(account);
  }
}
