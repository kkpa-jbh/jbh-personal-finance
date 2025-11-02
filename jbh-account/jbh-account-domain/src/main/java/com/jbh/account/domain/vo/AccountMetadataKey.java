package com.jbh.account.domain.vo;

public enum AccountMetadataKey {
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
  FULLY_WITHDRAWN,
  FULLY_WITHDRAWN_DATE,
  FULLY_WITHDRAWN_AT,
  ;
}
