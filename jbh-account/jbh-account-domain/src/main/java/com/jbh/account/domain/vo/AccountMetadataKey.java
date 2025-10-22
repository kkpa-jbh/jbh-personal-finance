package com.jbh.account.domain.vo;

public enum AccountMetadataKey {
  // Credit Card Input Metadata
  CREDIT_LIMIT,
  PAYMENT_DUE_DAY,

  // Investment Input Metadata
  BROKER_NAME,
  ACCOUNT_NUMBER,

  // System Calculated Metadata (all types)
  FULLY_WITHDRAWN,
  FULLY_WITHDRAWN_DATE,
  FULLY_WITHDRAWN_AT,
}
