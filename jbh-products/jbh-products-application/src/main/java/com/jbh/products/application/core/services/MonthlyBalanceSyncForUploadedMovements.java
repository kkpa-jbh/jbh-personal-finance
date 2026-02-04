package com.jbh.products.application.core.services;

import com.jbh.commons.exception.BusinessException;
import com.jbh.products.application.common.logging.LoggerFactory;
import com.jbh.products.application.core.dto.MonthlyBalanceDTO;
import com.jbh.products.application.core.mappers.MonthlyBalanceMapper;
import com.jbh.products.application.core.services.monthlybalance.MonthlyBalanceService;
import com.jbh.products.domain.entity.MonthlyBalanceDomain;
import com.jbh.products.domain.entity.MovementDomain;
import com.jbh.products.domain.vo.ProductId;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;

@SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
public class MonthlyBalanceSyncForUploadedMovements {

  private static final Logger LOG =
      LoggerFactory.getLogger(MonthlyBalanceSyncForUploadedMovements.class);

  private final MonthlyBalanceService monthlyBalanceService;

  public MonthlyBalanceSyncForUploadedMovements(final MonthlyBalanceService monthlyBalanceService) {
    this.monthlyBalanceService = monthlyBalanceService;
  }

  public List<MonthlyBalanceDTO> syncForUploadedMovementsAsync(
      final ProductId accountId, final List<MovementDomain> multipleMovementsDomain) {
    // Group movements by Year-Month based on the movementDate attribute
    final Map<YearMonth, List<MovementDomain>> movementsByPeriodMap =
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
    final List<MonthlyBalanceDomain> monthlyBalancesToPersist = new ArrayList<>();
    movementsPeriodsSorted.forEach(
        monthlyPeriodKey -> {
          final List<MovementDomain> movementsInPeriod = movementsByPeriodMap.get(monthlyPeriodKey);

          LOG.info(
              "Syncing Monthly Balance of {} movements for period {}",
              movementsInPeriod.size(),
              monthlyPeriodKey);

          final MonthlyBalanceDomain accountMonthlyBalance =
              monthlyBalanceService
                  .findByAccountIdAndPeriod(accountId, monthlyPeriodKey)
                  .map(MonthlyBalanceMapper::toDomain)
                  .orElseGet(() -> MonthlyBalanceDomain.withPeriod(accountId, monthlyPeriodKey));

          // TODO Should It return a AccountBusinessException?
          for (final MovementDomain movement : movementsInPeriod) {
            try {
              accountMonthlyBalance.assignMovement(movement);
            } catch (final BusinessException e) {
              throw new RuntimeException(e);
            }
          }

          LOG.debug("Monthly balance updated for {}", monthlyPeriodKey);
          monthlyBalancesToPersist.add(accountMonthlyBalance);
        });

    final List<MonthlyBalanceDTO> monthlyBalancesToSyncDTO =
        monthlyBalancesToPersist.stream().map(MonthlyBalanceMapper::toDTO).toList();

    persistBalancesAsync(accountId, monthlyBalancesToSyncDTO);

    return monthlyBalancesToSyncDTO;
  }

  public CompletableFuture<List<MonthlyBalanceDTO>> persistBalancesAsync(
      final ProductId accountId, final List<MonthlyBalanceDTO> monthlyBalances) {
    return monthlyBalanceService.persistBalancesAsync(accountId, monthlyBalances);
  }
}
