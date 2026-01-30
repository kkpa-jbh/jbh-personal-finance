package com.jbh.preferences.domain.vo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PreferencesMetadataTest {

  @Test
  void constructor_shouldCreateEmptyMapForNullData() {
    final PreferencesMetadata metadata = new PreferencesMetadata(null);

    assertTrue(metadata.isEmpty());
  }

  @Test
  void constructor_shouldCopyProvidedData() {
    final Map<String, Object> data = new HashMap<>();
    data.put("theme", "dark");

    final PreferencesMetadata metadata = new PreferencesMetadata(data);

    assertEquals("dark", metadata.get("theme"));
  }

  @Test
  void empty_shouldCreateEmptyMetadata() {
    final PreferencesMetadata metadata = PreferencesMetadata.empty();

    assertTrue(metadata.isEmpty());
  }

  @Test
  void fromMap_shouldCreateMetadataFromMap() {
    final Map<String, Object> data = Map.of("key", "value");

    final PreferencesMetadata metadata = PreferencesMetadata.fromMap(data);

    assertEquals("value", metadata.get("key"));
  }

  @Test
  void getData_shouldReturnUnmodifiableMap() {
    final PreferencesMetadata metadata = PreferencesMetadata.empty().with("key", "value");

    assertThrows(UnsupportedOperationException.class, () -> metadata.getData().put("new", "value"));
  }

  @Test
  void get_shouldReturnValueForExistingKey() {
    final PreferencesMetadata metadata = PreferencesMetadata.empty().with("theme", "dark");

    assertEquals("dark", metadata.get("theme"));
  }

  @Test
  void get_shouldReturnNullForNonExistingKey() {
    final PreferencesMetadata metadata = PreferencesMetadata.empty();

    assertNull(metadata.get("nonexistent"));
  }

  @Test
  void containsKey_shouldReturnTrueForExistingKey() {
    final PreferencesMetadata metadata = PreferencesMetadata.empty().with("theme", "dark");

    assertTrue(metadata.containsKey("theme"));
  }

  @Test
  void containsKey_shouldReturnFalseForNonExistingKey() {
    final PreferencesMetadata metadata = PreferencesMetadata.empty();

    assertFalse(metadata.containsKey("nonexistent"));
  }

  @Test
  void isEmpty_shouldReturnTrueForEmptyMetadata() {
    assertTrue(PreferencesMetadata.empty().isEmpty());
  }

  @Test
  void isEmpty_shouldReturnFalseForNonEmptyMetadata() {
    final PreferencesMetadata metadata = PreferencesMetadata.empty().with("key", "value");

    assertFalse(metadata.isEmpty());
  }

  @Test
  void with_shouldAddNewKeyValue() {
    final PreferencesMetadata original = PreferencesMetadata.empty();

    final PreferencesMetadata updated = original.with("theme", "dark");

    assertEquals("dark", updated.get("theme"));
    assertTrue(original.isEmpty());
  }

  @Test
  void with_shouldThrowExceptionForNullKey() {
    final PreferencesMetadata metadata = PreferencesMetadata.empty();

    assertThrows(NullPointerException.class, () -> metadata.with(null, "value"));
  }

  @Test
  void without_shouldRemoveKey() {
    final PreferencesMetadata original = PreferencesMetadata.empty()
        .with("theme", "dark")
        .with("language", "en");

    final PreferencesMetadata updated = original.without("theme");

    assertFalse(updated.containsKey("theme"));
    assertTrue(updated.containsKey("language"));
  }

  @Test
  void without_shouldHandleNonExistingKey() {
    final PreferencesMetadata metadata = PreferencesMetadata.empty().with("theme", "dark");

    final PreferencesMetadata updated = metadata.without("nonexistent");

    assertEquals("dark", updated.get("theme"));
  }
}
