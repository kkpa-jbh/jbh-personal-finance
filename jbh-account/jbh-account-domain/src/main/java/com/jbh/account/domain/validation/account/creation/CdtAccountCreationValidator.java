package com.jbh.account.domain.validation.account.creation;

import com.jbh.account.domain.entity.AccountDomain;
import com.jbh.account.domain.entity.AccountMovementDomain;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.exceptions.BusinessDomainExceptionType;
import com.jbh.account.domain.vo.AccountMetadataKey;
import java.time.LocalDate;
import java.util.Map;

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
  public void validateMetadata(final Map<String, Object> metadata) throws AccountBusinessException {
    // No specific metadata required for CDT accounts (yet)
    // Future validations can be added here:
    // - Maturity date
    // - Interest rate
    // - Minimum deposit amount
    // - Financial institution
    validateMaturityDate(metadata);
  }

  private void validateMaturityDate(final Map<String, Object> metadata)
      throws AccountBusinessException {
    final String maturityDateKey = AccountMetadataKey.MATURITY_DATE.name();

    if (metadata.containsKey(maturityDateKey)) {
      final Object paymentDueDay = metadata.get(maturityDateKey);
      if (!(paymentDueDay instanceof LocalDate)) {
        throw new AccountBusinessException(BusinessDomainExceptionType.INVALID_MATURITY_DATE_TYPE);
      }
    }
  }

  @Override
  public void validateInsufficientNetFlow(
      final AccountDomain account, final AccountMovementDomain movement)
      throws AccountBusinessException {
    defaultValidationInsufficientNetFlow(account, movement);
  }
}
