package com.jbh.products.application.feature.product.usecases;

import com.jbh.products.application.feature.product.dto.ProductDTO;
import com.jbh.products.application.feature.product.commands.FindProductCommand;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Retrieves products from the user's portfolio with various filtering options.
 *
 * <p><strong>User Explanation:</strong> "View your financial products. You can see all your active
 * accounts or search for a specific product by its ID."
 *
 * <p><strong>Business Rules:</strong>
 *
 * <ul>
 *   <li>Active products query excludes deleted and inactive products
 *   <li>Product lookup by ID returns the product regardless of status
 *   <li>Read-only operations with no database modifications
 * </ul>
 */
public interface FindProductsUseCase {

  /**
   * Finds all active products for the specified user.
   *
   * <p><strong>Validations:</strong>
   *
   * <ul>
   *   <li>User ID cannot be null
   * </ul>
   *
   * <p><strong>Database Operations:</strong>
   *
   * <ul>
   *   <li>SELECT: Active products for user (where active=true and deleted_at is null)
   * </ul>
   *
   * @param userId the user ID to find products for
   * @return list of active product DTOs (may be empty)
   */
  List<ProductDTO> findActiveByUserId(UUID userId);

  /**
   * Finds a product by its ID regardless of status.
   *
   * <p><strong>Validations:</strong>
   *
   * <ul>
   *   <li>None (query returns empty if not found)
   * </ul>
   *
   * <p><strong>Database Operations:</strong>
   *
   * <ul>
   *   <li>SELECT: Product by ID (regardless of active/deleted status)
   * </ul>
   *
   * @param findProductCommand contains the product ID to search for
   * @return Optional containing the product DTO if found, empty otherwise
   */
  Optional<ProductDTO> findProductById(FindProductCommand findProductCommand);
}
