package com.jbh.account.domain.validation;

import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.vo.AccountType;
import java.util.Map;

/**
 * Strategy interface for validating account creation based on account type.
 *
 * <p>Each account type (CREDIT_CARD, SAVINGS, INVESTMENT, CDT) has specific metadata requirements
 * that must be validated before creating an account.
 *
 * <p>Implementations should throw {@link AccountBusinessException} with appropriate {@link
 * com.jbh.account.domain.exceptions.BusinessDomainExceptionType} when validation fails.
 */
public interface AccountCreationValidator {

  /**
   * Validates account metadata for creation based on account type requirements.
   *
   * @param metadata The metadata map for the account (keys are AccountMetadataKey.name())
   * @throws AccountBusinessException if validation fails with specific error type
   */
  void validate(Map<String, Object> metadata) throws AccountBusinessException;

  /**
   * Returns the account type this validator supports.
   *
   * @return The supported AccountType
   */
  AccountType supportedAccountType();
}
