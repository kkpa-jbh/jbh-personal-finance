package com.jbh.account.application.core.ports.output;

import com.jbh.account.application.core.dto.ProductDTO;
import com.jbh.account.domain.vo.AccountId;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {

  Optional<ProductDTO> findByUserAndAccountId(UUID userId, AccountId accountId);

  Optional<ProductDTO> findByAccountId(AccountId accountId);

  ProductDTO save(ProductDTO account);
}
