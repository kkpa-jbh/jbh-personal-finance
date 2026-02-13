package com.jbh.finance.domain.product.validation;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.domain.movement.MovementDomain;
import com.jbh.finance.domain.product.ProductDomain;
import com.jbh.finance.domain.shared.exceptions.BusinessDomainExceptionType;
import com.jbh.finance.domain.product.vo.ProductMetadata;
import com.jbh.finance.domain.product.vo.ProductMetadataKey;

public class LoanProductCreationValidator extends BaseProductCreationValidator
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
