package com.jbh.account.application.accounts.ports;

import com.jbh.account.application.accounts.dto.AccountDTO;
import com.jbh.account.application.accounts.ports.output.AccountRepository;
import com.jbh.account.domain.vo.AccountId;
import java.util.Optional;
import java.util.UUID;

public class AccountRepositoryTest implements AccountRepository {

  @Override
  public Optional<AccountDTO> findByAccountId(final UUID userId, final AccountId accountId) {
    return Optional.empty();
  }

  @Override
  public AccountDTO save(final AccountDTO account) {
    return account;
  }
}
