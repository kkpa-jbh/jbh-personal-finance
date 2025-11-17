package com.jbh.account.application.core.validation.product_type;

import com.jbh.account.application.core.dto.MovementDTO;
import com.jbh.account.application.core.dto.ProductDTO;
import com.jbh.account.application.core.exceptions.BusinessApplicationExceptionType;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.vo.IncomeCategory;

public class LoanMovementValidator implements ProductMovementValidator {

  @Override
  public void validateMovementByProductType(
      final ProductDTO existingProduct, final MovementDTO movementDTO)
      throws AccountBusinessException {
    final var categoryType = movementDTO.category().getType();

    if (categoryType != IncomeCategory.TRANSFER) {
      throw new AccountBusinessException(
          BusinessApplicationExceptionType.INVALID_CATEGORY_LOAN_MOVEMENT);
    }
  }
}
