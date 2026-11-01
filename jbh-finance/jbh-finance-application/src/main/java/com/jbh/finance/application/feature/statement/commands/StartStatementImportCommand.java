package com.jbh.finance.application.feature.statement.commands;

import com.jbh.finance.application.feature.statement.vo.PdfSha256;
import com.jbh.finance.domain.product.vo.ProductId;
import java.util.UUID;

/**
 * Asks to admit a statement import.
 *
 * @param userId the user who owns the product
 * @param productId the product that receives the movements
 * @param pdfSha256 the digest of the uploaded PDF
 */
public record StartStatementImportCommand(UUID userId, ProductId productId, PdfSha256 pdfSha256) {}
