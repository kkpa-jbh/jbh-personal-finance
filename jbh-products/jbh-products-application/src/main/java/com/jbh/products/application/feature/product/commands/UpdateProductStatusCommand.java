package com.jbh.products.application.feature.product.commands;

import com.jbh.products.application.shared.validation.CommandValidator;

import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.products.domain.product.vo.ProductId;
import java.util.UUID;

public record UpdateProductStatusCommand(UUID userId, ProductId productId, boolean active)
    implements CommandValidator {

  public UpdateProductStatusCommand {
    if (userId == null) {
      throw new GenericSpecificationException("User ID cannot be null");
    }

    if (productId == null) {
      throw new GenericSpecificationException("Product ID cannot be null");
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
  }
}
