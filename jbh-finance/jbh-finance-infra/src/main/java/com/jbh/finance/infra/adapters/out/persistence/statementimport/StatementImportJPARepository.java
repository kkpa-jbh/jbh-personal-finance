package com.jbh.finance.infra.adapters.out.persistence.statementimport;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceUnit;
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
  public int insertProcessing(
      final UUID id, final UUID userId, final UUID productId, final String pdfSha256) {
    return getEntityManager()
        .createNativeQuery(INSERT_PROCESSING)
        .setParameter(ID_PARAM, id)
        .setParameter("userId", userId)
        .setParameter(PRODUCT_ID_PARAM, productId)
        .setParameter(SHA_PARAM, pdfSha256)
        .executeUpdate();
  }

  public int markStaleAsFailed(
      final UUID productId, final String pdfSha256, final String errorCode, final double seconds) {
    return getEntityManager()
        .createNativeQuery(MARK_STALE)
        .setParameter(ERROR_CODE_PARAM, errorCode)
        .setParameter(PRODUCT_ID_PARAM, productId)
        .setParameter(SHA_PARAM, pdfSha256)
        .setParameter("staleSeconds", seconds)
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
