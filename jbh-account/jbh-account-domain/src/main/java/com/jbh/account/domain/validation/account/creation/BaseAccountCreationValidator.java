package com.jbh.account.domain.validation.account.creation;

import com.jbh.account.domain.entity.AccountMovementDomain;
import com.jbh.account.domain.entity.ProductDomain;
import com.jbh.account.domain.exceptions.ProductBusinessException;
import com.jbh.account.domain.exceptions.BusinessDomainExceptionType;
import java.math.BigDecimal;

@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod")
public abstract class BaseAccountCreationValidator {

  protected void defaultValidationInsufficientNetFlow(
      final ProductDomain account, final AccountMovementDomain movement)
      throws ProductBusinessException {

    final BigDecimal currentBalance = account.getCurrentBalance();
    final BigDecimal mvmtAmount = movement.getMovementAmount();
    final boolean isNegativeAmount = mvmtAmount != null && mvmtAmount.signum() < 0;
    if (isNegativeAmount) {
      final BigDecimal possibleCurrentBalance = currentBalance.add(mvmtAmount);
      final boolean isNegativeCurrentBalance = possibleCurrentBalance.signum() < 0;
      if (isNegativeCurrentBalance) {
        throw new ProductBusinessException(BusinessDomainExceptionType.INSUFFICIENT_FUNDS);
      }
    }
  }
}
