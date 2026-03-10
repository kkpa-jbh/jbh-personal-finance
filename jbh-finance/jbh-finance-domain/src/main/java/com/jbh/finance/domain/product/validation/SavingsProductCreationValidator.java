package com.jbh.finance.domain.product.validation;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.domain.movement.MovementDomain;
import com.jbh.finance.domain.product.ProductDomain;
import com.jbh.finance.domain.product.vo.ProductMetadata;
import com.jbh.finance.domain.shared.exceptions.BusinessDomainExceptionType;

/**
 * Validator for SAVINGS product type creation.
 *
 * <p>Savings products currently do not require specific metadata. This validator exists to follow
 * the Strategy Pattern and can be extended in the future if savings-specific validations are needed
 * (e.g., minimum balance, interest rate, etc.).
 */
public class SavingsProductCreationValidator extends BaseProductCreationValidator
    implements ProductCreationValidator {

  @Override
  public void validateMetadata(final ProductMetadata metadata) throws BusinessException {
    if (metadata != null && !metadata.isEmpty()) {
      throw new BusinessException(BusinessDomainExceptionType.INVALID_METADATA);
    }
  }

  @Override
  public void validateInsufficientNetFlow(
      final ProductDomain productDomain, final MovementDomain movement) throws BusinessException {
    defaultValidationInsufficientNetFlow(productDomain, movement);
  }
}
