package com.jbh.account.domain.validation.account.creation;

import com.jbh.account.domain.entity.AccountMovementDomain;
import com.jbh.account.domain.entity.ProductDomain;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.exceptions.BusinessDomainExceptionType;
import com.jbh.account.domain.vo.ProductMetadata;
import com.jbh.account.domain.vo.ProductMetadataKey;

public class LoanProductCreationValidator extends BaseAccountCreationValidator
    implements AccountCreationValidator {

  @Override
  public void validateMetadata(final ProductMetadata metadata) throws AccountBusinessException {
    if (!metadata.hasKey(ProductMetadataKey.LOAN_PRINCIPAL_AMOUNT)) {
      throw new AccountBusinessException(BusinessDomainExceptionType.EMPTY_LOAN_PRINCIPAL_AMOUNT);
    }

    if (!metadata.hasKey(ProductMetadataKey.LOAN_TOTAL_AMOUNT_PAID)) {
      throw new AccountBusinessException(BusinessDomainExceptionType.EMPTY_LOAN_TOTAL_AMOUNT_PAID);
    }

    if (!metadata.hasKey(ProductMetadataKey.LOAN_PAYOFF_AMOUNT_TODAY)) {
      throw new AccountBusinessException(BusinessDomainExceptionType.EMPTY_LOAN_PAYOFF_AMOUNT);
    }
  }

  @Override
  public void validateInsufficientNetFlow(
      final ProductDomain account, final AccountMovementDomain movement)
      throws AccountBusinessException {
    // do nothing
  }
}
