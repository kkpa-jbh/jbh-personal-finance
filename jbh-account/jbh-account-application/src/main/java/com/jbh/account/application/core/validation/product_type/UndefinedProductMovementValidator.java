package com.jbh.account.application.core.validation.product_type;

import com.jbh.account.application.core.dto.AccountDTO;
import com.jbh.account.application.core.dto.MovementDTO;
import com.jbh.account.domain.exceptions.AccountBusinessException;

public class UndefinedProductMovementValidator implements ProductMovementValidator {

  @Override
  public void validateMovementByProductType(
      final AccountDTO existingProduct, final MovementDTO movementDTO)
      throws AccountBusinessException {
    // Do nothing
  }
}
