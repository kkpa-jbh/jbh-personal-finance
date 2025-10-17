package com.jbh.account.domain.exceptions;

import com.jbh.account.domain.utils.JbhStringUtils;

public enum BusinessDomainExceptionType implements BusinessExceptionType {
  EMPTY_MOVEMENTS("Movements cannot be empty", "Los movimientos no pueden estar vacíos"),
  ACCOUNT_MISMATCH(
      "Account ID mismatch when applying movement",
      "ID de cuenta no coincide al aplicar movimiento"),
  INSUFFICIENT_FUNDS("Insufficient effective balance", "Fondos efectivos insuficientes"),
  INVALID_MOV_DATE_MONTHLY_PERIOD(
      "Movement date is not in the same period as the monthly balance",
      "Fecha de movimiento no está en el mismo periodo que el balance mensual"),
  INVALID_CATEGORY_BALANCE_SNAPSHOT(
      "Category cannot be provided for balance snapshots",
      "Categoría no puede ser proporcionada para balance snapshots"),
  EMPTY_MOVEMENT_DATE("Movement date cannot be null", "Fecha de movimiento no puede ser nula"),
  EMPTY_MOVEMENT_TYPE("Movement type cannot be null", "Tipo de movimiento no puede ser nulo"),
  FUTURE_MOVEMENT_DATE(
      "Movement date cannot be in the future", "Fecha de movimiento no puede ser futura"),
  EMPTY_AMOUNT("Total amount cannot be null", "Cantidad total no puede ser nula"),
  EMPTY_CATEGORY("Category cannot be null", "Categoría no puede ser nula"),
  DEPOSIT_AMOUNT_NOT_POSITIVE(
      "Deposit amount cannot be negative", "Cantidad de depósito no puede ser negativa"),
  WITHDRAWAL_AMOUNT_NOT_POSITIVE(
      "Withdrawal amount cannot be positive", "Cantidad de retiro no puede ser positiva"),
  ;

  private final String en;
  private final String es;

  BusinessDomainExceptionType(final String en, final String es) {
    this.en = en;
    this.es = es;
  }

  @Override
  public String getMessage() {
    return JbhStringUtils.buildJsonMessage(en, es);
  }
}
