package com.jbh.account.domain.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jbh.commons.util.JbhBooleanUtils;
import com.jbh.commons.util.JbhStringUtils;
import org.junit.jupiter.api.Test;

public class JbhStringUtilsTest {

  @Test
  public void shouldBuildJsonMessageWithBasicStrings() {
    final String result = buildJsonMessage("Hello", "Hola");

    final String expected =
        """
           {
             "en": "Hello",
             "es": "Hola"
           }
           """;

    assertEquals(expected, result);

    assertTrue(JbhBooleanUtils.isTrue(true));
    assertFalse(JbhBooleanUtils.isTrue(null));
  }

  private String buildJsonMessage(final String en, final String es) {
    return JbhStringUtils.buildJsonMessage(en, es);
  }

  @Test
  public void shouldBuildJsonMessageWithEmptyStrings() {
    final String result = buildJsonMessage("", "");

    final String expected =
        """
           {
             "en": "",
             "es": ""
           }
           """;

    assertEquals(expected, result);
  }

  @Test
  public void shouldEscapeQuotesInJsonMessage() {
    final String result = buildJsonMessage("Say \"Hello\"", "Diga \"Hola\"");

    final String expected =
        """
           {
             "en": "Say \\"Hello\\"",
             "es": "Diga \\"Hola\\""
           }
           """;

    assertEquals(expected, result);
  }

  @Test
  public void shouldEscapeBackslashesInJsonMessage() {
    final String result = buildJsonMessage("Path\\to\\file", "Ruta\\al\\archivo");

    final String expected =
        """
           {
             "en": "Path\\\\to\\\\file",
             "es": "Ruta\\\\al\\\\archivo"
           }
           """;

    assertEquals(expected, result);
  }

  @Test
  public void shouldEscapeBothQuotesAndBackslashesInJsonMessage() {
    final String result = buildJsonMessage("Path\\to\\\"file\"", "Ruta\\al\\\"archivo\"");

    final String expected =
        """
           {
             "en": "Path\\\\to\\\\\\"file\\"",
             "es": "Ruta\\\\al\\\\\\"archivo\\""
           }
           """;

    assertEquals(expected, result);
  }

  @Test
  public void shouldHandleSpecialCharactersInJsonMessage() {
    final String result = buildJsonMessage("Line 1\nLine 2", "Línea 1\nLínea 2");

    final String expected =
        """
           {
             "en": "Line 1\nLine 2",
             "es": "Línea 1\nLínea 2"
           }
           """;

    assertEquals(expected, result);
  }

  @Test
  public void shouldHandleLongStringsInJsonMessage() {
    final String longEnglish =
        "This is a very long English message that contains multiple words and should be properly formatted in JSON";
    final String longSpanish =
        "Este es un mensaje muy largo en español que contiene múltiples palabras y debe ser formateado correctamente en JSON";

    final String result = buildJsonMessage(longEnglish, longSpanish);

    final String expected =
        """
           {
             "en": "This is a very long English message that contains multiple words and should be properly formatted in JSON",
             "es": "Este es un mensaje muy largo en español que contiene múltiples palabras y debe ser formateado correctamente en JSON"
           }
           """;

    assertEquals(expected, result);
  }

  @Test
  public void shouldHandleIsBlank() {
    assertEquals(true, JbhStringUtils.isBlank(""));
    assertEquals(true, JbhStringUtils.isBlank("   "));
    assertEquals(false, JbhStringUtils.isBlank("Hello"));
    assertEquals(false, JbhStringUtils.isBlank(" Hello "));
  }
}
