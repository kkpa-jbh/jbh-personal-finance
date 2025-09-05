package com.jbh.accounts_mgmt.accounts;

import java.util.Objects;
import java.util.UUID;

public record AccountId(UUID value) {

  public AccountId {
    Objects.requireNonNull(value, "AccountId cannot be null");
    validateFormat(value);
  }

  public static AccountId of(UUID value) {
    return new AccountId(value);
  }

  public static AccountId generate() {
    return new AccountId(UUID.randomUUID());
  }

  private static void validateFormat(UUID value) {
    // rules if needed
  }
}

