package com.jbh.account.application.core.validation.product_type;

import com.jbh.account.application.core.dto.MovementDTO;
import com.jbh.account.application.core.dto.ProductDTO;
import com.jbh.commons.exception.BusinessException;

public class UndefinedProductMovementValidator implements ProductMovementValidator {

  @Override
  public void validateMovementByProductType(
      final ProductDTO existingProduct, final MovementDTO movementDTO) throws BusinessException {
    // Do nothing
  }
}
