package com.jbh.finance.application.feature.statement.commands;

import com.jbh.finance.domain.product.vo.ProductId;
import java.util.UUID;

/**
 * Asks for the state of a statement import.
 *
 * @param userId the user who owns the product
 * @param productId the product of the import
 * @param importId the import id
 */
public record FindStatementImportCommand(UUID userId, ProductId productId, UUID importId) {}
