package com.jbh.products.application.feature.product.ports.input;

import com.jbh.products.application.feature.product.dto.ProductDTO;
import com.jbh.products.application.feature.product.services.ProductsService;
import com.jbh.products.application.feature.product.usecases.FindProductsUseCase;
import com.jbh.products.application.feature.product.commands.FindProductCommand;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class FindActiveProductsInputPort implements FindProductsUseCase {

  private final ProductsService accountService;

  public FindActiveProductsInputPort(final ProductsService accountService) {
    this.accountService = accountService;
  }

  @Override
  public List<ProductDTO> findActiveByUserId(final UUID userId) {
    if (userId == null) {
      throw new IllegalArgumentException("User ID cannot be null");
    }
    return accountService.findActiveByUserId(userId);
  }

  @Override
  public Optional<ProductDTO> findProductById(final FindProductCommand findProductCommand) {
    return accountService.findProductById(findProductCommand.productId());
  }
}
