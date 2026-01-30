package com.jbh.preferences.domain.vo;

public enum Currency {
  COP("Colombian Peso", "COP"),
  USD("US Dollar", "USD");

  private final String displayName;
  private final String code;

  Currency(final String displayName, final String code) {
    this.displayName = displayName;
    this.code = code;
  }

  public static Currency fromCode(final String code) {
    if (code == null) {
      return defaultCurrency();
    }
    for (final Currency currency : values()) {
      if (currency.code.equals(code.toUpperCase(java.util.Locale.ROOT))) {
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
