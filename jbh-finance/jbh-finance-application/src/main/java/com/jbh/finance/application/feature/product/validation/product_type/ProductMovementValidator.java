package com.jbh.finance.application.feature.product.validation.product_type;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.application.feature.product.dto.ProductDTO;

public interface ProductMovementValidator {

  void validateMovementByProductType(ProductDTO existingProduct, MovementDTO movementDTO)
      throws BusinessException;
}
