package com.jbh.finance.infra.adapters.out.persistence.statementimport;

import com.jbh.finance.application.feature.statement.dto.NewStatementImportDTO;
import com.jbh.finance.application.feature.statement.vo.PdfSha256;
import com.jbh.finance.application.shared.exceptions.BusinessApplicationExceptionType;
import com.jbh.finance.domain.product.vo.ProductId;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceUnit;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/** Every write is one atomic SQL statement, so two instances never both win. */
@ApplicationScoped
@PersistenceUnit(name = "finance")
public class StatementImportJPARepository implements PanacheRepository<StatementImportJPAEntity> {

  private static final String ID_PARAM = "id";
  private static final String PRODUCT_ID_PARAM = "productId";
  private static final String SHA_PARAM = "pdfSha256";
  private static final String ERROR_CODE_PARAM = "errorCode";
  private static final String TIMED_OUT_CODE =
      BusinessApplicationExceptionType.STATEMENT_IMPORT_TIMED_OUT.name();
  private static final double MILLIS_PER_SECOND = 1000.0;

  private static final String INSERT_PROCESSING =
      "INSERT INTO finance.statement_imports (id, user_id, product_id, pdf_sha256, status)"
          + " VALUES (:id, :userId, :productId, :pdfSha256, 'PROCESSING')"
          + " ON CONFLICT (product_id, pdf_sha256) WHERE status <> 'FAILED' DO NOTHING";

  private static final String MARK_STALE =
      "UPDATE finance.statement_imports"
          + " SET status = 'FAILED', error_code = :errorCode, finished_at = CURRENT_TIMESTAMP"
          + " WHERE product_id = :productId AND pdf_sha256 = :pdfSha256"
          + " AND status = 'PROCESSING'"
          + " AND started_at < CURRENT_TIMESTAMP - make_interval(secs => :staleSeconds)";

  private static final String MARK_DONE =
      "UPDATE finance.statement_imports"
          + " SET status = 'DONE', summary = CAST(:summary AS jsonb),"
          + " finished_at = CURRENT_TIMESTAMP"
          + " WHERE id = :id AND status = 'PROCESSING'";

  private static final String MARK_FAILED =
      "UPDATE finance.statement_imports"
          + " SET status = 'FAILED', error_code = :errorCode, finished_at = CURRENT_TIMESTAMP"
          + " WHERE id = :id AND status = 'PROCESSING'";

  /** Returns the rows inserted: 1, or 0 when the PDF is already PROCESSING or DONE. */
  public int insertProcessing(final NewStatementImportDTO newImport) {
    return getEntityManager()
        .createNativeQuery(INSERT_PROCESSING)
        .setParameter(ID_PARAM, newImport.id())
        .setParameter("userId", newImport.userId())
        .setParameter(PRODUCT_ID_PARAM, newImport.productId().value())
        .setParameter(SHA_PARAM, newImport.pdfSha256().value())
        .executeUpdate();
  }

  public int markStaleAsFailed(
      final ProductId productId, final PdfSha256 pdfSha256, final Duration staleAfter) {
    return getEntityManager()
        .createNativeQuery(MARK_STALE)
        .setParameter(ERROR_CODE_PARAM, TIMED_OUT_CODE)
        .setParameter(PRODUCT_ID_PARAM, productId.value())
        .setParameter(SHA_PARAM, pdfSha256.value())
        .setParameter("staleSeconds", staleAfter.toMillis() / MILLIS_PER_SECOND)
        .executeUpdate();
  }

  public int markDone(final UUID id, final String summaryJson) {
    return getEntityManager()
        .createNativeQuery(MARK_DONE)
        .setParameter(ID_PARAM, id)
        .setParameter("summary", summaryJson)
        .executeUpdate();
  }

  public int markFailed(final UUID id, final String errorCode) {
    return getEntityManager()
        .createNativeQuery(MARK_FAILED)
        .setParameter(ID_PARAM, id)
        .setParameter(ERROR_CODE_PARAM, errorCode)
        .executeUpdate();
  }

  public Optional<StatementImportJPAEntity> findByIdAndProduct(
      final UUID id, final UUID productId) {
    return find(
            "id = :id and productId = :productId",
            Parameters.with(ID_PARAM, id).and(PRODUCT_ID_PARAM, productId))
        .singleResultOptional();
  }
}
