package com.jbh.products.domain.product.validation;

import com.jbh.commons.exception.BusinessException;
import com.jbh.products.domain.movement.MovementDomain;
import com.jbh.products.domain.product.ProductDomain;
import com.jbh.products.domain.shared.exceptions.BusinessDomainExceptionType;
import java.math.BigDecimal;

@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod")
public abstract class BaseAccountCreationValidator {

  protected void defaultValidationInsufficientNetFlow(
      final ProductDomain account, final MovementDomain movement) throws BusinessException {

    final BigDecimal currentBalance = account.getCurrentBalance();
    final BigDecimal mvmtAmount = movement.getMovementAmount();
    final boolean isNegativeAmount = mvmtAmount != null && mvmtAmount.signum() < 0;
    if (isNegativeAmount) {
      final BigDecimal possibleCurrentBalance = currentBalance.add(mvmtAmount);
      final boolean isNegativeCurrentBalance = possibleCurrentBalance.signum() < 0;
      if (isNegativeCurrentBalance) {
        throw new BusinessException(BusinessDomainExceptionType.INSUFFICIENT_FUNDS);
      }
    }
  }
}
