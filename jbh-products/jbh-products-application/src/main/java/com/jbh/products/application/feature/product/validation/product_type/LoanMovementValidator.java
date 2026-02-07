package com.jbh.products.application.feature.product.validation.product_type;

import com.jbh.products.application.feature.movement.dto.MovementDTO;
import com.jbh.products.application.feature.product.dto.ProductDTO;
import com.jbh.products.application.shared.exceptions.BusinessApplicationExceptionType;
import com.jbh.products.domain.movement.vo.IncomeCategory;
import com.jbh.commons.exception.BusinessException;

public class LoanMovementValidator implements ProductMovementValidator {

  @Override
  public void validateMovementByProductType(
      final ProductDTO existingProduct, final MovementDTO movementDTO) throws BusinessException {
    final var categoryType = movementDTO.category().getType();

    if (categoryType != IncomeCategory.TRANSFER) {
      throw new BusinessException(BusinessApplicationExceptionType.INVALID_CATEGORY_LOAN_MOVEMENT);
    }
  }
}
