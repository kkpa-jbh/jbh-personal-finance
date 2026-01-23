package com.jbh.account.domain.validation.account.creation;

import com.jbh.account.domain.entity.ProductDomain;
import com.jbh.account.domain.entity.ProductMovementDomain;
import com.jbh.account.domain.exceptions.BusinessDomainExceptionType;
import com.jbh.account.domain.exceptions.ProductBusinessException;
import com.jbh.account.domain.vo.ProductMetadata;
import com.jbh.account.domain.vo.ProductMetadataKey;

public class LoanProductCreationValidator extends BaseAccountCreationValidator
    implements ProductCreationValidator {

  @Override
  public void validateMetadata(final ProductMetadata metadata) throws ProductBusinessException {
    if (!metadata.hasKey(ProductMetadataKey.LOAN_PRINCIPAL_AMOUNT)) {
      throw new ProductBusinessException(BusinessDomainExceptionType.EMPTY_LOAN_PRINCIPAL_AMOUNT);
    }

    if (!metadata.hasKey(ProductMetadataKey.LOAN_PAYOFF_AMOUNT_TODAY)) {
      throw new ProductBusinessException(BusinessDomainExceptionType.EMPTY_LOAN_PAYOFF_AMOUNT);
    }
  }

  @Override
  public void validateInsufficientNetFlow(
      final ProductDomain productDomain, final ProductMovementDomain movement)
      throws ProductBusinessException {

    // Custom Validation for Movements
  }
}
