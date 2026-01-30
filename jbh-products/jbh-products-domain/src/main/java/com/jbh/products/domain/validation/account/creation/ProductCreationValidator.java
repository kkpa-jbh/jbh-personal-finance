package com.jbh.products.domain.validation.account.creation;

import com.jbh.products.domain.entity.ProductDomain;
import com.jbh.products.domain.entity.ProductMovementDomain;
import com.jbh.products.domain.vo.ProductMetadata;
import com.jbh.commons.exception.BusinessException;

/**
 * Strategy interface for validating account creation based on account type.
 *
 * <p>Each account type (CREDIT_CARD, SAVINGS, INVESTMENT, CDT) has specific metadata requirements
 * that must be validated before creating an account.
 *
 * <p>Implementations should throw {@link BusinessException} with appropriate {@link
 * com.jbh.products.domain.exceptions.BusinessDomainExceptionType} when validation fails.
 */
public interface ProductCreationValidator {

  /**
   * Validates account metadata for creation based on account type requirements.
   *
   * @param metadata The ProductMetadata containing account-specific fields
   * @throws BusinessException if validation fails with specific error type
   */
  void validateMetadata(ProductMetadata metadata) throws BusinessException;

  /**
   * Validates that the productDomain movement is valid for the productDomain type. @Param
   * productDomain The productDomain to validate
   *
   * @param movement The movement to validate
   * @throws BusinessException if validation fails with specific error type
   */
  void validateInsufficientNetFlow(ProductDomain productDomain, ProductMovementDomain movement)
      throws BusinessException;
}
