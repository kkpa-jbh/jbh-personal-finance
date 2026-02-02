package com.jbh.products.application.core.ports.input;

import com.jbh.products.application.core.dto.ProductDTO;
import com.jbh.products.application.core.services.account.ProductsService;
import com.jbh.products.application.core.usecases.FindProductsUseCase;
import com.jbh.products.application.core.vo.commands.FindProductCommand;
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
