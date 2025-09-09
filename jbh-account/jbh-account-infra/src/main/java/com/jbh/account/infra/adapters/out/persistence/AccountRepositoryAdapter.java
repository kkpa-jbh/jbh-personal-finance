package com.jbh.account.infra.adapters.out.persistence;

import static com.jbh.account.infra.LogSanitizer.sanitize;

import com.jbh.account.application.accounts.ports.output.AccountRepository;
import com.jbh.account.domain.accounts.AccountDomain;
import com.jbh.account.domain.accounts.AccountId;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class AccountRepositoryAdapter implements AccountRepository {

  private static final Logger LOG = LoggerFactory.getLogger(AccountRepositoryAdapter.class);

  @Override
  public Optional<AccountDomain> findByAccountId(final UUID userId, final AccountId accountId) {
    final String input = String.format("AccountRepositoryAdapter.findByAccountId called %s - %s", userId, accountId);
    LOG.info(sanitize(input));
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
