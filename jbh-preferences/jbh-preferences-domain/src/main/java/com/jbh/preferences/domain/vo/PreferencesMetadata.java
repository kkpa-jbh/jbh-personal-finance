package com.jbh.preferences.domain.vo;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public record PreferencesMetadata(Map<String, Object> data) {

  @SuppressWarnings("PMD.UnusedAssignment")
  public PreferencesMetadata {
    data = data == null ? new HashMap<>() : new HashMap<>(data);
  }

  public static PreferencesMetadata empty() {
    return new PreferencesMetadata(new HashMap<>());
  }

  public static PreferencesMetadata fromMap(final Map<String, Object> data) {
    return new PreferencesMetadata(data);
  }

  public Map<String, Object> getData() {
    return Collections.unmodifiableMap(data);
  }

  public Object get(final String key) {
    return data.get(key);
  }

  public boolean containsKey(final String key) {
    return data.containsKey(key);
  }

  public boolean isEmpty() {
    return data.isEmpty();
  }

  public PreferencesMetadata with(final String key, final Object value) {
    Objects.requireNonNull(key, "Metadata key cannot be null");
    final Map<String, Object> newData = new HashMap<>(data);
    newData.put(key, value);
    return new PreferencesMetadata(newData);
  }

  public PreferencesMetadata without(final String key) {
    final Map<String, Object> newData = new HashMap<>(data);
    newData.remove(key);
    return new PreferencesMetadata(newData);
  }
}
