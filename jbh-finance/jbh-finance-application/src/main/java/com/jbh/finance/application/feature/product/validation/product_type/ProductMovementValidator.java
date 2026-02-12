package com.jbh.finance.application.feature.product.validation.product_type;

import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.commons.exception.BusinessException;

public interface ProductMovementValidator {

  void validateMovementByProductType(ProductDTO existingProduct, MovementDTO movementDTO)
      throws BusinessException;
}
