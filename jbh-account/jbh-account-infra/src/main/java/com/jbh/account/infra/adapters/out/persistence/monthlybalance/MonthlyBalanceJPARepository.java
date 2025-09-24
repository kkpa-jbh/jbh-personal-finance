package com.jbh.account.infra.adapters.out.persistence.monthlybalance;

import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.application.core.ports.output.monthlybalance.AccountMonthlyBalanceQueryRepo;
import com.jbh.account.domain.vo.AccountId;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceUnit;
import jakarta.transaction.Transactional;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
@PersistenceUnit(name = "acctmgmt")
@Transactional
public class MonthlyBalanceJPARepository
    implements PanacheRepository<AccountMonthlyBalanceJPAEntity>, AccountMonthlyBalanceQueryRepo {

  @Override
  public Optional<MonthlyBalanceDTO> findByAccountIdYearAndMonth(
      final AccountId accountId, final Integer balanceYear, final Integer balanceMonth) {
    return find(
            "accountId = :accountId and year = :balanceYear and month = :balanceMonth",
            Parameters.with("accountId", accountId.value())
                .and("balanceYear", balanceYear)
                .and("balanceMonth", balanceMonth))
        .firstResultOptional()
        .map(AccountMonthlyBalanceJPAEntity::toDTO);
  }

  @Override
  public Optional<MonthlyBalanceDTO> findByAccountIdAndPeriod(
      final AccountId accountId, final YearMonth period) {
    return findByAccountIdYearAndMonth(accountId, period.getYear(), period.getMonthValue());
  }

  private List<AccountMonthlyBalanceJPAEntity> findNextFromPeriodInclusiveJPA(
      final AccountId accountId, final YearMonth currentPeriod) {
    return find(
            "accountId = :accountId and period >= :period",
            Parameters.with("accountId", accountId.value()).and("period", currentPeriod))
        .list();
  }

  @Override
  public List<MonthlyBalanceDTO> findNextBalancesFromPeriodInclusive(
      final AccountId accountId, final YearMonth currentPeriod) {
    return findNextFromPeriodInclusiveJPA(accountId, currentPeriod).stream()
        .map(AccountMonthlyBalanceJPAEntity::toDTO)
        .toList();
  }

  @Override
  public Optional<MonthlyBalanceDTO> findLastOfficialReport(final AccountId accountId) {
    final List<AccountMonthlyBalanceJPAEntity> lastOfficialReportList =
        find(
                "accountId = :accountId and officialMonthlyReport = true order by period desc",
                Parameters.with("accountId", accountId.value()))
            .list();

    if (lastOfficialReportList.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(lastOfficialReportList.getFirst().toDTO());
  }
}
