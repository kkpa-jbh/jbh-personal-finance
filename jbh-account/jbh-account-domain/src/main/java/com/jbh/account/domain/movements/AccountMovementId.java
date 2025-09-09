package com.jbh.account.domain.movements;

import static java.util.Objects.requireNonNull;

import java.util.UUID;

public record AccountMovementId(UUID value) {

  public AccountMovementId(final UUID value) {
    this.value = requireNonNull(value, "MovementID cannot be null");
  }

  public static AccountMovementId of(final UUID value) {
    return new AccountMovementId(value);
  }

  public static AccountMovementId generate() {
    return new AccountMovementId(UUID.randomUUID());
  }


}