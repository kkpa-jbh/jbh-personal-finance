package com.jbh.products.infra.adapters.out.persistence.monthlybalance;

import com.jbh.products.application.core.dto.MonthlyBalanceDTO;
import com.jbh.products.application.core.ports.output.monthlybalance.AccountMonthlyBalanceQueryRepo;
import com.jbh.products.domain.vo.ProductId;
import com.jbh.products.domain.vo.ProductPK;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceUnit;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
@PersistenceUnit(name = "productmgmt")
@Transactional
@Named("monthlyBalanceJPARepository")
public class MonthlyBalanceJPARepository
    implements PanacheRepository<AccountMonthlyBalanceJPAEntity>, AccountMonthlyBalanceQueryRepo {

  private static final String ACCOUNT_ID_PARAM = "accountId";

  @Override
  public Optional<MonthlyBalanceDTO> findByAccountIdYearAndMonth(
      final ProductId accountId, final Integer balanceYear, final Integer balanceMonth) {
    return find(
            "accountId = :accountId and year = :balanceYear and month = :balanceMonth",
            Parameters.with(ACCOUNT_ID_PARAM, accountId.value())
                .and("balanceYear", balanceYear)
                .and("balanceMonth", balanceMonth))
        .firstResultOptional()
        .map(AccountMonthlyBalanceJPAEntity::toDTO);
  }

  @Override
  public Optional<MonthlyBalanceDTO> findByAccountIdAndPeriod(
      final ProductId accountId, final YearMonth period) {
    return findByAccountIdYearAndMonth(accountId, period.getYear(), period.getMonthValue());
  }

  private List<AccountMonthlyBalanceJPAEntity> findNextFromPeriodInclusiveJPA(
      final ProductId accountId, final YearMonth currentPeriod) {
    return find(
            "accountId = :accountId and period >= :period",
            Parameters.with(ACCOUNT_ID_PARAM, accountId.value()).and("period", currentPeriod))
        .list();
  }

  @Override
  public List<MonthlyBalanceDTO> findNextBalancesFromPeriodInclusive(
      final ProductId accountId, final YearMonth currentPeriod) {
    return findNextFromPeriodInclusiveJPA(accountId, currentPeriod).stream()
        .map(AccountMonthlyBalanceJPAEntity::toDTO)
        .toList();
  }

  @Override
  public List<MonthlyBalanceDTO> findByAccountAndPeriods(
      final ProductPK accountPK, final YearMonth startPeriod, final YearMonth endPeriod) {
    return find(
            "accountId = :accountId and period >= :startPeriod and period <= :endPeriod order by period asc",
            Parameters.with(ACCOUNT_ID_PARAM, accountPK.accountId().value())
                .and("startPeriod", startPeriod)
                .and("endPeriod", endPeriod))
        .list()
        .stream()
        .map(AccountMonthlyBalanceJPAEntity::toDTO)
        .toList();
  }

  @Override
  public List<MonthlyBalanceDTO> findAllByAccountIdUntilNow(final ProductId accountId) {
    final YearMonth endPeriod = YearMonth.now();
    return find(
            "accountId = :accountId  and period <= :endPeriod order by period asc",
            Parameters.with(ACCOUNT_ID_PARAM, accountId.value()).and("endPeriod", endPeriod))
        .list()
        .stream()
        .map(AccountMonthlyBalanceJPAEntity::toDTO)
        .toList();
  }

  @Override
  public Optional<MonthlyBalanceDTO> findLastOfficialReport(final ProductId accountId) {
    final List<AccountMonthlyBalanceJPAEntity> lastOfficialReportList =
        find(
                "accountId = :accountId and officialMonthlyReport = true order by period desc",
                Parameters.with(ACCOUNT_ID_PARAM, accountId.value()))
            .list();

    if (lastOfficialReportList.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(lastOfficialReportList.getFirst().toDTO());
  }

  @Override
  public BigDecimal sumNetProfitOfficialReported(final ProductId accountId) {
    try {
      final BigDecimal result =
          getEntityManager()
              .createQuery(
                  """
              SELECT COALESCE(SUM(mb.monthlyNetProfit), 0)
              FROM AccountMonthlyBalanceJPAEntity mb
              WHERE mb.accountId = :accountId
                AND mb.officialMonthlyReport = true
                AND mb.monthlyNetProfit IS NOT NULL
              """,
                  BigDecimal.class)
              .setParameter(ACCOUNT_ID_PARAM, accountId)
              .getSingleResult();

      return result != null ? result : BigDecimal.ZERO;
    } catch (final NoResultException e) {
      return BigDecimal.ZERO;
    }
  }
}
