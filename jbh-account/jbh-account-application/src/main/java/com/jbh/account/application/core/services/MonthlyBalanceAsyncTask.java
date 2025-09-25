package com.jbh.account.application.core.services;

import static com.jbh.account.application.core.mappers.MonthlyBalanceMapper.toDTO;
import static com.jbh.account.application.core.mappers.MonthlyBalanceMapper.toDomain;
import static com.jbh.account.domain.utils.MoneyUtils.JBH_ZERO;

import com.jbh.account.application.async.AsyncTaskExecutor;
import com.jbh.account.application.async.vo.AsyncTask;
import com.jbh.account.application.async.vo.AsyncTaskType;
import com.jbh.account.application.common.logging.LoggerFactory;
import com.jbh.account.application.core.comparator.AccountMonthlyBalanceComparators;
import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.application.core.mappers.MonthlyBalanceMapper;
import com.jbh.account.application.core.services.monthlybalance.MonthlyBalanceService;
import com.jbh.account.domain.entity.AccountMonthlyBalanceDomain;
import com.jbh.account.domain.entity.AccountMovementDomain;
import com.jbh.account.domain.vo.AccountId;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;

public class MonthlyBalanceAsyncTask {

  private static final Logger LOG = LoggerFactory.getLogger(MonthlyBalanceAsyncTask.class);

  private final MonthlyBalanceService monthlyBalanceService;
  private final AsyncTaskExecutor asyncTaskExecutor;

  public MonthlyBalanceAsyncTask(
      final MonthlyBalanceService monthlyBalanceService,
      final AsyncTaskExecutor asyncTaskExecutor) {
    this.monthlyBalanceService = monthlyBalanceService;
    this.asyncTaskExecutor = asyncTaskExecutor;
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
    persistBalancesAndSyncThemASYNC(accountId, monthlyBalancesToSyncDTO);

    // FIXME: Should return DTO ?
    return monthlyBalancesToSyncDTO;
  }

  /**
   * Saves the monthly balances in the database and syncs them asynchronously. This is called when
   * uploading movements from file or creating a new movement.
   */
  public CompletableFuture<List<MonthlyBalanceDTO>> persistBalancesAndSyncThemASYNC(
      final AccountId accountId, final List<MonthlyBalanceDTO> monthlyBalances) {

    if (monthlyBalances == null || monthlyBalances.isEmpty()) {
      LOG.warn("No monthly balances available for saving them ASYNC");
      return CompletableFuture.completedFuture(Collections.emptyList());
    }

    final YearMonth initPeriod = monthlyBalances.getFirst().period();
    final YearMonth lastPeriod = monthlyBalances.getLast().period();
    LOG.info(
        String.format(
            "Starting async task to sync balances from %s to %s", initPeriod, lastPeriod));

    // Create AsyncTask metadata
    final Map<String, Object> metadata =
        new HashMap<>(
            Map.of(
                "accountId", accountId.value().toString(),
                "initPeriod", initPeriod.toString(),
                "lastPeriod", lastPeriod.toString(),
                "balancesCount", monthlyBalances.size()));

    final AsyncTask asyncTask = new AsyncTask(AsyncTaskType.MONTHLY_BALANCES_SYNC, metadata);

    // Create Callable that contains the entire business logic
    return asyncTaskExecutor.submitTask(
        asyncTask,
        () -> {
          // Step 1: Save balances (executes first)
          monthlyBalanceService.saveMultiBalances(monthlyBalances);

          Thread.sleep(Duration.ofSeconds(1).toMillis());
          // Step 3: Return profit/opening balances (they contain the combined results)
          // The monthly balances are already persisted in the database
          return adjustCurrentAndNextMonthlyBalancesAsync(accountId, initPeriod, lastPeriod);
        });
  }

