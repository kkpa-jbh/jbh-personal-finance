package com.jbh.products.application.core.vo.commands;

import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.products.domain.vo.ProductId;
import java.util.UUID;

public record DeleteProductCommand(UUID userId, ProductId productId) implements CommandValidator {

  public DeleteProductCommand {
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
