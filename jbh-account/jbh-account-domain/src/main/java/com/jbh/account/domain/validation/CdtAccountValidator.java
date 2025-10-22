package com.jbh.account.domain.validation;

import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.vo.AccountType;
import java.util.Map;

/**
 * Validator for CDT (Certificate of Deposit / Certificado de Depósito a Término) account type
 * creation.
 *
 * <p>CDT accounts currently do not require specific metadata. This validator exists to follow the
 * Strategy Pattern and can be extended in the future if CDT-specific validations are needed (e.g.,
 * maturity date, interest rate, minimum deposit, etc.).
 */
public class CdtAccountValidator implements AccountCreationValidator {

  @Override
  public void validate(final Map<String, Object> metadata) throws AccountBusinessException {
    // No specific metadata required for CDT accounts (yet)
    // Future validations can be added here:
    // - Maturity date
    // - Interest rate
    // - Minimum deposit amount
    // - Financial institution
  }

  @Override
  public AccountType supportedAccountType() {
    return AccountType.CDT;
  }
}
