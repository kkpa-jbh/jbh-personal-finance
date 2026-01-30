package com.jbh.products.domain.vo;

import java.util.Objects;
import java.util.UUID;

public record ProductId(UUID value) {

  public ProductId {
    Objects.requireNonNull(value, "AccountId cannot be null");
  }

  public static ProductId of(final UUID value) {
    return new ProductId(value);
  }

  public static ProductId generate() {
    return new ProductId(UUID.randomUUID());
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
