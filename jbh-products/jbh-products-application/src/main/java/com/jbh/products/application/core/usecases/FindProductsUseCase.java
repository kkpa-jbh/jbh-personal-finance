package com.jbh.products.application.core.usecases;

import com.jbh.products.application.core.dto.ProductDTO;
import com.jbh.products.application.core.vo.commands.FindProductCommand;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FindProductsUseCase {

  List<ProductDTO> findActiveByUserId(UUID userId);

  Optional<ProductDTO> findProductById(FindProductCommand findProductCommand);
}
