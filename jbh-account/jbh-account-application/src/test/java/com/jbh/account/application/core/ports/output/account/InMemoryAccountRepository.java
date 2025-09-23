package com.jbh.account.application.core.ports.output.account;

import static com.jbh.account.domain.utils.MoneyUtils.JBH_ZERO;

import com.jbh.account.application.core.dto.AccountDTO;
import com.jbh.account.application.core.ports.output.AccountRepository;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountType;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryAccountRepository implements AccountRepository {

  public static final String DEFAULT_ACCOUNT_NAME = "Account 1";
  public static final AccountType DEFAULT_ACCOUNT_TYPE = AccountType.OTHER;
  private final Map<UUID, AccountDTO> storage = new HashMap<>();

  @Override
  public Optional<AccountDTO> findByUserAndAccountId(final UUID userId, final AccountId accountId) {
    AccountDTO account = storage.get(accountId.value());
    if (account != null && account.userId().equals(userId)) {
      return Optional.of(account);
    }
    account =
        AccountDTO.defaultBuilder(userId, accountId, DEFAULT_ACCOUNT_NAME, DEFAULT_ACCOUNT_TYPE)
            .currentBalance(JBH_ZERO)
            .movementBalance(JBH_ZERO)
            .build();
    storage.put(accountId.value(), account);
    return Optional.of(account);
  }

  @Override
  public Optional<AccountDTO> findByAccountId(final AccountId accountId) {
    return Optional.ofNullable(storage.get(accountId.value()));
  }

  @Override
  public AccountDTO save(final AccountDTO account) {
    storage.put(account.id().value(), account);
    return account;
  }

  public void clearStorage() {
    storage.clear();
  }

  public int size() {
    return storage.size();
  }
}
