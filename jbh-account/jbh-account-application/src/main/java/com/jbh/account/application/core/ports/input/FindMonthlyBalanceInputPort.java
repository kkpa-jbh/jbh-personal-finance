package com.jbh.account.application.core.ports.input;

import static com.jbh.account.application.core.exceptions.BusinessApplicationExceptionType.INVALID_RANGE_DATES_FOR_MONTHLY_BALANCES;

import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.application.core.services.account.AccountService;
import com.jbh.account.application.core.services.monthlybalance.MonthlyBalanceService;
import com.jbh.account.application.core.usecases.FindMonthlyBalanceUseCase;
import com.jbh.account.domain.vo.ProductPK;
import com.jbh.commons.exception.BusinessException;
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
      final ProductPK accountPK, final YearMonth startPeriod, final YearMonth endPeriod)
      throws BusinessException {

    if (startPeriod == null || endPeriod == null) {
      throw new BusinessException(INVALID_RANGE_DATES_FOR_MONTHLY_BALANCES);
    }

    if (startPeriod.isAfter(endPeriod)) {
      throw new BusinessException(INVALID_RANGE_DATES_FOR_MONTHLY_BALANCES);
    }

    if (endPeriod.isAfter(YearMonth.now())) {
      throw new BusinessException(INVALID_RANGE_DATES_FOR_MONTHLY_BALANCES);
    }

    accountService.findByUserAndAccountId(accountPK.userId(), accountPK.accountId());

    return monthlyBalanceService.findByAccountAndPeriods(accountPK, startPeriod, endPeriod);
  }
}