  /**
   * Syncs the current and next monthly balances for the given periods. THe monthly balances are
   * already persisted in the database.
   */
  private List<MonthlyBalanceDTO> adjustCurrentAndNextMonthlyBalancesAsync(
      final AccountId accountId, final YearMonth initPeriod, final YearMonth endPeriod) {

    LOG.info("Monthly balances should be already persisted in the database");

    LOG.info(
        "Adjusting Opening/Profit Balances for account {}" + " from period {} to period {}",
        accountId.value(),
        initPeriod,
        endPeriod);

    final List<MonthlyBalanceDTO> existingNextPeriodBalanceDTO =
        monthlyBalanceService.findNextBalancesFromPeriodInclusive(accountId, initPeriod);

    if (existingNextPeriodBalanceDTO == null || existingNextPeriodBalanceDTO.isEmpty()) {
      LOG.warn(
          "No future balances to sync profit were found for account {} and period {}",
          accountId,
          initPeriod);
      return Collections.emptyList();
    }

    final List<AccountMonthlyBalanceDomain> existingNextBalancesFromPeriod =
        existingNextPeriodBalanceDTO.stream().map(MonthlyBalanceMapper::toDomain).toList();
    final ConcurrentMap<YearMonth, AccountMonthlyBalanceDomain> existingDomainBalancesMap =
        existingNextBalancesFromPeriod.stream()
            .collect(
                Collectors.toConcurrentMap(
                    AccountMonthlyBalanceDomain::getPeriod, Function.identity()));

    YearMonth currentPeriod = initPeriod;
    final List<AccountMonthlyBalanceDomain> profitBalancesSynced = new ArrayList<>();

    // Syncing current and next monthly balances
    LOG.info("Syncing current and next monthly balances {} - {} ", currentPeriod, endPeriod);
    while (isAvailablePeriod(currentPeriod, endPeriod)) {

      LOG.info("Syncing Movement Balance and Monthly Profit for period: {}", currentPeriod);
      final AccountMonthlyBalanceDomain currentMonthlyBalance =
          existingDomainBalancesMap.get(currentPeriod);
      currentMonthlyBalance.recalculateBalances();

      final YearMonth nextPeriod = currentMonthlyBalance.getPeriod().plusMonths(1);
      // LOG.debug("Syncing Opening Balance for next period: {} ", nextPeriod);
      // Syncing Next month opening balance with current month closing balance
      AccountMonthlyBalanceDomain nextMonthlyBalanceOfCurrent =
          existingNextBalancesFromPeriod.stream()
              .filter(mb -> mb.getPeriod().equals(nextPeriod))
              .findFirst()
              .orElse(null);
      // If not found in database, let's create new one with the same balance snapshot from previous
      // month.
      if (nextMonthlyBalanceOfCurrent == null) {
        LOG.info("Creating new monthly balance for period: {}", nextPeriod);
        final boolean isEndPeriod = currentPeriod.equals(endPeriod);
        // If it is the last period, just set the opening balance (Next Future Month)
        final BigDecimal closingBalance =
            isEndPeriod ? JBH_ZERO : currentMonthlyBalance.getClosingBalance();
        nextMonthlyBalanceOfCurrent =
            toDomain(
                MonthlyBalanceDTO.withInitialDataForNextMonth(
                    accountId, nextPeriod, closingBalance, !isEndPeriod));
        existingDomainBalancesMap.putIfAbsent(nextPeriod, nextMonthlyBalanceOfCurrent);
      }
      LOG.info("Adjusting Opening Balance for next period: {}", nextPeriod);
      nextMonthlyBalanceOfCurrent.assignOpeningBalance(currentMonthlyBalance);

      // Preparing to persist
      if (!profitBalancesSynced.contains(currentMonthlyBalance)) {
        profitBalancesSynced.add(currentMonthlyBalance);
      }
      if (!profitBalancesSynced.contains(nextMonthlyBalanceOfCurrent)) {
        profitBalancesSynced.add(nextMonthlyBalanceOfCurrent);
      }

      // Increasing the while index
      currentPeriod = currentPeriod.plusMonths(1);
    }

    // Sort using natural ordering (period ASC)
    profitBalancesSynced.sort(AccountMonthlyBalanceComparators.BY_PERIOD_ASC);

    // persist monthly balances with profit and opening balances synced.
    final List<MonthlyBalanceDTO> profitBalancesSyncedDto =
        profitBalancesSynced.stream().map(MonthlyBalanceMapper::toDTO).toList();
    monthlyBalanceService.saveMultiBalances(profitBalancesSyncedDto);

    return profitBalancesSyncedDto;
  }

  private boolean isAvailablePeriod(final YearMonth currentPeriod, final YearMonth endPeriod) {
    return currentPeriod.isBefore(getEdgePeriod())
        && currentPeriod.isBefore(endPeriod.plusMonths(1));
  }

  private YearMonth getEdgePeriod() {
    return YearMonth.now().plusMonths(1);
  }

  public AccountMonthlyBalanceDomain syncForNewMovement(final AccountMovementDomain newMovement) {
    // Implementation for syncing monthly balances
    final AccountId accountId = newMovement.getAccountId();
    LOG.info(
        "Syncing monthly balance for account {} and movement date {}",
        newMovement.getAccountId().value(),
        newMovement.getMovementDate());
    final LocalDate movementDate = newMovement.getMovementDate();
    final YearMonth movementPeriod = YearMonth.from(movementDate);
    final AccountMonthlyBalanceDomain accountMonthlyBalance =
        monthlyBalanceService
            .findByAccountIdAndPeriod(accountId, movementPeriod)
            .map(MonthlyBalanceMapper::toDomain)
            .orElseGet(() -> AccountMonthlyBalanceDomain.withPeriod(accountId, movementPeriod));

    accountMonthlyBalance.assignMovement(newMovement);
    persistBalancesAndSyncThemASYNC(
        accountId, Collections.singletonList(toDTO(accountMonthlyBalance)));

    return accountMonthlyBalance;
  }

  public Optional<MonthlyBalanceDTO> findByAccountIdYearAndMonth(
      final AccountId accountId, final Integer balanceYear, final Integer balanceMonth) {
    return monthlyBalanceService.findByAccountIdYearAndMonth(accountId, balanceYear, balanceMonth);
  }
}
