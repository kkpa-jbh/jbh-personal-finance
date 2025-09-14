package com.jbh.account.application.accounts.ports.output;

import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountMonthlyBalanceDTO;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface AccountMonthlyBalanceRepository {

  Optional<AccountMonthlyBalanceDTO> findByAccountIdYearAndMonth(
      AccountId accountId, Integer balanceYear, Integer balanceMonth);

  AccountMonthlyBalanceDTO save(AccountMonthlyBalanceDTO accountMonthlyBalance);

  List<AccountMonthlyBalanceDTO> save(List<AccountMonthlyBalanceDTO> accountMonthlyBalance);

  List<AccountMonthlyBalanceDTO> findNextBalancesFromPeriodInclusive(
      AccountId accountId, YearMonth currentPeriod);
}
