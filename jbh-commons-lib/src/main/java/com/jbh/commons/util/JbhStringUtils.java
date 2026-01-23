package com.jbh.commons.util;

public final class JbhStringUtils {

  private JbhStringUtils() {}

  @SuppressWarnings("FORMAT_STRING_MANIPULATION")
  public static String buildFormattedJsonMessage(
      final String enTemplate, final String esTemplate, final Object... args) {
    final String formattedEn = String.format(java.util.Locale.ROOT, enTemplate, args);
    final String formattedEs = String.format(java.util.Locale.ROOT, esTemplate, args);
    return buildJsonMessage(formattedEn, formattedEs);
  }

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

  public static boolean isBlank(final String value) {
    return value == null || value.isBlank();
  }
}
