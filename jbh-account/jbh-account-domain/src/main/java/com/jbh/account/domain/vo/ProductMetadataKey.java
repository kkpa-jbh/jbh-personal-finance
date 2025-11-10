package com.jbh.account.domain.vo;

public enum ProductMetadataKey {
  INITIAL_BALANCE,

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
  IS_FULLY_WITHDRAWN,
  FULLY_WITHDRAWN_DATE,
  FULLY_WITHDRAWN_AT,

  // Core Loan Terms
  LOAN_PRINCIPAL_AMOUNT, // Original amount borrowed
  LOAN_INTEREST_RATE, // Annual interest rate (APR)
  LOAN_TOTAL_AMOUNT_PAID, // Total cumulative amount paid

  LOAN_PAYOFF_AMOUNT, // Total amount to pay off loan today
  ;
}
