package com.jbh.account.infra.adapters.out.persistence.monthlybalance;

import com.jbh.account.domain.vo.AccountId;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceUnit;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
@PersistenceUnit(name = "acctmgmt")
public class MonthlyBalanceJPARepository
    implements PanacheRepository<AccountMonthlyBalanceJPAEntity> {

  public Optional<AccountMonthlyBalanceJPAEntity> findByAccountIdYearAndMonth(
      final AccountId accountId, final Integer balanceYear, final Integer balanceMonth) {
    return find(
            "accountId = :accountId and year = :balanceYear and month = :balanceMonth",
            Parameters.with("accountId", accountId.value())
                .and("balanceYear", balanceYear)
                .and("balanceMonth", balanceMonth))
        .firstResultOptional();
  }

  public List<AccountMonthlyBalanceJPAEntity> findNextBalancesFromPeriodInclusive(
      final AccountId accountId, final YearMonth currentPeriod) {
    return find(
            "accountId = :accountId and period > :period",
            Parameters.with("accountId", accountId.value()).and("period", currentPeriod))
        .list();
  }
}
