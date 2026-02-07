package com.jbh.products.application.feature.product.validation.product_type;

import com.jbh.products.application.feature.movement.dto.MovementDTO;
import com.jbh.products.application.feature.product.dto.ProductDTO;
import com.jbh.commons.exception.BusinessException;

public interface ProductMovementValidator {

  void validateMovementByProductType(ProductDTO existingProduct, MovementDTO movementDTO)
      throws BusinessException;
}
