package com.jbh.account.application.core.ports.output.account;

import com.jbh.account.application.core.dto.AccountDTO;
import com.jbh.account.application.core.ports.output.AccountRepository;
import com.jbh.account.domain.vo.AccountId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InMemoryAccountRepository implements AccountRepository {

  private static final Logger log = LoggerFactory.getLogger(InMemoryAccountRepository.class);
  private final Map<UUID, AccountDTO> storage = new HashMap<>();

  @Override
  public Optional<AccountDTO> findByUserAndAccountId(final UUID userId, final AccountId accountId) {
    final AccountDTO account = storage.get(accountId.value());
    if (account != null && account.userId().equals(userId)) {
      return Optional.of(account);
    }
    /*
    account =
        AccountDTO.defaultBuilder(userId, accountId, DEFAULT_ACCOUNT_NAME, DEFAULT_ACCOUNT_TYPE)
            .currentBalance(JBH_ZERO)
            .movementBalance(JBH_ZERO)
            .build();
    storage.put(accountId.value(), account);

     */
    return Optional.empty();
  }

  @Override
  public Optional<AccountDTO> findByAccountId(final AccountId accountId) {
    return Optional.ofNullable(storage.get(accountId.value()));
  }

  @Override
  public AccountDTO save(final AccountDTO account) {
    storage.put(account.id().value(), account);
    log.warn("Updated Account " + account);
    return account;
  }

  public void clearStorage() {
    storage.clear();
  }

  public int size() {
    return storage.size();
  }
}
