package com.jbh.account.domain.validation.account.creation;

import com.jbh.account.domain.entity.AccountMovementDomain;
import com.jbh.account.domain.entity.ProductDomain;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.vo.ProductMetadata;

public class LoanProductCreationValidator extends BaseAccountCreationValidator
    implements AccountCreationValidator {

  @Override
  public void validateMetadata(final ProductMetadata metadata) throws AccountBusinessException {
    // do nothing
  }

  @Override
  public void validateInsufficientNetFlow(
      final ProductDomain account, final AccountMovementDomain movement)
      throws AccountBusinessException {
    // do nothing
  }
}
