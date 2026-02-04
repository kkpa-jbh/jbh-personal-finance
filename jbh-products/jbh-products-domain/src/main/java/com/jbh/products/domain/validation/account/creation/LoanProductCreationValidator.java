package com.jbh.products.domain.validation.account.creation;

import com.jbh.commons.exception.BusinessException;
import com.jbh.products.domain.entity.MovementDomain;
import com.jbh.products.domain.entity.ProductDomain;
import com.jbh.products.domain.exceptions.BusinessDomainExceptionType;
import com.jbh.products.domain.vo.ProductMetadata;
import com.jbh.products.domain.vo.ProductMetadataKey;

public class LoanProductCreationValidator extends BaseAccountCreationValidator
    implements ProductCreationValidator {

  @Override
  public void validateMetadata(final ProductMetadata metadata) throws BusinessException {
    if (!metadata.hasKey(ProductMetadataKey.LOAN_PRINCIPAL_AMOUNT)) {
      throw new BusinessException(BusinessDomainExceptionType.EMPTY_LOAN_PRINCIPAL_AMOUNT);
    }

    if (!metadata.hasKey(ProductMetadataKey.LOAN_PAYOFF_AMOUNT_TODAY)) {
      throw new BusinessException(BusinessDomainExceptionType.EMPTY_LOAN_PAYOFF_AMOUNT);
    }
  }

  @Override
  public void validateInsufficientNetFlow(
      final ProductDomain productDomain, final MovementDomain movement) throws BusinessException {

    // Custom Validation for Movements
  }
}
