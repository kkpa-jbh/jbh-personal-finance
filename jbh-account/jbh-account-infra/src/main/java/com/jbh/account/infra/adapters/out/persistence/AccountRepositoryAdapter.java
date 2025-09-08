package com.jbh.account.infra.adapters.out.persistence;

import com.jbh.account.application.accounts.ports.output.AccountRepository;
import com.jbh.account.domain.accounts.AccountDomain;
import com.jbh.account.domain.accounts.AccountId;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class AccountRepositoryAdapter implements AccountRepository {

  @Override
  public Optional<AccountDomain> findByAccountId(UUID userId, AccountId accountId) {
    String input = String.format("AccountRepositoryAdapter.findByAccountId called %s - %s", userId, accountId);
    System.out.println(input);
    return Optional.empty();
  }

  @Override
  public AccountDomain save(AccountDomain account) {
    return null;
  }

  @Override
  public boolean existsByAccountId(AccountId accountId) {
    return false;
  }
}
