package com.jbh.finance.application.feature.statement.dto;

import com.jbh.finance.application.feature.statement.vo.PdfSha256;
import com.jbh.finance.domain.product.vo.ProductId;
import java.util.UUID;

/**
 * A statement import that is about to start.
 *
 * @param id the import id
 * @param userId the user who owns the product
 * @param productId the product that receives the movements
 * @param pdfSha256 the digest of the uploaded PDF
 */
public record NewStatementImportDTO(
    UUID id, UUID userId, ProductId productId, PdfSha256 pdfSha256) {}
