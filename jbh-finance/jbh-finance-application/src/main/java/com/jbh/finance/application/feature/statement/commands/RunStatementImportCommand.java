package com.jbh.finance.application.feature.statement.commands;

import com.jbh.finance.domain.product.vo.ProductId;
import java.util.UUID;

/**
 * Asks to run an admitted statement import.
 *
 * @param importId the import id returned by the start use case
 * @param userId the user who owns the product
 * @param productId the product that receives the movements
 * @param extractCommand the unlocked PDF to read
 */
public record RunStatementImportCommand(
    UUID importId,
    UUID userId,
    ProductId productId,
    ExtractStatementMovementsCommand extractCommand) {}
