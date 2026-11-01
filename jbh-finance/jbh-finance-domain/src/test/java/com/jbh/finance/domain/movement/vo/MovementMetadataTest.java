package com.jbh.finance.domain.movement.vo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class MovementMetadataTest {

  @Test
  void shouldStoreConfidenceAndNeedsReviewWhenPuttingStatementReview() {
    // Arrange
    final MovementMetadata metadata = MovementMetadata.createEmpty();

    // Act
    metadata.putStatementReview(new MovementReviewVO(new BigDecimal("0.9"), false));

    // Assert
    assertEquals(new BigDecimal("0.9"), metadata.get(MovementMetadataKey.STATEMENT_CONFIDENCE));
    assertEquals(false, metadata.get(MovementMetadataKey.STATEMENT_NEEDS_REVIEW));
  }

  @Test
  void shouldStoreNeedsReviewTrueWhenPuttingLowConfidenceReview() {
    // Arrange
    final MovementMetadata metadata = MovementMetadata.createEmpty();

    // Act
    metadata.putStatementReview(new MovementReviewVO(new BigDecimal("0.4"), true));

    // Assert
    assertEquals(new BigDecimal("0.4"), metadata.get(MovementMetadataKey.STATEMENT_CONFIDENCE));
    assertEquals(true, metadata.get(MovementMetadataKey.STATEMENT_NEEDS_REVIEW));
  }
}
