package com.jbh.accounts_mgmt.accounts.domain;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import java.util.UUID;

public class AccountId {

  private final UUID value;

  private AccountId(UUID value) {
    this.value = requireNonNull(value, "AccountId cannot be null");
    validateFormat(value);
  }

  public static AccountId of(UUID value) {
    return new AccountId(value);
  }

  public static AccountId generate() {
    return new AccountId(UUID.randomUUID());
  }

  private void validateFormat(UUID value) {

  }

  public UUID getValue() {
    return value;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    AccountId accountId = (AccountId) obj;

    if (this == obj || Objects.equals(this.getValue(), accountId.getValue())) {
      return true;
    }

    return Objects.equals(value, accountId.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

}
