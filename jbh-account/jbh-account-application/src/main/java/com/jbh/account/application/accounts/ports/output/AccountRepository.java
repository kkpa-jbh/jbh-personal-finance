package com.jbh.account.application.accounts.ports.output;

import com.jbh.account.application.accounts.dto.AccountDTO;
import com.jbh.account.domain.vo.AccountId;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {

  Optional<AccountDTO> findByAccountId(UUID userId, AccountId accountId);

  AccountDTO save(AccountDTO account);
}
