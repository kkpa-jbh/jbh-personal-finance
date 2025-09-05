package com.jbh.account_app.accounts.ports.output;

import com.jbh.accounts_mgmt.accounts.AccountDomain;
import com.jbh.accounts_mgmt.accounts.AccountId;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {

  Optional<AccountDomain> findByAccountId(UUID userId, AccountId accountId);

  AccountDomain save(AccountDomain account);

  boolean existsByAccountId(AccountId accountId);
}
