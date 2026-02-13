package com.jbh.finance.domain.product.validation;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.domain.movement.MovementDomain;
import com.jbh.finance.domain.product.ProductDomain;
import com.jbh.finance.domain.product.vo.ProductMetadata;

/**
 * Strategy interface for validating product creation based on product type.
 *
 * <p>Each product type (CREDIT_CARD, SAVINGS, INVESTMENT, CDT) has specific metadata requirements
 * that must be validated before creating an product.
 *
 * <p>Implementations should throw {@link BusinessException} with appropriate {@link
 * com.jbh.finance.domain.exceptions.BusinessDomainExceptionType} when validation fails.
 */
public interface ProductCreationValidator {

  /**
   * Validates product metadata for creation based on product type requirements.
   *
   * @param metadata The ProductMetadata containing product-specific fields
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
  void validateInsufficientNetFlow(ProductDomain productDomain, MovementDomain movement)
      throws BusinessException;
}
