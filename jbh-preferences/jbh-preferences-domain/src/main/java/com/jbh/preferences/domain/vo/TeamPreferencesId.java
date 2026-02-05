package com.jbh.preferences.domain.vo;

import java.util.Objects;
import java.util.UUID;

public record TeamPreferencesId(UUID value) {

  public TeamPreferencesId {
    Objects.requireNonNull(value, "TeamPreferencesId cannot be null");
  }

  public static TeamPreferencesId of(final UUID value) {
    return new TeamPreferencesId(value);
  }

  public static TeamPreferencesId generate() {
    return new TeamPreferencesId(UUID.randomUUID());
  }

  @Override
  public String toString() {
    if (value == null) {
      return null;
    }
    final String uuidString = value.toString();
    final String firstSegment = uuidString.substring(0, uuidString.indexOf('-'));
    return "TeamPreferencesId[" + firstSegment + "]";
  }
}
