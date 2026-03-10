package com.jbh.finance.application.feature.product.commands;

import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.finance.application.shared.validation.CommandValidator;
import com.jbh.finance.domain.product.vo.ProductId;
import com.jbh.finance.domain.product.vo.ProductMetadata;
import java.util.UUID;

public record EditProductCommand(
    UUID userId, ProductId productId, String name, ProductMetadata metadata)
    implements CommandValidator {

  public EditProductCommand {
    if (userId == null) {
      throw new GenericSpecificationException("User ID cannot be null");
    }

    if (productId == null) {
      throw new GenericSpecificationException("Product ID cannot be null");
    }

    final boolean hasName = name != null && !name.isBlank();
    if (!hasName) {
      throw new GenericSpecificationException("At least one of name must be provided for edit");
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
    if (!hasName) {
      throw new GenericSpecificationException("At least one of name  must be provided for edit");
    }
  }
}
