package com.jbh.products.application.core.usecases;

import com.jbh.products.application.core.vo.commands.UpdateMetadataProductCommand;
import com.jbh.products.domain.product.vo.ProductPK;
import com.jbh.commons.exception.BusinessException;

/**
 * Updates the complete metadata configuration of an existing financial product.
 *
 * <p><strong>User Explanation:</strong> "Update the configuration and details of your product,
 * such as interest rates, credit limits, or investment-specific information."
 *
 * <p><strong>Business Rules:</strong>
 *
 * <ul>
 *   <li>Replaces all existing metadata with the new metadata provided
 *   <li>Product type determines which metadata fields are valid
 *   <li>Domain validates metadata requirements during update
 * </ul>
 */
public interface UpdateProductUseCase {

  /**
   * Replaces all metadata for the specified product with new metadata.
   *
   * <p><strong>Validations:</strong>
   *
   * <ul>
   *   <li>Product must exist for the user
   *   <li>Metadata must be valid for the product type
   *   <li>Domain validates type-specific metadata requirements
   * </ul>
   *
   * <p><strong>Database Operations:</strong>
   *
   * <ul>
   *   <li>UPDATE: Product metadata (JSONB column)
   * </ul>
   *
   * @param accountPK the product primary key (userId and productId)
   * @param command contains the new metadata to replace existing metadata
   * @throws BusinessException if product not found or metadata validation fails
   */
  void replaceMetadata(ProductPK accountPK, UpdateMetadataProductCommand command)
      throws BusinessException;
}
