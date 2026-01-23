package com.jbh.account.application.core.validation.product_type;

import com.jbh.account.application.core.dto.MovementDTO;
import com.jbh.account.application.core.dto.ProductDTO;
import com.jbh.account.domain.exceptions.ProductBusinessException;

public interface ProductMovementValidator {

  void validateMovementByProductType(ProductDTO existingProduct, MovementDTO movementDTO)
      throws ProductBusinessException;
}
