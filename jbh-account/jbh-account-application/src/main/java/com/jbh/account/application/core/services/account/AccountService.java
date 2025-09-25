package com.jbh.account.application.core.services.account;

import com.jbh.account.application.core.dto.AccountDTO;
import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.application.core.dto.MovementDTO;
import com.jbh.account.domain.entity.AccountDomain;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountPK;
import java.util.Optional;
import java.util.UUID;

/** Account Service Interface for CRUD operations */
public interface AccountService {
  Optional<AccountDTO> findByUserAndAccountId(UUID userId, AccountId accountId);

  Optional<AccountDTO> findByAccountId(AccountId accountId);

  AccountDTO save(AccountDTO account);

  AccountDTO save(AccountDomain account);

  AccountDTO syncByMonthlyReport(MonthlyBalanceDTO monthlyBalance);

  AccountDTO syncByMovement(
      AccountPK accountPK, MovementDTO movement, boolean isMonthOfficiallyReported);
}
