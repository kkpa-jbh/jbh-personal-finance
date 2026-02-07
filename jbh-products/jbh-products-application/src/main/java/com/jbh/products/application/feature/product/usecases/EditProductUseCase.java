package com.jbh.products.application.feature.product.usecases;

import com.jbh.commons.exception.BusinessException;
import com.jbh.products.application.feature.product.dto.ProductDTO;
import com.jbh.products.application.feature.product.commands.EditProductCommand;

/**
 * Edits basic product information such as name and metadata for an active product.
 *
 * <p><strong>User Explanation:</strong> "Update your product's name or configuration details.
 * This allows you to keep your product information current as your financial situation changes."
 *
 * <p><strong>Business Rules:</strong>
 *
 * <ul>
 *   <li>Only active products can be edited
 *   <li>Name and metadata updates are optional (only provided fields are updated)
 *   <li>Empty or blank values are ignored
 * </ul>
 */
public interface EditProductUseCase {

  /**
   * Edits the product name and/or metadata for an active product.
   *
   * <p><strong>Validations:</strong>
   *
   * <ul>
   *   <li>Command cannot be null
   *   <li>Command fields validation (via command.validate())
   *   <li>Product must exist for the user
   *   <li>Product must be active (not inactive or deleted)
   * </ul>
   *
   * <p><strong>Database Operations:</strong>
   *
   * <ul>
   *   <li>UPDATE: Product name (if provided and not blank)
   *   <li>UPDATE: Product metadata (if provided and not empty)
   * </ul>
   *
   * @param command contains userId, productId, optional new name, and optional new metadata
   * @return updated product DTO
   * @throws BusinessException if product not found, product is inactive, or validation fails
   */
  ProductDTO execute(EditProductCommand command) throws BusinessException;
}
