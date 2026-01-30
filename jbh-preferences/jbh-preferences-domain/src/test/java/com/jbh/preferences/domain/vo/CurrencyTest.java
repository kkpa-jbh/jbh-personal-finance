package com.jbh.preferences.domain.vo;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CurrencyTest {

  @Test
  void fromCode_shouldReturnUsdForNullCode() {
    final Currency result = Currency.fromCode(null);

    assertEquals(Currency.defaultCurrency(), result);
  }

  @Test
  void fromCode_shouldReturnCurrencyForValidCode() {
    assertEquals(Currency.COP, Currency.fromCode("COP"));
    assertEquals(Currency.USD, Currency.fromCode("USD"));
  }

  @Test
  void fromCode_shouldReturnCurrencyForLowercaseCode() {
    assertEquals(Currency.COP, Currency.fromCode("cop"));
    assertEquals(Currency.USD, Currency.fromCode("usd"));
  }

  @Test
  void fromCode_shouldThrowExceptionForInvalidCode() {
    assertThrows(IllegalArgumentException.class, () -> Currency.fromCode("INVALID"));
  }

  @Test
  void getDisplayName_shouldReturnCorrectDisplayName() {
    assertEquals("Colombian Peso", Currency.COP.getDisplayName());
    assertEquals("US Dollar", Currency.USD.getDisplayName());
  }

  @Test
  void getCode_shouldReturnCorrectCode() {
    assertEquals("COP", Currency.COP.getCode());
    assertEquals("USD", Currency.USD.getCode());
  }
}
