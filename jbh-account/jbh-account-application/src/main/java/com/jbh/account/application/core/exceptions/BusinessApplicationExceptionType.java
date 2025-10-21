package com.jbh.account.application.core.exceptions;

import com.jbh.account.domain.exceptions.BusinessExceptionType;
import com.jbh.account.domain.utils.JbhStringUtils;

@SuppressWarnings("PMD.LongVariable")
public enum BusinessApplicationExceptionType implements BusinessExceptionType {
  INVALID_RANGE_DATES_FOR_MONTHLY_BALANCES(
      "Invalid range dates for monthly balances",
      "Rango de fechas no válido para los saldos mensuales"),
  ACCOUNTPK_MISMATCH(
      "What are you trying to do? The account id does not match the user id",
      "¿Qué estás intentando hacer? El ID de la cuenta no coincide con el ID de usuario"),
  ;

  private final String en;
  private final String es;

  BusinessApplicationExceptionType(final String en, final String es) {
    this.en = en;
    this.es = es;
  }

  @Override
  public String getMessage() {
    return JbhStringUtils.buildJsonMessage(en, es);
  }
}
