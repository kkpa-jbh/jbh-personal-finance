package com.jbh.products.application.core.ports.output;

import com.jbh.products.application.core.dto.ProductDTO;
import com.jbh.products.domain.vo.ProductId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {

  Optional<ProductDTO> findByUserAndAccountId(UUID userId, ProductId accountId);

  Optional<ProductDTO> findByAccountId(ProductId accountId);

  List<ProductDTO> findActiveByUserId(UUID userId);

  ProductDTO save(ProductDTO account);
}
