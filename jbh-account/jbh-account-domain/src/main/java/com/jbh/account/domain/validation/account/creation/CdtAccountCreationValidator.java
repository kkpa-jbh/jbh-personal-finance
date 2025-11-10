package com.jbh.account.domain.validation.account.creation;

import com.jbh.account.domain.entity.AccountMovementDomain;
import com.jbh.account.domain.entity.ProductDomain;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.exceptions.BusinessDomainExceptionType;
import com.jbh.account.domain.vo.ProductMetadata;
import com.jbh.account.domain.vo.ProductMetadataKey;
import java.time.LocalDate;

/**
 * Validator for CDT (Certificate of Deposit / Certificado de Depósito a Término) account type
 * creation.
 *
 * <p>CDT accounts currently do not require specific metadata. This validator exists to follow the
 * Strategy Pattern and can be extended in the future if CDT-specific validations are needed (e.g.,
 * maturity date, interest rate, minimum deposit, etc.).
 */
public class CdtAccountCreationValidator extends BaseAccountCreationValidator
    implements AccountCreationValidator {

  @Override
  public void validateMetadata(final ProductMetadata metadata) throws AccountBusinessException {
    // No specific metadata required for CDT accounts (yet)
    // Future validations can be added here:
    // - Maturity date
    // - Interest rate
    // - Minimum deposit amount
    // - Financial institution
    validateMaturityDate(metadata);
  }

  private void validateMaturityDate(final ProductMetadata metadata)
      throws AccountBusinessException {
    if (metadata.hasKey(ProductMetadataKey.MATURITY_DATE)) {
      final Object maturityDate = metadata.get(ProductMetadataKey.MATURITY_DATE);
      if (!(maturityDate instanceof LocalDate)) {
        throw new AccountBusinessException(BusinessDomainExceptionType.INVALID_MATURITY_DATE_TYPE);
      }
    }
  }

  @Override
  public void validateInsufficientNetFlow(
      final ProductDomain account, final AccountMovementDomain movement)
      throws AccountBusinessException {
    // Do nothing
  }
}
