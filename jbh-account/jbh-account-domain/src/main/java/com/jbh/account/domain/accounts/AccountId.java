package com.jbh.account.domain.accounts;

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
  
}

