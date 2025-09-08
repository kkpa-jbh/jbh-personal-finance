package com.jbh.accounts_mgmt.movements;

import static java.util.Objects.requireNonNull;

import java.util.UUID;

public record AccountMovementId(UUID value) {

  public AccountMovementId(UUID value) {
    this.value = requireNonNull(value, "MovementID cannot be null");
    validateFormat(value);
  }

  public static AccountMovementId of(UUID value) {
    return new AccountMovementId(value);
  }

  public static AccountMovementId generate() {
    return new AccountMovementId(UUID.randomUUID());
  }

  private static void validateFormat(UUID value) {
    // No specific validation rules needed for UUID format
    // UUID class itself ensures valid format
    // Parameter used for potential future validation logic
  }


}