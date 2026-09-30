package com.jbh.finance.application.feature.statement.dto;

import java.math.BigDecimal;

/**
 * Settings of the movement classification.
 *
 * @param batchSize number of questions sent to the classifier in one call
 * @param minConfidence rows under this confidence are marked for review (0 to 1)
 */
public record ClassificationSettings(int batchSize, BigDecimal minConfidence) {

  private static final int MIN_BATCH_SIZE = 1;

  public ClassificationSettings {
    if (batchSize < MIN_BATCH_SIZE) {
      throw new IllegalArgumentException("Batch size must be at least " + MIN_BATCH_SIZE);
    }
    if (minConfidence == null
        || minConfidence.compareTo(BigDecimal.ZERO) < 0
        || minConfidence.compareTo(BigDecimal.ONE) > 0) {
      throw new IllegalArgumentException("Min confidence must be between 0 and 1");
    }
  }
}
