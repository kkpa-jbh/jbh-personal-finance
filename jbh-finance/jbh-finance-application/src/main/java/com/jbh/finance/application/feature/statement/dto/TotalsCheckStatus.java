package com.jbh.finance.application.feature.statement.dto;

/** Result of comparing the movements with the balance change printed on the statement. */
public enum TotalsCheckStatus {
  MATCH,
  MISMATCH,
  NOT_AVAILABLE
}
