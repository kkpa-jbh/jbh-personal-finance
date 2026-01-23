package com.jbh.account.application.core.ports.output;

import com.jbh.account.application.core.dto.ProductDTO;
import com.jbh.account.domain.vo.ProductId;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {

  Optional<ProductDTO> findByUserAndAccountId(UUID userId, ProductId accountId);

  Optional<ProductDTO> findByAccountId(ProductId accountId);

  ProductDTO save(ProductDTO account);
}
