package com.jbh.preferences.domain.vo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class PreferencesIdTest {

  @Test
  void constructor_shouldCreateIdWithValidUuid() {
    final UUID uuid = UUID.randomUUID();

    final PreferencesId id = new PreferencesId(uuid);

    assertEquals(uuid, id.value());
  }

  @Test
  void constructor_shouldThrowExceptionForNullUuid() {
    assertThrows(NullPointerException.class, () -> new PreferencesId(null));
  }

  @Test
  void of_shouldCreateIdFromUuid() {
    final UUID uuid = UUID.randomUUID();

    final PreferencesId id = PreferencesId.of(uuid);

    assertEquals(uuid, id.value());
  }

  @Test
  void generate_shouldCreateIdWithRandomUuid() {
    final PreferencesId id1 = PreferencesId.generate();
    final PreferencesId id2 = PreferencesId.generate();

    assertNotNull(id1.value());
    assertNotNull(id2.value());
    assertNotEquals(id1.value(), id2.value());
  }

  @Test
  void toString_shouldReturnFormattedString() {
    final UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    final PreferencesId id = new PreferencesId(uuid);

    final String result = id.toString();

    assertEquals("PreferencesId[550e8400]", result);
  }
}
