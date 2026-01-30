package com.jbh.products.infra.adapters.in.rest.vo;

import static com.jbh.products.infra.adapters.exceptions.AccountInfraBusinessExceptionType.INVALID_RANGE_DATES_FOR_MONTHLY_BALANCES;

import com.jbh.commons.exception.BusinessException;
import java.time.YearMonth;

public record MonthlyBalanceRequest(YearMonth startPeriod, YearMonth endPeriod) {

  public void validate() throws BusinessException {
    if (startPeriod == null || endPeriod == null) {
      throw new BusinessException(INVALID_RANGE_DATES_FOR_MONTHLY_BALANCES);
    }
  }
}
