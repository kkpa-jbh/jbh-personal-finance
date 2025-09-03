package com.jbh.accounts_mgmt.transactions;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import java.util.UUID;

// Domain Hexagon: com.example.domain.model
public class TransactionId {

  private final UUID value;

  private TransactionId(UUID value) {
    this.value = requireNonNull(value, "TransactionId cannot be null");
    validateFormat(value);
  }

  public static TransactionId from(UUID value) {
    return new TransactionId(value);
  }

  public static TransactionId generate() {
    return new TransactionId(UUID.randomUUID());
  }

  private void validateFormat(UUID value) {

  }

  public UUID getValue() {
    return value;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    TransactionId that = (TransactionId) obj;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

}