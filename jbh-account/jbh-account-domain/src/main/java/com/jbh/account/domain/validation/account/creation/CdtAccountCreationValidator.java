package com.jbh.account.domain.validation.account.creation;

import com.jbh.account.domain.entity.ProductDomain;
import com.jbh.account.domain.entity.ProductMovementDomain;
import com.jbh.account.domain.exceptions.ProductBusinessException;
import com.jbh.account.domain.vo.ProductMetadata;
import com.jbh.account.domain.vo.ProductMetadataKey;

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
  public void validateMetadata(final ProductMetadata metadata) throws ProductBusinessException {
    // No specific metadata required for CDT accounts (yet)
    // Future validations can be added here:
    // - Maturity date
    // - Interest rate
    // - Minimum deposit amount
    // - Financial institution
    validateMaturityDate(metadata);
  }

  private void validateMaturityDate(final ProductMetadata metadata)
      throws ProductBusinessException {
    if (metadata.hasKey(ProductMetadataKey.MATURITY_DATE)) {
      metadata.getCdt().getMaturityDate();
    }
  }

  @Override
  public void validateInsufficientNetFlow(
      final ProductDomain productDomain, final ProductMovementDomain movement)
      throws ProductBusinessException {
    // Do nothing
  }
}
