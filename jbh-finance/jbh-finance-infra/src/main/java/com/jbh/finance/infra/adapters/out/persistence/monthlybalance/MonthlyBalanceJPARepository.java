package com.jbh.finance.infra.adapters.out.persistence.monthlybalance;

import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.finance.application.feature.monthlybalance.ports.output.MonthlyBalanceQueryRepo;
import com.jbh.finance.domain.product.vo.ProductId;
import com.jbh.finance.domain.product.vo.ProductPK;
import com.jbh.finance.domain.shared.vo.PeriodRange;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceUnit;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
@PersistenceUnit(name = "finance")
@Transactional
@Named("monthlyBalanceJPARepository")
public class MonthlyBalanceJPARepository
    implements PanacheRepository<MonthlyBalanceJPAEntity>, MonthlyBalanceQueryRepo {

  private static final String PRODUCT_ID_PARAM = "productId";

  @Override
  public List<MonthlyBalanceDTO> findByAccountAndPeriods(
      final ProductPK accountPK, final YearMonth startPeriod, final YearMonth endPeriod) {
    return find(
            "productId = :productId and period >= :startPeriod and period <= :endPeriod order by period asc",
            Parameters.with(PRODUCT_ID_PARAM, accountPK.productId().value())
                .and("startPeriod", startPeriod)
                .and("endPeriod", endPeriod))
        .list()
        .stream()
        .map(MonthlyBalanceJPAEntity::toDTO)
        .toList();
  }

  @Override
  public Optional<MonthlyBalanceDTO> findByAccountIdYearAndMonth(
      final ProductId productId, final Integer balanceYear, final Integer balanceMonth) {
    return find(
            "productId = :productId and year = :balanceYear and month = :balanceMonth",
            Parameters.with(PRODUCT_ID_PARAM, productId.value())
                .and("balanceYear", balanceYear)
                .and("balanceMonth", balanceMonth))
        .firstResultOptional()
        .map(MonthlyBalanceJPAEntity::toDTO);
  }

  @Override
  public Optional<MonthlyBalanceDTO> findByAccountIdAndPeriod(
      final ProductId productId, final YearMonth period) {
    return findByAccountIdYearAndMonth(productId, period.getYear(), period.getMonthValue());
  }

  @Override
  public Optional<MonthlyBalanceDTO> findLastOfficialReport(final ProductId productId) {
    final List<MonthlyBalanceJPAEntity> lastOfficialReportList =
        find(
                "productId = :productId and officialMonthlyReport = true order by period desc",
                Parameters.with(PRODUCT_ID_PARAM, productId.value()))
            .list();

    if (lastOfficialReportList.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(lastOfficialReportList.getFirst().toDTO());
  }

  @Override
  public BigDecimal sumNetProfitOfficialReported(final ProductId productId) {
    try {
      final BigDecimal result =
          getEntityManager()
              .createQuery(
                  """
              SELECT COALESCE(SUM(mb.monthlyNetProfit), 0)
              FROM MonthlyBalanceJPAEntity mb
              WHERE mb.productId = :productId
                AND mb.officialMonthlyReport = true
                AND mb.monthlyNetProfit IS NOT NULL
              """,
                  BigDecimal.class)
              .setParameter(PRODUCT_ID_PARAM, productId)
              .getSingleResult();

      return result != null ? result : BigDecimal.ZERO;
    } catch (final NoResultException e) {
      return BigDecimal.ZERO;
    }
  }

  @Override
  public List<MonthlyBalanceDTO> findNextBalancesFromPeriodInclusive(
      final ProductId productId, final YearMonth currentPeriod) {
    return findNextFromPeriodInclusiveJPA(productId, currentPeriod).stream()
        .map(MonthlyBalanceJPAEntity::toDTO)
        .toList();
  }

  private List<MonthlyBalanceJPAEntity> findNextFromPeriodInclusiveJPA(
      final ProductId productId, final YearMonth currentPeriod) {
    return find(
            "productId = :productId and period >= :period",
            Parameters.with(PRODUCT_ID_PARAM, productId.value()).and("period", currentPeriod))
        .list();
  }

  @Override
  public List<MonthlyBalanceDTO> findAllByAccountIdUntilNow(final ProductId productId) {
    final YearMonth endPeriod = YearMonth.now();
    return find(
            "productId = :productId  and period <= :endPeriod order by period asc",
            Parameters.with(PRODUCT_ID_PARAM, productId.value()).and("endPeriod", endPeriod))
        .list()
        .stream()
        .map(MonthlyBalanceJPAEntity::toDTO)
        .toList();
  }

  @Override
  public List<MonthlyBalanceDTO> findByProductIdsAndPeriods(
      final List<ProductId> productIds, final PeriodRange periodRange) {

    if (productIds == null || productIds.isEmpty()) {
      return Collections.emptyList();
    }

    final List<UUID> productUuids = productIds.stream().map(ProductId::value).toList();

    final List<MonthlyBalanceJPAEntity> entities =
        find(
                "productId IN :productIds AND period >= :startPeriod AND "
                    + buildEndPeriod(periodRange.isEndPeriodExclusive())
                    + " ORDER BY productId, period DESC",
                Parameters.with("productIds", productUuids)
                    .and("startPeriod", periodRange.getStartPeriod())
                    .and("endPeriod", periodRange.getEndPeriod()))
            .list();

    return entities.stream().map(MonthlyBalanceJPAEntity::toDTO).collect(Collectors.toList());
  }

  private static String buildEndPeriod(final boolean isEndPeriodExclusive) {
    return isEndPeriodExclusive ? "period < :endPeriod" : "period <= :endPeriod";
  }
}
