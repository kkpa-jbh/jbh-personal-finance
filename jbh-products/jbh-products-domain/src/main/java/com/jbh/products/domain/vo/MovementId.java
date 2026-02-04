package com.jbh.products.domain.vo;

import static java.util.Objects.requireNonNull;

import java.util.UUID;

public record MovementId(UUID value) {

  public MovementId(final UUID value) {
    this.value = requireNonNull(value, "MovementID cannot be null");
  }

  public static MovementId of(final UUID value) {
    return new MovementId(value);
  }

  public static MovementId generate() {
    return new MovementId(UUID.randomUUID());
  }
}
