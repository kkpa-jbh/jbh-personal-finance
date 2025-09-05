package com.jbh.account_app.accounts.ports.output;

import com.jbh.accounts_mgmt.accounts.AccountId;
import com.jbh.accounts_mgmt.accounts.AccountMonthlyBalanceDomain;
import java.util.Optional;

public interface AccountMonthlyBalanceRepository {

  Optional<AccountMonthlyBalanceDomain> findByAccountIdYearAndMonth(AccountId accountId, Integer balanceYear,
      Integer balanceMonth);

  void save(AccountMonthlyBalanceDomain accountMonthlyBalance);
}
