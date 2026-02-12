package com.jbh.finance.application.feature.product.ports.output;

import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.domain.product.vo.ProductId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {

  Optional<ProductDTO> findByUserAndProductId(UUID userId, ProductId accountId);

  Optional<ProductDTO> findByProductId(ProductId accountId);

  List<ProductDTO> findActiveByUserId(UUID userId);

  ProductDTO save(ProductDTO account);

  void deleteById(ProductId productId);
}
