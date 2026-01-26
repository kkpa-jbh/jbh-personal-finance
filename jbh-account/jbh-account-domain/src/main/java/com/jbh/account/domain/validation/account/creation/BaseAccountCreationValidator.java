package com.jbh.account.domain.validation.account.creation;

import com.jbh.account.domain.entity.ProductDomain;
import com.jbh.account.domain.entity.ProductMovementDomain;
import com.jbh.account.domain.exceptions.BusinessDomainExceptionType;
import com.jbh.commons.exception.BusinessException;
import java.math.BigDecimal;

@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod")
public abstract class BaseAccountCreationValidator {

  protected void defaultValidationInsufficientNetFlow(
      final ProductDomain account, final ProductMovementDomain movement) throws BusinessException {

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
