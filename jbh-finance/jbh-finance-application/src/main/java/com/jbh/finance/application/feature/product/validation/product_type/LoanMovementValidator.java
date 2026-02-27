package com.jbh.finance.application.feature.product.validation.product_type;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.application.shared.exceptions.BusinessApplicationExceptionType;

public class LoanMovementValidator implements ProductMovementValidator {

  @Override
  public void validateMovementByProductType(
      final ProductDTO existingProduct, final MovementDTO movementDTO) throws BusinessException {
    final var category = movementDTO.category();

    if (category.isNotIncomeTransfer()) {
      throw new BusinessException(BusinessApplicationExceptionType.INVALID_CATEGORY_LOAN_MOVEMENT);
    }
  }
}
