package com.jbh.finance.test.application.feature.statement.dto;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jbh.finance.application.feature.statement.dto.ClassificationSettings;
import java.math.BigDecimal;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class ClassificationSettingsTest {

  @Test
  void shouldThrowWhenBatchSizeIsZero() {
    // Arrange
    final BigDecimal minConfidence = new BigDecimal("0.7");

    // Act & Assert
    assertThrows(
        IllegalArgumentException.class, () -> new ClassificationSettings(0, minConfidence));
  }

  @Test
  void shouldThrowWhenConfidenceIsNegative() {
    // Arrange
    final BigDecimal minConfidence = new BigDecimal("-0.1");

    // Act & Assert
    assertThrows(
        IllegalArgumentException.class, () -> new ClassificationSettings(20, minConfidence));
  }

  @Test
  void shouldThrowWhenConfidenceIsAboveOne() {
    // Arrange
    final BigDecimal minConfidence = new BigDecimal("1.1");

    // Act & Assert
    assertThrows(
        IllegalArgumentException.class, () -> new ClassificationSettings(20, minConfidence));
  }

  @Test
  void shouldThrowWhenConfidenceIsNull() {
    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> new ClassificationSettings(20, null));
  }

  @Test
  void shouldCreateSettingsWhenValuesAreAtTheLimits() {
    // Act & Assert
    assertDoesNotThrow(() -> new ClassificationSettings(1, BigDecimal.ZERO));
    assertDoesNotThrow(() -> new ClassificationSettings(20, BigDecimal.ONE));
  }
}
