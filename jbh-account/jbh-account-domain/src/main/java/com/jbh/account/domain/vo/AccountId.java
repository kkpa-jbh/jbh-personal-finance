package com.jbh.account.domain.vo;

import java.util.Objects;
import java.util.UUID;

public record AccountId(UUID value) {

  public AccountId {
    Objects.requireNonNull(value, "AccountId cannot be null");
  }

  public static AccountId of(final UUID value) {
    return new AccountId(value);
  }

  public static AccountId generate() {
    return new AccountId(UUID.randomUUID());
  }

  @Override
  public String toString() {
    if (value == null) {
      return null;
    }
    final String uuidString = value.toString();
    final String firstSegment = uuidString.substring(0, uuidString.indexOf('-'));
    return "AccountId[" + firstSegment + "]";
  }
}
