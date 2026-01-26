package com.jbh.account.domain.validation.account.creation;

import com.jbh.account.domain.entity.ProductDomain;
import com.jbh.account.domain.entity.ProductMovementDomain;
import com.jbh.account.domain.vo.ProductMetadata;
import com.jbh.account.domain.vo.ProductMetadataKey;
import com.jbh.commons.exception.BusinessException;

/**
 * Validator for CDT (Certificate of Deposit / Certificado de Depósito a Término) account type
 * creation.
 *
 * <p>CDT accounts currently do not require specific metadata. This validator exists to follow the
 * Strategy Pattern and can be extended in the future if CDT-specific validations are needed (e.g.,
 * maturity date, interest rate, minimum deposit, etc.).
 */
public class CdtAccountCreationValidator extends BaseAccountCreationValidator
    implements ProductCreationValidator {

  @Override
  public void validateMetadata(final ProductMetadata metadata) throws BusinessException {
    // No specific metadata required for CDT accounts (yet)
    // Future validations can be added here:
    // - Maturity date
    // - Interest rate
    // - Minimum deposit amount
    // - Financial institution
    validateMaturityDate(metadata);
  }

  private void validateMaturityDate(final ProductMetadata metadata) throws BusinessException {
    if (metadata.hasKey(ProductMetadataKey.MATURITY_DATE)) {
      metadata.findCDTMetadata().getMaturityDate();
    }
  }

  @Override
  public void validateInsufficientNetFlow(
      final ProductDomain productDomain, final ProductMovementDomain movement)
      throws BusinessException {
    // Do nothing
  }
}
