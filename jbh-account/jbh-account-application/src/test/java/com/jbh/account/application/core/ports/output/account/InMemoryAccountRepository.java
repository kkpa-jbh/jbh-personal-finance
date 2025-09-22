package com.jbh.account.application.core.ports.output.account;

import com.jbh.account.application.core.dto.AccountDTO;
import com.jbh.account.application.core.ports.output.AccountRepository;
import com.jbh.account.domain.vo.AccountId;
import java.util.Optional;
import java.util.UUID;

public class InMemoryAccountRepository implements AccountRepository {

  @Override
  public Optional<AccountDTO> findByUserAndAccountId(final UUID userId, final AccountId accountId) {
    return Optional.empty();
  }

  @Override
  public Optional<AccountDTO> findByAccountId(final AccountId accountId) {
    return Optional.empty();
  }

  @Override
  public AccountDTO save(final AccountDTO account) {
    return account;
  }
}
