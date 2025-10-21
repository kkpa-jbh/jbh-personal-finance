package com.jbh.account.infra.adapters.in.rest.vo;

import static com.jbh.account.infra.adapters.exceptions.AccountInfraBusinessExceptionType.INVALID_RANGE_DATES_FOR_MONTHLY_BALANCES;

import com.jbh.account.domain.exceptions.AccountBusinessException;
import java.time.YearMonth;

public record MonthlyBalanceRequest(YearMonth startPeriod, YearMonth endPeriod) {

  public void validate() throws AccountBusinessException {
    if (startPeriod == null || endPeriod == null) {
      throw new AccountBusinessException(INVALID_RANGE_DATES_FOR_MONTHLY_BALANCES);
    }
  }
}
