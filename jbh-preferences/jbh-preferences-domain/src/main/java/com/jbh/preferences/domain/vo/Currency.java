package com.jbh.preferences.domain.vo;

@SuppressWarnings("IMPROPER_UNICODE")
public enum Currency {
  COP("Colombian Peso", "COP"),
  USD("US Dollar", "USD");

  private final String displayName;
  private final String code;

  Currency(final String displayName, final String code) {
    this.displayName = displayName;
    this.code = code;
  }

  @SuppressWarnings("IMPROPER_UNICODE")
  public static Currency fromCode(final String code) {
    if (code == null) {
      return defaultCurrency();
    }
    final String normalizedCode = code.toUpperCase(java.util.Locale.ROOT);
    for (final Currency currency : values()) {
      if (currency.code.equals(normalizedCode)) {
        return currency;
      }
    }
    throw new IllegalArgumentException("Unknown currency code: " + code);
  }

  public static Currency defaultCurrency() {
    return COP;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getCode() {
    return code;
  }
}
