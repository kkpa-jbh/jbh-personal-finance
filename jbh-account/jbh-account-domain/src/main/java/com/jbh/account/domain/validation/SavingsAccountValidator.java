package com.jbh.account.domain.validation;

import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.vo.AccountType;
import java.util.Map;

/**
 * Validator for SAVINGS account type creation.
 *
 * <p>Savings accounts currently do not require specific metadata. This validator exists to follow
 * the Strategy Pattern and can be extended in the future if savings-specific validations are
 * needed (e.g., minimum balance, interest rate, etc.).
 */
public class SavingsAccountValidator implements AccountCreationValidator {

  @Override
  public void validate(final Map<String, Object> metadata) throws AccountBusinessException {
    // No specific metadata required for savings accounts (yet)
    // Future validations can be added here:
    // - Minimum balance
    // - Interest rate
    // - Account features
  }

  @Override
  public AccountType supportedAccountType() {
    return AccountType.SAVINGS;
  }
}
