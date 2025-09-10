package com.jbh.account.application.accounts.ports.output;

import com.jbh.account.domain.entity.AccountMonthlyBalanceDomain;
import com.jbh.account.domain.vo.AccountId;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface AccountMonthlyBalanceRepository {

  Optional<AccountMonthlyBalanceDomain> findByAccountIdYearAndMonth(AccountId accountId, Integer balanceYear,
      Integer balanceMonth);


  AccountMonthlyBalanceDomain save(AccountMonthlyBalanceDomain accountMonthlyBalance);

  List<AccountMonthlyBalanceDomain> save(List<AccountMonthlyBalanceDomain> accountMonthlyBalance);

  List<AccountMonthlyBalanceDomain> findNextBalancesFromPeriodInclusive(AccountId accountId, YearMonth currentPeriod);
}
