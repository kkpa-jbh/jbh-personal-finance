package com.jbh.account.domain.validation.account.creation;

import com.jbh.account.domain.entity.AccountDomain;
import com.jbh.account.domain.entity.AccountMovementDomain;
import com.jbh.account.domain.exceptions.AccountBusinessException;
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
  void validateMetadata(Map<String, Object> metadata) throws AccountBusinessException;

  /**
   * Validates that the account movement is valid for the account type. @Param account The account
   * to validate
   *
   * @param movement The movement to validate
   * @throws AccountBusinessException if validation fails with specific error type
   */
  void validateInsufficientNetFlow(AccountDomain account, AccountMovementDomain movement)
      throws AccountBusinessException;
}
