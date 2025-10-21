package com.jbh.account.application.core.ports.input;

import static com.jbh.account.application.core.exceptions.BusinessApplicationExceptionType.INVALID_RANGE_DATES_FOR_MONTHLY_BALANCES;

import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.application.core.exceptions.BusinessApplicationExceptionType;
import com.jbh.account.application.core.services.account.AccountService;
import com.jbh.account.application.core.services.monthlybalance.MonthlyBalanceService;
import com.jbh.account.application.core.usecases.FindMonthlyBalanceUseCase;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.vo.AccountPK;
import java.time.YearMonth;
import java.util.List;

public class FindMonthlyBalanceInputPort implements FindMonthlyBalanceUseCase {

  private final MonthlyBalanceService monthlyBalanceService;
  private final AccountService accountService;

  public FindMonthlyBalanceInputPort(
      final MonthlyBalanceService monthlyBalanceService, final AccountService accountService) {
    this.monthlyBalanceService = monthlyBalanceService;
    this.accountService = accountService;
  }

  @Override
  public List<MonthlyBalanceDTO> findByAccountAndPeriods(
      final AccountPK accountPK, final YearMonth startPeriod, final YearMonth endPeriod)
      throws AccountBusinessException {

    if (startPeriod == null || endPeriod == null) {
      throw new AccountBusinessException(INVALID_RANGE_DATES_FOR_MONTHLY_BALANCES);
    }

    if (startPeriod.isAfter(endPeriod)) {
      throw new AccountBusinessException(INVALID_RANGE_DATES_FOR_MONTHLY_BALANCES);
    }

    if (endPeriod.isAfter(YearMonth.now())) {
      throw new AccountBusinessException(INVALID_RANGE_DATES_FOR_MONTHLY_BALANCES);
    }

    accountService
        .findByUserAndAccountId(accountPK.userId(), accountPK.accountId())
        .orElseThrow(
            () ->
                new AccountBusinessException(BusinessApplicationExceptionType.ACCOUNTPK_MISMATCH));

    return monthlyBalanceService.findByAccountAndPeriods(accountPK, startPeriod, endPeriod);
  }
}
