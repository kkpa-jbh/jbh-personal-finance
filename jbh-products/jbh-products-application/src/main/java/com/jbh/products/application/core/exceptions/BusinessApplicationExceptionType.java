package com.jbh.products.application.core.exceptions;

import com.jbh.commons.exception.BusinessExceptionType;
import com.jbh.commons.util.JbhStringUtils;

@SuppressWarnings("PMD.LongVariable")
public enum BusinessApplicationExceptionType implements BusinessExceptionType {
  INVALID_RANGE_DATES_FOR_MONTHLY_BALANCES(
      "Invalid range dates for monthly balances",
      "Rango de fechas no válido para los saldos mensuales"),
  PRODUCT_NOT_FOUND(
      "Product not found for the given user and account ID",
      "Producto no encontrado para el usuario y ID de cuenta proporcionados"),
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
  INVALID_CATEGORY_LOAN_MOVEMENT(
      "Only transfer movements are allowed for loan products",
      "Solo se permiten movimientos de transferencia para productos de préstamo"),
  INVALID_LIQUIDATION_AMOUNT(
      "The amount to liquidate is invalid", "El importe a liquidar no es válido"),
  DISALLOWED_MOVEMENT_FOR_PRODUCT(
      "Product type not allowed to add a movement. Please use the transfer option instead.",
      "Este tipo de producto no puede agregar un movimiento. Por favor, use la opción de transferencia."),
  INVALID_PRODUCT_USE_CASE(
      "The action you want to do is not valid for this product type",
      "la acción que deseas realizar no es válida para este tipo de producto"),
  WITHDRAWAL_EXCEEDS_BALANCE(
      "The new withdrawal exceeds the monthly balance %s",
      "La retirada excede el saldo reportado del mes %s"),
  DEPOSIT_EXCEEDS_BALANCE(
      "The new deposit exceeds the monthly balance %s",
      "El depósito excede el saldo reportado del mes %s"),
  MONTHLY_BALANCE_PERIOD_NOT_IN_PAST(
      "The monthly balance period is not in the past",
      "El periodo de la cuenta no es en el pasado"),
  MONTHLY_BALANCE_NOT_CONSECUTIVE(
      "The monthly balance period is not consecutive. The last period was: %s",
      "El periodo de la cuenta no es consecutivo. La última periodo fue: %s"),
  SNAPSHOT_AFTER_OFFICIAL_REPORT(
      "Cannot add a snapshot after the monthly balance was officially reported",
      "No se puede añadir un snapshot después de que el balance anual fue reportado"),
  PRODUCT_NOT_ACTIVE(
      "Product is not active and cannot be modified",
      "El producto no está activo y no puede ser modificado"),
  PRODUCT_ALREADY_DELETED(
      "Product has already been deleted", "El producto ya ha sido eliminado");

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

  @Override
  public String getFormattedMessage(final Object... args) {
    return JbhStringUtils.buildFormattedJsonMessage(en, es, args);
  }
}
