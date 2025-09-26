package com.jbh.account.application.core.services;

import com.jbh.account.application.common.logging.LoggerFactory;
import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.application.core.mappers.MonthlyBalanceMapper;
import com.jbh.account.application.core.services.monthlybalance.MonthlyBalanceService;
import com.jbh.account.domain.entity.AccountMonthlyBalanceDomain;
import com.jbh.account.domain.entity.AccountMovementDomain;
import com.jbh.account.domain.vo.AccountId;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;

public class MonthlyBalanceSyncForUploadedMovements {

  private static final Logger LOG =
      LoggerFactory.getLogger(MonthlyBalanceSyncForUploadedMovements.class);

  private final MonthlyBalanceService monthlyBalanceService;

  public MonthlyBalanceSyncForUploadedMovements(final MonthlyBalanceService monthlyBalanceService) {
    this.monthlyBalanceService = monthlyBalanceService;
  }

  public List<MonthlyBalanceDTO> syncForUploadedMovementsAsync(
      final AccountId accountId, final List<AccountMovementDomain> multipleMovementsDomain) {
    // Group movements by Year-Month based on the movementDate attribute
    final Map<YearMonth, List<AccountMovementDomain>> movementsByPeriodMap =
        multipleMovementsDomain.stream()
            .collect(
                Collectors.groupingBy(
                    movement -> {
                      final int year = movement.getMovementDate().getYear();
                      final int month = movement.getMovementDate().getMonthValue();
                      return YearMonth.of(year, month);
                    }));
    final Stream<YearMonth> movementsPeriodsSorted =
        movementsByPeriodMap.keySet().stream().sorted();

    // Sync Monthly Balances and prepare them for persistence
    final List<AccountMonthlyBalanceDomain> monthlyBalancesToPersist = new ArrayList<>();
    movementsPeriodsSorted.forEach(
        monthlyPeriodKey -> {
          final List<AccountMovementDomain> movementsInPeriod =
              movementsByPeriodMap.get(monthlyPeriodKey);

          LOG.info(
              "Syncing Monthly Balance of {} movements for period {}",
              movementsInPeriod.size(),
              monthlyPeriodKey);

          final AccountMonthlyBalanceDomain accountMonthlyBalance =
              monthlyBalanceService
                  .findByAccountIdAndPeriod(accountId, monthlyPeriodKey)
                  .map(MonthlyBalanceMapper::toDomain)
                  .orElseGet(
                      () -> AccountMonthlyBalanceDomain.withPeriod(accountId, monthlyPeriodKey));

          movementsInPeriod.forEach(accountMonthlyBalance::assignMovement);

          LOG.debug("Monthly balance updated for {}", monthlyPeriodKey);
          monthlyBalancesToPersist.add(accountMonthlyBalance);
        });

    final List<MonthlyBalanceDTO> monthlyBalancesToSyncDTO =
        monthlyBalancesToPersist.stream().map(MonthlyBalanceMapper::toDTO).toList();
    persistBalancesAsync(accountId, monthlyBalancesToSyncDTO);

    return monthlyBalancesToSyncDTO;
  }

  public CompletableFuture<List<MonthlyBalanceDTO>> persistBalancesAsync(
      final AccountId accountId, final List<MonthlyBalanceDTO> monthlyBalances) {
    return monthlyBalanceService.persistBalancesAsync(accountId, monthlyBalances);
  }
}
