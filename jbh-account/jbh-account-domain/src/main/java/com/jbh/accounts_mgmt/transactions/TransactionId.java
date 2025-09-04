package com.jbh.accounts_mgmt.transactions;

import static java.util.Objects.requireNonNull;

import java.util.UUID;

public record TransactionId(UUID value) {

  public TransactionId(UUID value) {
    this.value = requireNonNull(value, "TransactionId cannot be null");
    validateFormat(value);
  }

  public static TransactionId of(UUID value) {
    return new TransactionId(value);
  }

  public static TransactionId generate() {
    return new TransactionId(UUID.randomUUID());
  }

  private void validateFormat(UUID value) {

  }


}