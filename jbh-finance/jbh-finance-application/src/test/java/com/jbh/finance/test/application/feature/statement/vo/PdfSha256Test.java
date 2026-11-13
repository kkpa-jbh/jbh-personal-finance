package com.jbh.finance.test.application.feature.statement.vo;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jbh.finance.application.feature.statement.vo.PdfSha256;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class PdfSha256Test {

  @Test
  void shouldGiveKnownDigestWhenBytesAreAbc() {
    // Act
    final PdfSha256 digest = PdfSha256.of("abc".getBytes(UTF_8));

    // Assert
    assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", digest.value());
  }

  @Test
  void shouldGiveKnownDigestWhenBytesAreEmpty() {
    // Act
    final PdfSha256 digest = PdfSha256.of(new byte[0]);

    // Assert
    assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", digest.value());
  }

  @Test
  void shouldThrowWhenBytesAreNull() {
    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> PdfSha256.of(null));
  }

  @Test
  void shouldGiveDifferentValuesWhenBytesDiffer() {
    // Act
    final PdfSha256 first = PdfSha256.of("abc".getBytes(UTF_8));
    final PdfSha256 second = PdfSha256.of("abd".getBytes(UTF_8));

    // Assert
    assertNotEquals(first.value(), second.value());
    assertNotEquals(first, second);
  }

  @Test
  void shouldBeEqualWhenBytesAreTheSame() {
    // Act
    final PdfSha256 first = PdfSha256.of("same bytes".getBytes(UTF_8));
    final PdfSha256 second = PdfSha256.of("same bytes".getBytes(UTF_8));

    // Assert
    assertEquals(first, second);
    assertEquals(first.hashCode(), second.hashCode());
  }
}
