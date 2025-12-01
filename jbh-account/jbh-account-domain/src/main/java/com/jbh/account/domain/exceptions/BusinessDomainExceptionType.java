package com.jbh.account.domain.exceptions;

import com.jbh.account.domain.utils.JbhStringUtils;

@SuppressWarnings("PMD.LongVariable")
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
  EXCEEDED_MAXIMUM_NET_GROWTH(
      "There is something wrong with the amounts inputs, They produces an unrealistic growth rate",
      "Hay algo mal con los cantidades de entrada, produce un tasa de crecimiento no realista"),

  // Account Creation Validation Errors
  MISSING_CREDIT_LIMIT(
      "CREDIT_LIMIT is required for CREDIT_CARD accounts",
      "CREDIT_LIMIT es requerido para cuentas CREDIT_CARD"),
  INVALID_CREDIT_LIMIT_TYPE(
      "CREDIT_LIMIT must be a BigDecimal", "CREDIT_LIMIT debe ser un BigDecimal"),
  INVALID_CREDIT_LIMIT_VALUE(
      "CREDIT_LIMIT must be greater than zero", "CREDIT_LIMIT debe ser mayor que cero"),
  MISSING_PAYMENT_DUE_DAY(
      "PAYMENT_DUE_DAY is required for CREDIT_CARD accounts",
      "PAYMENT_DUE_DAY es requerido para cuentas CREDIT_CARD"),
  INVALID_PAYMENT_DUE_DAY_TYPE(
      "The payment due day must be an Integer", "PAYMENT_DUE_DAY debe ser un Integer"),
  INVALID_PAYMENT_DUE_DAY_RANGE(
      "PAYMENT_DUE_DAY must be between 1 and 31", "PAYMENT_DUE_DAY debe estar entre 1 y 31"),
  MISSING_BROKER_NAME(
      "The broker name is required for INVESTMENT accounts",
      "El nombre del broker es requerido para cuentas INVESTMENT"),
  MISSING_COMMISSION_RATE(
      "It's required to provide the commission rate",
      "Es requerido proveer el porcentaje de comisión"),
  MISSING_MATURITY_DATE(
      "It's required to provide the maturity date", "Es requerido proveer la fecha de vencimiento"),
  INVALID_MATURITY_DATE_TYPE(
      "The maturity date has an invalid format",
      "La fecha de vencimiento tiene un formato inválido"),

  EMPTY_LOAN_PRINCIPAL_AMOUNT(
      "The amount borrowed has not been set", "El valor del préstamo no ha sido definido."),
  EMPTY_LOAN_TOTAL_AMOUNT_PAID(
      "The total amount paid towards the loan has not been set",
      "El valor total pagado hacia el préstamo no ha sido definido."),
  EMPTY_LOAN_PAYOFF_AMOUNT(
      "The payoff amount for the loan has not been set",
      "El valor pendiente de pagar el préstamo, no ha sido definido."),
  PAYMENT_AMOUNT_GREATER_PAYOFF(
      "The amount paid is greater than the payoff amount",
      "El monto pagado es mayor al monto pendiente de pagar"),
  MISSING_METADATA("Missing required metadata: %s", "Falta el metadato requerido: %s"),
  INVALID_PERCENTAGE("Percentage must be between 0 and 100", "Porcentaje debe estar entre 0 y 100");

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

  @Override
  public String getFormattedMessage(final Object... args) {
    return JbhStringUtils.buildFormattedJsonMessage(en, es, args);
  }
}
