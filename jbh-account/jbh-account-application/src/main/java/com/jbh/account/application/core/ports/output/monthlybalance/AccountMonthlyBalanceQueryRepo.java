package com.jbh.account.application.core.ports.output.monthlybalance;

import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountPK;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface AccountMonthlyBalanceQueryRepo {

  Optional<MonthlyBalanceDTO> findByAccountIdYearAndMonth(
      AccountId accountId, Integer balanceYear, Integer balanceMonth);

  Optional<MonthlyBalanceDTO> findByAccountIdAndPeriod(AccountId accountId, YearMonth period);

  Optional<MonthlyBalanceDTO> findLastOfficialReport(AccountId accountId);

  BigDecimal sumNetProfitOfficialReported(AccountId accountId);

  List<MonthlyBalanceDTO> findNextBalancesFromPeriodInclusive(
      AccountId accountId, YearMonth currentPeriod);

  List<MonthlyBalanceDTO> findByAccountAndPeriods(
      AccountPK accountPK, YearMonth startPeriod, YearMonth endPeriod);

  List<MonthlyBalanceDTO> findAllByAccountIdUntilNow(AccountId accountId);
}
