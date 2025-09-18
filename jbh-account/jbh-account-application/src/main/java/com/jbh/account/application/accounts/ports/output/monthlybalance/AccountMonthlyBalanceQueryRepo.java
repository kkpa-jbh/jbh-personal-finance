package com.jbh.account.application.accounts.ports.output.monthlybalance;

import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountMonthlyBalanceDTO;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface AccountMonthlyBalanceQueryRepo {

  Optional<AccountMonthlyBalanceDTO> findByAccountIdYearAndMonth(
      AccountId accountId, Integer balanceYear, Integer balanceMonth);

  Optional<AccountMonthlyBalanceDTO> findByAccountIdAndPeriod(
      AccountId accountId, YearMonth period);

  List<AccountMonthlyBalanceDTO> findNextBalancesFromPeriodInclusive(
      AccountId accountId, YearMonth currentPeriod);
}
