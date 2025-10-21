package com.jbh.account.infra.adapters.exceptions;

import com.jbh.account.domain.exceptions.BusinessExceptionType;
import com.jbh.account.domain.utils.JbhStringUtils;

@SuppressWarnings("PMD.LongVariable")
public enum AccountInfraBusinessExceptionType implements BusinessExceptionType {
  INVALID_RANGE_DATES_FOR_MONTHLY_BALANCES(
      "Invalid range dates for monthly balances",
      "Rango de fechas no válido para los saldos mensuales"),
  ;

  private final String en;
  private final String es;

  AccountInfraBusinessExceptionType(final String en, final String es) {
    this.en = en;
    this.es = es;
  }

  @Override
  public String getMessage() {
    return JbhStringUtils.buildJsonMessage(en, es);
  }
}
