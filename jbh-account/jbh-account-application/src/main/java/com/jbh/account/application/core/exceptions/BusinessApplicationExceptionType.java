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
  INVALID_TRANSFER_RECIPIENT(
      "The transfer recipient is invalid", "El destinatario de la transferencia no es válido"),
  INVALID_TRANSFER_AMOUNT(
      "The amount to transfer is invalid", "El importe a transferir no es válido"),
  INVALID_TRANSFER_DATE("The transfer date is invalid", "La fecha de transferencia no es válida"),
  CDT_MOVEMENTS_EXCEEDED(
      "There is already a movement for this account", "Ya hay un movimiento para esta cuenta"),
  CDT_WRONG_INCOME_CATEGORY(
      "The income category is not initial balance",
      "La categoría de ingreso no es balance inicial"),
  INVALID_CATEGORY_INVESTMENT_WITHDRAWAL(
      "You cannot withdraw the full investment by using this option",
      "No puedes retirar el total de la inversión usando esta opción"),
  INVALID_LIQUIDATION_AMOUNT(
      "The amount to liquidate is invalid", "El importe a liquidar no es válido"),
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
