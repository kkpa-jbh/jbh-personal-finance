package com.jbh.account.application.core.validation.product_type;

import com.jbh.account.application.core.dto.MovementDTO;
import com.jbh.account.application.core.dto.ProductDTO;
import com.jbh.commons.exception.BusinessException;

public interface ProductMovementValidator {

  void validateMovementByProductType(ProductDTO existingProduct, MovementDTO movementDTO)
      throws BusinessException;
}
