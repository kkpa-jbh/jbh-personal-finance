package com.jbh.products.domain.validation.account.creation;

import com.jbh.products.domain.entity.ProductDomain;
import com.jbh.products.domain.entity.MovementDomain;
import com.jbh.products.domain.vo.ProductMetadata;
import com.jbh.commons.exception.BusinessException;

/**
 * Validator for SAVINGS account type creation.
 *
 * <p>Savings accounts currently do not require specific metadata. This validator exists to follow
 * the Strategy Pattern and can be extended in the future if savings-specific validations are needed
 * (e.g., minimum balance, interest rate, etc.).
 */
public class SavingsAccountCreationValidator extends BaseAccountCreationValidator
    implements ProductCreationValidator {

  @Override
  public void validateMetadata(final ProductMetadata metadata) throws BusinessException {
    // No specific metadata required for savings accounts (yet)
    // Future validations can be added here:
    // - Minimum balance
    // - Interest rate
    // - Account features
  }

  @Override
  public void validateInsufficientNetFlow(
      final ProductDomain productDomain, final MovementDomain movement) throws BusinessException {
    defaultValidationInsufficientNetFlow(productDomain, movement);
  }
}
