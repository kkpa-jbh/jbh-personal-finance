package com.jbh.account.application.core.usecases;

import com.jbh.account.application.core.dto.ProductDTO;
import java.util.List;
import java.util.UUID;

public interface FindActiveProductsUseCase {

  List<ProductDTO> findActiveByUserId(UUID userId);
}
