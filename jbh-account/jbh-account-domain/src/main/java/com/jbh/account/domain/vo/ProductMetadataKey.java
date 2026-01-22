package com.jbh.account.domain.vo;

import java.util.List;

@SuppressWarnings("PMD.LongVariable")
public enum ProductMetadataKey {
  // Credit Card Input Metadata
  CREDIT_LIMIT,
  PAYMENT_DUE_DAY, // Fecha de Vencimiento de la cuenta (TC Dia del mes a pagar o corte).

  // Investment Input Metadata
  BROKER_NAME,
  COMMISSION_RATE,

  // CDT Input Metadata
  MATURITY_DATE,
  OPENING_DATE,
  TERM_LENGTH_IN_DAYS,

  // System Calculated Metadata (all types)
  COMMON_INITIAL_BALANCE,
  COMMON_IS_FULLY_WITHDRAWN,
  COMMON_FULLY_WITHDRAWN_DATE,
  COMMON_FULLY_WITHDRAWN_AT,

  // Core Loan Terms
  LOAN_PRINCIPAL_AMOUNT, // Original amount borrowed
  LOAN_INTEREST_RATE, // Annual interest rate (APR)
  LOAN_TOTAL_AMOUNT_PAID, // Total cumulative amount paid
  LOAN_PAYOFF_AMOUNT_TODAY, // Total amount to pay off loan today

  // Real Estate
  REAL_ESTATE_PURCHASE_DATE,
  REAL_ESTATE_PURCHASE_PRICE,
  REAL_ESTATE_PROPERTY_SIZE,
  REAL_ESTATE_RENTAL_INCOME,
  REAL_ESTATE_FINANCED_AMOUNT, // Monto que se financiará con crédito
  REAL_ESTATE_DOWN_PAYMENT_AMOUNT, // Valor Cuota Inicial (Calculado por el porcentaje)
  REAL_ESTATE_DOWN_PAYMENT_PERCENTAGE, // Porcentaje de cuota inicial (ej: 30%)
  REAL_ESTATE_DOWN_PAYMENT_PAID_TO_DATE, // Monto acumulado pagado de la cuota inicial hasta hoy
  ;

  private static final List<ProductMetadataKey> REQUIRED_LOAN_METADATA =
      List.of(LOAN_PRINCIPAL_AMOUNT, LOAN_TOTAL_AMOUNT_PAID, LOAN_PAYOFF_AMOUNT_TODAY);

  private static final List<ProductMetadataKey> REQUIRED_REAL_ESTATE_METADATA =
      List.of(
          REAL_ESTATE_PURCHASE_DATE,
          REAL_ESTATE_PURCHASE_PRICE,
          REAL_ESTATE_PROPERTY_SIZE,
          REAL_ESTATE_FINANCED_AMOUNT,
          REAL_ESTATE_DOWN_PAYMENT_PERCENTAGE,
          REAL_ESTATE_DOWN_PAYMENT_PAID_TO_DATE);

  public static List<ProductMetadataKey> findRequiredMetadataBy(final ProductType productType) {
    return switch (productType) {
      case REAL_ESTATE_INVESTMENT -> REQUIRED_REAL_ESTATE_METADATA;
      case LOAN -> REQUIRED_LOAN_METADATA;
      default -> throw new IllegalStateException("Unexpected value: " + productType);
    };
  }
}
