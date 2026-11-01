package com.jbh.finance.infra.adapters.out.persistence.statementimport;

import com.jbh.finance.application.feature.statement.dto.StatementImportDTO;
import com.jbh.finance.application.feature.statement.dto.StatementImportStatus;
import com.jbh.finance.application.feature.statement.dto.StatementImportSummaryDTO;
import com.jbh.finance.application.feature.statement.vo.PdfSha256;
import com.jbh.finance.domain.product.vo.ProductId;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Read model of {@code finance.statement_imports}. Rows are written by native statements. */
@Entity
@Getter
@Setter
@Table(name = "statement_imports", schema = "finance")
public class StatementImportJPAEntity extends PanacheEntityBase {

  @Id
  @Column(name = "id")
  public UUID id;

  @Column(name = "user_id")
  public UUID userId;

  @Column(name = "product_id")
  public UUID productId;

  @Column(name = "pdf_sha256", columnDefinition = "TEXT")
  public String pdfSha256;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", columnDefinition = "TEXT")
  public StatementImportStatus status;

  @Column(name = "error_code", columnDefinition = "TEXT")
  public String errorCode;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "summary", columnDefinition = "jsonb")
  public Map<String, Object> summary;

  @Column(name = "started_at")
  public Instant startedAt;

  @Column(name = "finished_at")
  public Instant finishedAt;

  /**
   * Maps the row to its DTO.
   *
   * @param summaryDto the summary already converted from the JSON column, or {@code null}
   * @return the DTO
   */
  public StatementImportDTO toDTO(final StatementImportSummaryDTO summaryDto) {
    return new StatementImportDTO(
        id,
        userId,
        ProductId.of(productId),
        new PdfSha256(pdfSha256),
        status,
        errorCode,
        summaryDto,
        startedAt,
        finishedAt);
  }
}
