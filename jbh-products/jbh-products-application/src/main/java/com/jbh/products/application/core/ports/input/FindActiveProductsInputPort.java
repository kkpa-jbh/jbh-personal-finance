package com.jbh.products.application.core.ports.input;

import com.jbh.products.application.core.dto.ProductDTO;
import com.jbh.products.application.core.ports.output.AccountRepository;
import com.jbh.products.application.core.usecases.FindActiveProductsUseCase;
import java.util.List;
import java.util.UUID;

public class FindActiveProductsInputPort implements FindActiveProductsUseCase {

  private final AccountRepository accountRepository;

  public FindActiveProductsInputPort(final AccountRepository accountRepository) {
    this.accountRepository = accountRepository;
  }

  @Override
  public List<ProductDTO> findActiveByUserId(final UUID userId) {
    if (userId == null) {
      throw new IllegalArgumentException("User ID cannot be null");
    }
    return accountRepository.findActiveByUserId(userId);
  }
}
