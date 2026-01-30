package com.jbh.preferences.domain.vo;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LanguageTest {

  @Test
  void constructor_shouldCreateLanguageWithValidCode() {
    final Language language = new Language("fr");

    assertEquals("fr", language.code());
  }

  @Test
  void constructor_shouldNormalizeCodeToLowercase() {
    final Language language = new Language("FR");

    assertEquals("fr", language.code());
  }

  @Test
  void constructor_shouldThrowExceptionForNullCode() {
    assertThrows(NullPointerException.class, () -> new Language(null));
  }

  @Test
  void constructor_shouldThrowExceptionForBlankCode() {
    assertThrows(IllegalArgumentException.class, () -> new Language(""));
    assertThrows(IllegalArgumentException.class, () -> new Language("   "));
  }

  @Test
  void constructor_shouldThrowExceptionForCodeExceedingMaxLength() {
    final String longCode = "a".repeat(11);

    assertThrows(IllegalArgumentException.class, () -> new Language(longCode));
  }

  @Test
  void of_shouldReturnDefaultForNullCode() {
    final Language result = Language.of(null);

    assertEquals(Language.DEFAULT, result);
  }

  @Test
  void of_shouldReturnDefaultForBlankCode() {
    final Language result = Language.of("");

    assertEquals(Language.DEFAULT, result);
  }

  @Test
  void of_shouldCreateNewLanguageForValidCode() {
    final Language result = Language.of("fr");

    assertEquals("fr", result.code());
  }

  @Test
  void defaultLanguage_shouldReturnSpanish() {
    assertEquals(Language.SPANISH, Language.defaultLanguage());
  }

  @Test
  void isEnglish_shouldReturnTrueForEnglish() {
    assertTrue(Language.ENGLISH.isEnglish());
  }

  @Test
  void isEnglish_shouldReturnFalseForOtherLanguages() {
    assertFalse(Language.SPANISH.isEnglish());
    assertFalse(new Language("fr").isEnglish());
  }

  @Test
  void isSpanish_shouldReturnTrueForSpanish() {
    assertTrue(Language.SPANISH.isSpanish());
  }

  @Test
  void isSpanish_shouldReturnFalseForOtherLanguages() {
    assertFalse(Language.ENGLISH.isSpanish());
    assertFalse(new Language("fr").isSpanish());
  }
}
