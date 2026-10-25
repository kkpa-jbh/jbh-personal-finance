package com.jbh.preferences.application.core.vo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CallerTokenTest {

  @Test
  void shouldKeepTheValue() {
    assertEquals("Bearer abc", new CallerToken("Bearer abc").value());
  }

  @Test
  void shouldNeverPrintTheToken() {
    assertFalse(new CallerToken("Bearer secret-value").toString().contains("secret-value"));
  }

  @Test
  void shouldRejectANullValue() {
    assertThrows(NullPointerException.class, () -> new CallerToken(null));
  }
}
