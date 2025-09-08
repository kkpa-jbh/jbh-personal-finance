package com.jbh.account.application.accounts.ports.output;

import com.jbh.account.domain.accounts.AccountDomain;
import com.jbh.account.domain.accounts.AccountId;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {

  Optional<AccountDomain> findByAccountId(UUID userId, AccountId accountId);

  AccountDomain save(AccountDomain account);

  boolean existsByAccountId(AccountId accountId);
}
