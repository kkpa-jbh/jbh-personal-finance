package com.jbh.finance.infra;

public final class LogSanitizer {

  private LogSanitizer() {}

  public static String sanitize(final String input) {
    if (input == null) {
      return null;
    }
    return input.replaceAll("[\r\n\t]", "_");
  }

  public static String sanitize(final Object input) {
    if (input == null) {
      return null;
    }
    return input.toString().replaceAll("[\r\n\t]", "_");
  }
}
