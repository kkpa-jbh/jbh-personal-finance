package com.jbh.products.application.core.vo.commands;

import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.products.domain.product.vo.ProductId;
import java.util.UUID;

public record FindProductCommand(UUID userId, ProductId productId) {

  public FindProductCommand {
    if (userId == null) {
      throw new GenericSpecificationException("User ID cannot be null");
    }

    if (productId == null) {
      throw new GenericSpecificationException("Product ID cannot be null");
    }
  }
}
