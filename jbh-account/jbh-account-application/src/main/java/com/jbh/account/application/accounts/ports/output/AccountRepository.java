package com.jbh.account.application.accounts.ports.output;

import com.jbh.account.domain.vo.AccountDomainDTO;
import com.jbh.account.domain.vo.AccountId;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {

  Optional<AccountDomainDTO> findByAccountId(UUID userId, AccountId accountId);

  AccountDomainDTO save(AccountDomainDTO account);

}
