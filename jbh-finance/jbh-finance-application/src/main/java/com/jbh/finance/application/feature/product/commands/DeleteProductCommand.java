package com.jbh.finance.application.feature.product.commands;

import com.jbh.finance.application.shared.validation.CommandValidator;

import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.finance.domain.product.vo.ProductId;
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
