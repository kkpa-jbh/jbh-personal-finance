package com.jbh.account.application.accounts.ports.output;

import com.jbh.accounts_mgmt.accounts.AccountId;
import com.jbh.accounts_mgmt.accounts.AccountMonthlyBalanceDomain;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface AccountMonthlyBalanceRepository {

  Optional<AccountMonthlyBalanceDomain> findByAccountIdYearAndMonth(AccountId accountId, Integer balanceYear,
      Integer balanceMonth);

  Optional<AccountMonthlyBalanceDomain> findByPeriod(YearMonth period);

  AccountMonthlyBalanceDomain save(AccountMonthlyBalanceDomain accountMonthlyBalance);

  List<AccountMonthlyBalanceDomain> save(List<AccountMonthlyBalanceDomain> accountMonthlyBalance);
}
