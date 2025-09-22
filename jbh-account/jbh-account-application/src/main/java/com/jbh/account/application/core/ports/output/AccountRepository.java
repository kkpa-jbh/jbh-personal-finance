package com.jbh.account.application.core.ports.output;

import com.jbh.account.application.core.dto.AccountDTO;
import com.jbh.account.domain.vo.AccountId;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {

  Optional<AccountDTO> findByUserAndAccountId(UUID userId, AccountId accountId);

  Optional<AccountDTO> findByAccountId(AccountId accountId);

  AccountDTO save(AccountDTO account);
}
