package com.jbh.preferences.domain.vo;

import java.util.Locale;
import java.util.Objects;

public record Language(String code) {

  public static final Language ENGLISH = new Language("en");
  public static final Language SPANISH = new Language("es");
  public static final Language DEFAULT = SPANISH;

  private static final int MAX_CODE_LENGTH = 10;

  @SuppressWarnings("PMD.UnusedAssignment")
  public Language {
    Objects.requireNonNull(code, "Language code cannot be null");
    if (code.isBlank()) {
      throw new IllegalArgumentException("Language code cannot be blank");
    }
    if (code.length() > MAX_CODE_LENGTH) {
      throw new IllegalArgumentException(
          "Language code cannot exceed " + MAX_CODE_LENGTH + " characters");
    }
    code = code.toLowerCase(Locale.ROOT);
  }

  public static Language of(final String code) {
    if (code == null || code.isBlank()) {
      return DEFAULT;
    }
    return new Language(code);
  }

  public static Language defaultLanguage() {
    return DEFAULT;
  }

  public boolean isEnglish() {
    return ENGLISH.code.equals(code);
  }

  public boolean isSpanish() {
    return SPANISH.code.equals(code);
  }
}
