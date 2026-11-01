package com.jbh.finance.infra.adapters.out.persistence.statementimport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jbh.finance.application.feature.statement.dto.NewStatementImportDTO;
import com.jbh.finance.application.feature.statement.dto.StatementImportDTO;
import com.jbh.finance.application.feature.statement.dto.StatementImportSummaryDTO;
import com.jbh.finance.application.feature.statement.ports.output.StatementImportRepository;
import com.jbh.finance.application.feature.statement.vo.PdfSha256;
import com.jbh.finance.domain.product.vo.ProductId;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class StatementImportRepositoryAdapter implements StatementImportRepository {

  private static final int NO_ROWS = 0;

  @Inject StatementImportJPARepository jpaRepo;
  @Inject ObjectMapper objectMapper;

  /** Reads decimals as {@code BigDecimal}, so amounts keep their exact value and scale. */
  private ObjectMapper summaryReader;

  @PostConstruct
  void initSummaryReader() {
    summaryReader = objectMapper.copy().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
  }

  @Override
  public boolean insertProcessing(final NewStatementImportDTO newImport) {
    return jpaRepo.insertProcessing(newImport) > NO_ROWS;
  }

  @Override
  public int markStaleAsFailed(
      final ProductId productId, final PdfSha256 pdfSha256, final Duration staleAfter) {
    return jpaRepo.markStaleAsFailed(productId, pdfSha256, staleAfter);
  }

  @Override
  public boolean markDone(final UUID importId, final StatementImportSummaryDTO summary) {
    return jpaRepo.markDone(importId, toJson(summary)) > NO_ROWS;
  }

  @Override
  public boolean markFailed(final UUID importId, final String errorCode) {
    return jpaRepo.markFailed(importId, errorCode) > NO_ROWS;
  }

  @Override
  public Optional<StatementImportDTO> findByIdAndProduct(
      final UUID importId, final ProductId productId) {
    return jpaRepo
        .findByIdAndProduct(importId, productId.value())
        .map(entity -> entity.toDTO(toSummary(entity)));
  }

  // The cause is dropped on purpose: its message can quote statement text.
  @SuppressWarnings("PMD.PreserveStackTrace")
  private StatementImportSummaryDTO toSummary(final StatementImportJPAEntity entity) {
    if (entity.getSummary() == null) {
      return null;
    }
    try {
      return summaryReader.readValue(entity.getSummary(), StatementImportSummaryDTO.class);
    } catch (final JsonProcessingException e) {
      throw new IllegalStateException("Cannot read the stored statement import summary");
    }
  }

  private String toJson(final StatementImportSummaryDTO summary) {
    try {
      return objectMapper.writeValueAsString(summary);
    } catch (final JsonProcessingException e) {
      throw new IllegalStateException("Cannot serialize the statement import summary", e);
    }
  }
}
