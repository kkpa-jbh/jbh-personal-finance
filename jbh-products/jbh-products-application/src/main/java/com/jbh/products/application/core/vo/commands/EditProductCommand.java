package com.jbh.products.application.core.vo.commands;

import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.products.domain.vo.ProductId;
import com.jbh.products.domain.vo.ProductMetadata;
import java.util.UUID;

public record EditProductCommand(
    UUID userId,
    ProductId productId,
    String name,
    ProductMetadata metadata)
    implements CommandValidator {

  public EditProductCommand {
    if (userId == null) {
      throw new GenericSpecificationException("User ID cannot be null");
    }

    if (productId == null) {
      throw new GenericSpecificationException("Product ID cannot be null");
    }

    final boolean hasName = name != null && !name.isBlank();
    final boolean hasMetadata = metadata != null && !metadata.isEmpty();
    if (!hasName && !hasMetadata) {
      throw new GenericSpecificationException(
          "At least one of name or metadata must be provided for edit");
    }
  }

  @Override
  public void validate() {
    if (userId == null) {
      throw new GenericSpecificationException("User ID cannot be null");
    }

    if (productId == null) {
      throw new GenericSpecificationException("Product ID cannot be null");
    }

    final boolean hasName = name != null && !name.isBlank();
    final boolean hasMetadata = metadata != null && !metadata.isEmpty();
    if (!hasName && !hasMetadata) {
      throw new GenericSpecificationException(
          "At least one of name or metadata must be provided for edit");
    }
  }
}
