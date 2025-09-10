package com.jbh.account.application.accounts.ports;

import com.jbh.account.application.accounts.ports.output.AccountRepository;
import com.jbh.account.domain.vo.AccountDomainDTO;
import com.jbh.account.domain.vo.AccountId;
import java.util.Optional;
import java.util.UUID;

public class AccountRepositoryTest implements AccountRepository {

  @Override
  public Optional<AccountDomainDTO> findByAccountId(UUID userId, AccountId accountId) {
    return Optional.empty();
  }

  @Override
  public AccountDomainDTO save(AccountDomainDTO account) {
    return account;
  }
}
