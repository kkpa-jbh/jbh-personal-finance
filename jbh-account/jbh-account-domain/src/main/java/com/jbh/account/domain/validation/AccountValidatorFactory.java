package com.jbh.account.domain.validation;

import com.jbh.account.domain.vo.AccountType;
import java.util.Map;

/**
 * Factory for obtaining the appropriate {@link AccountCreationValidator} for a given {@link
 * AccountType}.
 *
 * <p>This factory implements the Strategy Pattern by providing the correct validator implementation
 * based on the account type, eliminating the need for if/else or switch statements.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * AccountCreationValidator validator =
 *     AccountValidatorFactory.getValidator(AccountType.CREDIT_CARD);
 * validator.validate(metadata);
 * }</pre>
 */
public final class AccountValidatorFactory {

  private static final Map<AccountType, AccountCreationValidator> VALIDATORS;

  static {
    VALIDATORS =
        Map.of(
            AccountType.CREDIT_CARD, new CreditCardAccountValidator(),
            AccountType.SAVINGS, new SavingsAccountValidator(),
            AccountType.INVESTMENT, new InvestmentAccountValidator(),
            AccountType.CDT, new CdtAccountValidator());
  }

  private AccountValidatorFactory() {
    // Utility class - prevent instantiation
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  /**
   * Returns the appropriate validator for the given account type.
   *
   * @param accountType The account type to get validator for
   * @return The validator instance for the account type
   * @throws IllegalArgumentException if no validator is registered for the account type
   */
  public static AccountCreationValidator getValidator(final AccountType accountType) {
    final AccountCreationValidator validator = VALIDATORS.get(accountType);

    if (validator == null) {
      throw new IllegalArgumentException(
          "No validator registered for account type: " + accountType);
    }

    return validator;
  }
}
