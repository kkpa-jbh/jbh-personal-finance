package com.jbh.account.application.core.validation.product_type;

import com.jbh.account.application.core.dto.AccountDTO;
import com.jbh.account.application.core.dto.MovementDTO;
import com.jbh.account.domain.exceptions.AccountBusinessException;

public interface ProductMovementValidator {

  void validateMovementByProductType(AccountDTO existingProduct, MovementDTO movementDTO)
      throws AccountBusinessException;
}
