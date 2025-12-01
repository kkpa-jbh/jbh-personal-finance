package com.jbh.account.domain.validation.account.creation;

import com.jbh.account.domain.entity.AccountMovementDomain;
import com.jbh.account.domain.entity.ProductDomain;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.vo.ProductMetadata;

/**
 * Strategy interface for validating account creation based on account type.
 *
 * <p>Each account type (CREDIT_CARD, SAVINGS, INVESTMENT, CDT) has specific metadata requirements
 * that must be validated before creating an account.
 *
 * <p>Implementations should throw {@link AccountBusinessException} with appropriate {@link
 * com.jbh.account.domain.exceptions.BusinessDomainExceptionType} when validation fails.
 */
public interface ProductCreationValidator {

  /**
   * Validates account metadata for creation based on account type requirements.
   *
   * @param metadata The ProductMetadata containing account-specific fields
   * @throws AccountBusinessException if validation fails with specific error type
   */
  void validateMetadata(ProductMetadata metadata) throws AccountBusinessException;

  /**
   * Validates that the productDomain movement is valid for the productDomain type. @Param
   * productDomain The productDomain to validate
   *
   * @param movement The movement to validate
   * @throws AccountBusinessException if validation fails with specific error type
   */
  void validateInsufficientNetFlow(ProductDomain productDomain, AccountMovementDomain movement)
      throws AccountBusinessException;
}
