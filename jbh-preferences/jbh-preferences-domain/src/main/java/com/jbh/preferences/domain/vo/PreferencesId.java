package com.jbh.preferences.domain.vo;

import java.util.Objects;
import java.util.UUID;

public record PreferencesId(UUID value) {

  public PreferencesId {
    Objects.requireNonNull(value, "PreferencesId cannot be null");
  }

  public static PreferencesId of(final UUID value) {
    return new PreferencesId(value);
  }

  public static PreferencesId generate() {
    return new PreferencesId(UUID.randomUUID());
  }

  @Override
  public String toString() {
    if (value == null) {
      return null;
    }
    final String uuidString = value.toString();
    final String firstSegment = uuidString.substring(0, uuidString.indexOf('-'));
    return "PreferencesId[" + firstSegment + "]";
  }
}
