package com.jbh.account.domain.utils;

public final class JbhStringUtils {

  private JbhStringUtils() {}

  public static String buildJsonMessage(final String en, final String es) {
    return """
           {
             "en": "%s",
             "es": "%s"
           }
           """
        .formatted(escapeJson(en), escapeJson(es));
  }

  private static String escapeJson(final String value) {
    // Very basic escaping for quotes and backslashes
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
