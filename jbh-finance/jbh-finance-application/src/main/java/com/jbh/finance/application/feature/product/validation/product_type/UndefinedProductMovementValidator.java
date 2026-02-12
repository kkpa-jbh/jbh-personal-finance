package com.jbh.finance.application.feature.product.validation.product_type;

import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.commons.exception.BusinessException;

public class UndefinedProductMovementValidator implements ProductMovementValidator {

  @Override
  public void validateMovementByProductType(
      final ProductDTO existingProduct, final MovementDTO movementDTO) throws BusinessException {
    // Do nothing
  }
}
