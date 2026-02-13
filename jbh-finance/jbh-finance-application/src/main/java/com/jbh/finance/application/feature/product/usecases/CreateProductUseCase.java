package com.jbh.finance.application.feature.product.usecases;

import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.application.feature.product.commands.CreateProductCommand;
import com.jbh.commons.exception.BusinessException;

/**
 * Creates a new financial product (productDTO) in the user's portfolio with type-specific metadata
 * validation.
 *
 * <p><strong>User Explanation:</strong> "Create a product to add to your portfolio and track its
 * movements, balances, and performance over time."
 *
 * <p><strong>Business Rules:</strong>
 *
 * <ul>
 *   <li>Each product must have a unique name per user
 *   <li>Product type determines which metadata fields are required
 *   <li>Domain validates type-specific metadata requirements using Strategy Pattern
 * </ul>
 */
public interface CreateProductUseCase {

  /**
   * Creates and persists a new product with validated type-specific metadata.
   *
   * <p><strong>Validations:</strong>
   *
   * <ul>
   *   <li>Command cannot be null
   *   <li>Command fields validation (via command.validate())
   *   <li>Type-specific metadata validation (via ProductDomain)
   * </ul>
   *
   * <p><strong>Database Operations:</strong>
   *
   * <ul>
   *   <li>INSERT: New product record in products table
   *   <li>INSERT: Product metadata (JSONB column)
   * </ul>
   *
   * @param command contains product details (name, type, userId, metadata)
   * @return created product DTO with generated ID
   * @throws BusinessException if validation fails or product name already exists for user
   */
  ProductDTO execute(CreateProductCommand command) throws BusinessException;
}
