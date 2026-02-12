package com.jbh.finance.application.feature.product.usecases;

import com.jbh.finance.application.feature.product.dto.MetadataFieldConfigDTO;
import com.jbh.finance.domain.product.vo.ProductType;
import java.util.List;

/**
 * Retrieves the metadata field configuration for a specific product type.
 *
 * <p><strong>User Explanation:</strong> "Get the list of required and optional fields for a
 * product type. This helps the frontend display the correct form fields when creating or editing
 * products."
 *
 * <p><strong>Business Rules:</strong>
 *
 * <ul>
 *   <li>Each product type has a specific set of metadata fields
 *   <li>Configuration defines field name, type, required/optional status, and validation rules
 *   <li>Configuration is retrieved from registry (no database access)
 *   <li>Used by frontend to dynamically render product creation/edit forms
 * </ul>
 */
public interface GetProductMetadataConfigUseCase {

  /**
   * Retrieves metadata field configuration for the specified product type.
   *
   * <p><strong>Validations:</strong>
   *
   * <ul>
   *   <li>ProductType cannot be null
   * </ul>
   *
   * <p><strong>Database Operations:</strong>
   *
   * <ul>
   *   <li>None (reads from in-memory registry)
   * </ul>
   *
   * @param productType the product type to get configuration for
   * @return list of metadata field configuration DTOs
   * @throws IllegalArgumentException if productType is null
   */
  List<MetadataFieldConfigDTO> execute(ProductType productType);
}
