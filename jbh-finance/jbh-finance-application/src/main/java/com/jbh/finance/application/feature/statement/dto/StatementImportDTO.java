package com.jbh.finance.application.feature.statement.dto;

import com.jbh.finance.application.feature.statement.vo.PdfSha256;
import com.jbh.finance.domain.product.vo.ProductId;
import java.time.Instant;
import java.util.UUID;

/**
 * A stored statement import.
 *
 * @param id the import id
 * @param userId the user who owns the product
 * @param productId the product that receives the movements
 * @param pdfSha256 the digest of the uploaded PDF
 * @param status the stored status
 * @param errorCode the error name when {@code FAILED}, else {@code null}
 * @param summary the summary when {@code DONE}, else {@code null}
 * @param startedAt when the import started
 * @param finishedAt when the import ended, or {@code null} while {@code PROCESSING}
 */
public record StatementImportDTO(
    UUID id,
    UUID userId,
    ProductId productId,
    PdfSha256 pdfSha256,
    StatementImportStatus status,
    String errorCode,
    StatementImportSummaryDTO summary,
    Instant startedAt,
    Instant finishedAt) {}
