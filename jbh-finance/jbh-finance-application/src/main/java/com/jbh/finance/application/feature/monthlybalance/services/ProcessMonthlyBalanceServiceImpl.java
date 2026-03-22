package com.jbh.finance.application.feature.monthlybalance.services;

import static com.jbh.commons.util.JbhMoneyUtils.JBH_ZERO;
import static com.jbh.finance.application.feature.monthlybalance.mappers.MonthlyBalanceMapper.toDTO;
import static com.jbh.finance.application.feature.monthlybalance.mappers.MonthlyBalanceMapper.toDomain;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.async.AsyncTaskExecutor;
import com.jbh.finance.application.async.vo.AsyncTask;
import com.jbh.finance.application.async.vo.AsyncTaskType;
import com.jbh.finance.application.feature.monthlybalance.comparator.MonthlyBalanceComparators;
import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.finance.application.feature.monthlybalance.mappers.MonthlyBalanceMapper;
import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.application.feature.movement.mappers.MovementMapper;
import com.jbh.finance.application.feature.movement.validation.movement_type.MovementTypeValidatorStrategy;
import com.jbh.finance.application.feature.movement.validation.movement_type.MovementValidationStrategyFactory;
import com.jbh.finance.application.feature.product.services.ProductLifecycleService;
import com.jbh.finance.application.shared.exceptions.BusinessApplicationExceptionType;
import com.jbh.finance.domain.monthlybalance.MonthlyBalanceDomain;
import com.jbh.finance.domain.movement.vo.MovementType;
import com.jbh.finance.domain.product.vo.ProductId;
import java.math.BigDecimal;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("PMD.CouplingBetweenObjects")
public class ProcessMonthlyBalanceServiceImpl implements ProcessMonthlyBalanceService {
  private static final Logger LOG = LoggerFactory.getLogger(ProcessMonthlyBalanceServiceImpl.class);

  private final AsyncTaskExecutor asyncTaskExecutor;
  private final ProductLifecycleService accountService;
  private final MovementValidationStrategyFactory movValidationStrategyFactory;
  private final MonthlyBalanceLifecycleService lifecycleSrv;

  public ProcessMonthlyBalanceServiceImpl(
      final MonthlyBalanceLifecycleService monthlyBalanceLifecycleService,
      final AsyncTaskExecutor asyncTaskExecutor,
      final ProductLifecycleService accountService) {
    this.lifecycleSrv = monthlyBalanceLifecycleService;
    this.asyncTaskExecutor = asyncTaskExecutor;
    this.accountService = accountService;
    this.movValidationStrategyFactory = new MovementValidationStrategyFactory();
  }

  @Override
  public void validateNewMovementForOfficialMonthlyReport(final MovementDTO movementDTO)
      throws BusinessException {
    final YearMonth movementPeriod = YearMonth.from(movementDTO.movementDate());
    final ProductId accountId = movementDTO.productId();
    final BigDecimal balanceSnapshot = movementDTO.balanceSnapshot();
    final BigDecimal movementAmount = movementDTO.movementAmount();
    final MovementType movementType = movementDTO.movementType();

    final Optional<MonthlyBalanceDTO> monthlyBalanceQuery =
        findByAccountIdAndPeriod(accountId, movementPeriod);
    if (monthlyBalanceQuery.isPresent()) {
      // Do not allow to add a snapshot after the monthly balance was officially reported
      final MonthlyBalanceDTO existingMonthlyBalance = monthlyBalanceQuery.get();
      final var isMonthOfficiallyReported =
          isMonthOfficiallyReportedValid(existingMonthlyBalance, balanceSnapshot);

      // Validate the new movement does not exceed the monthly balance reported
      if (isMonthOfficiallyReported) {
        final MovementTypeValidatorStrategy strategy =
            movValidationStrategyFactory.getStrategy(movementType);
        strategy.validateMovementAgainstOfficialBalance(movementAmount, existingMonthlyBalance);
      }
    }
  }

  private boolean isMonthOfficiallyReportedValid(
      final MonthlyBalanceDTO existingMonthlyBalance, final BigDecimal balanceSnapshot)
      throws BusinessException {
    final boolean isMonthOfficiallyReported = existingMonthlyBalance.officialMonthlyReport();
    if (isMonthOfficiallyReported && balanceSnapshot != null) {
      throw new BusinessException(BusinessApplicationExceptionType.SNAPSHOT_AFTER_OFFICIAL_REPORT);
    }
    return isMonthOfficiallyReported;
  }

  @Override
  public MonthlyBalanceDTO syncForNewMovement(final MovementDTO newMovement)
      throws BusinessException {
    // Implementation for syncing monthly balances
    final ProductId productId = newMovement.productId();
    LOG.info(
        "Syncing monthly balance for productDTO {} and new movement date {}",
        productId,
        newMovement.movementDate());
    final var accountMonthlyBalance = findMonthlyBalanceOfMovement(newMovement, productId);

    accountMonthlyBalance.assignMovement(MovementMapper.toDomain(newMovement));
    final var syncedMonthlyBalanceDTO = toDTO(accountMonthlyBalance);
    persistBalancesAsync(productId, Collections.singletonList(syncedMonthlyBalanceDTO));

    return syncedMonthlyBalanceDTO;
  }

  private MonthlyBalanceDomain findMonthlyBalanceOfMovement(
      final MovementDTO newMovement, final ProductId productId) {
    final LocalDate movementDate = newMovement.movementDate();
    final YearMonth movementPeriod = YearMonth.from(movementDate);
    return findByAccountIdAndPeriod(productId, movementPeriod)
        .map(MonthlyBalanceMapper::toDomain)
        .orElseGet(() -> MonthlyBalanceDomain.withPeriod(productId, movementPeriod));
  }

  private void saveMultiBalances(final List<MonthlyBalanceDTO> profitBalancesSyncedDto) {
    lifecycleSrv.saveMultiBalances(profitBalancesSyncedDto);
  }

  /**
   * Syncs the current and next monthly balances for the given periods. THe monthly balances are
   * already persisted in the database.
   */
  private List<MonthlyBalanceDTO> adjustCurrentAndNextMonthlyBalancesAsync(
      final ProductId accountId, final YearMonth initPeriod, final YearMonth endPeriod)
      throws BusinessException {

    LOG.info(
        "Monthly balances from{} to {} for the productDTO {} should be already persisted in the database",
        initPeriod,
        endPeriod,
        accountId);

    LOG.info(
        "Adjusting Opening/Profit Balances for productDTO {}" + " from period {} to period {}",
        accountId,
        initPeriod,
        endPeriod);

    final List<MonthlyBalanceDTO> existingNextPeriodBalanceDTO =
        lifecycleSrv.findNextBalancesFromPeriodInclusive(accountId, initPeriod);

    if (existingNextPeriodBalanceDTO == null || existingNextPeriodBalanceDTO.isEmpty()) {
      LOG.warn(
          "No future balances to sync profit were found for productDTO {} and period {}",
          accountId,
          initPeriod);
      return Collections.emptyList();
    }

    final List<MonthlyBalanceDomain> existingNextBalancesFromPeriod =
        existingNextPeriodBalanceDTO.stream().map(MonthlyBalanceMapper::toDomain).toList();
    final ConcurrentMap<YearMonth, MonthlyBalanceDomain> existingDomainBalancesMap =
        existingNextBalancesFromPeriod.stream()
            .collect(
                Collectors.toConcurrentMap(MonthlyBalanceDomain::getPeriod, Function.identity()));

    YearMonth currentPeriod = initPeriod;
    final List<MonthlyBalanceDomain> profitBalancesSynced = new ArrayList<>();

    // Syncing current and next monthly balances
    // FIXME: Using now() is not a good idea
    final YearMonth now = YearMonth.now();
    LOG.info("Syncing current and next monthly balances {} - {} ", currentPeriod, endPeriod);
    while (isAvailablePeriod(now, currentPeriod, endPeriod)) {

      LOG.info("Syncing Movement Balance and Monthly Profit for period: {}", currentPeriod);
      final MonthlyBalanceDomain currentMonthlyBalance =
          existingDomainBalancesMap.get(currentPeriod);
      currentMonthlyBalance.recalculateBalances();

      final YearMonth nextPeriod = currentMonthlyBalance.getPeriod().plusMonths(1);
      // LOG.debug("Syncing Opening Balance for next period: {} ", nextPeriod);
      // Syncing Next month opening balance with current month closing balance
      MonthlyBalanceDomain nextMonthlyBalanceOfCurrent =
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

      final boolean skipNextMonthBalanceAdjustment =
          currentMonthlyBalance.isOfficialMonthlyReport()
              && nextMonthlyBalanceOfCurrent.isOfficialMonthlyReport();
      nextMonthlyBalanceOfCurrent.assignOpeningBalance(currentMonthlyBalance);

      // Preparing to persist
      if (!profitBalancesSynced.contains(currentMonthlyBalance)) {
        profitBalancesSynced.add(currentMonthlyBalance);
      }
      if (!profitBalancesSynced.contains(nextMonthlyBalanceOfCurrent)
          && !skipNextMonthBalanceAdjustment) {
        profitBalancesSynced.add(nextMonthlyBalanceOfCurrent);
      }

      // Increasing the while index
      currentPeriod = currentPeriod.plusMonths(1);
    }

    // Sort using natural ordering (period ASC)
    profitBalancesSynced.sort(MonthlyBalanceComparators.BY_PERIOD_ASC);

    // persist monthly balances with profit and opening balances synced.
    final List<MonthlyBalanceDTO> profitBalancesSyncedDto =
        profitBalancesSynced.stream().map(MonthlyBalanceMapper::toDTO).toList();
    saveMultiBalances(profitBalancesSyncedDto);

    return profitBalancesSyncedDto;
  }

  private boolean isAvailablePeriod(
      final YearMonth now, final YearMonth currentPeriod, final YearMonth endPeriod) {
    final boolean isAvailablePeriod =
        currentPeriod.isBefore(getEdgePeriod(now))
            && currentPeriod.isBefore(endPeriod.plusMonths(1));
    if (!isAvailablePeriod) {
      LOG.warn("It's not an available period {} - {}", currentPeriod, endPeriod);
    }
    return isAvailablePeriod;
  }

  private YearMonth getEdgePeriod(final YearMonth now) {
    return now.plusMonths(1);
  }

  @Override
  public CompletableFuture<List<MonthlyBalanceDTO>> persistBalancesAsync(
      final ProductId accountId, final List<MonthlyBalanceDTO> monthlyBalances) {

    if (monthlyBalances == null || monthlyBalances.isEmpty()) {
      LOG.warn("No monthly balances available for saving them ASYNC");
      return CompletableFuture.completedFuture(Collections.emptyList());
    }

    final YearMonth initPeriod = monthlyBalances.getFirst().period();
    final YearMonth lastPeriod = monthlyBalances.getLast().period();

    // Create AsyncTask metadata
    final Map<String, Object> metadata =
        new HashMap<>(
            Map.of(
                "productId", accountId.value().toString(),
                "initPeriod", initPeriod.toString(),
                "lastPeriod", lastPeriod.toString(),
                "balancesCount", monthlyBalances.size()));

    final AsyncTask asyncTask = new AsyncTask(AsyncTaskType.MONTHLY_BALANCES_SYNC, metadata);

    LOG.info(
        "Preparing to submit async task {} to persist balances from {} to {} for productDTO {}",
        asyncTask,
        initPeriod,
        lastPeriod,
        accountId);

    // Create Callable that contains the entire business logic
    return asyncTaskExecutor.submitTask(
        asyncTask,
        () -> {
          LOG.info(
              String.format(
                  "Starting async task for productDTO %s to persist balances from [%s- %s]",
                  accountId, initPeriod, lastPeriod));

          // Step 1: Save balances (executes first)
          saveMultiBalances(monthlyBalances);

          // Step 2: Return profit/opening balances (they contain the combined results)
          // The monthly balances are already persisted in the database
          final List<MonthlyBalanceDTO> updatedBalances =
              adjustCurrentAndNextMonthlyBalancesAsync(accountId, initPeriod, lastPeriod);

          LOG.info("Persisting monthly balances completed successfully {} ", asyncTask);

          // Step 3: Update Account Net Growth Rate when is fully withdrawl

          if (accountService.isFullyWithdrawn(accountId)) {
            accountService.updateWhenFullyWithdrawn(
                accountId, lifecycleSrv.findAllByAccountIdUntilNow(accountId));
          }

          return updatedBalances;
        });
  }

  @Override
  public MonthlyBalanceDTO syncForReversedMovement(final MovementDTO reversedMovement)
      throws BusinessException {
    final ProductId productId = reversedMovement.productId();
    final var accountMonthlyBalance = findMonthlyBalanceOfMovement(reversedMovement, productId);
    LOG.info(
        "Reversing monthly balance for productDTO {} and new movement date {}",
        productId,
        reversedMovement.movementDate());

    accountMonthlyBalance.reverseMovement(MovementMapper.toDomain(reversedMovement));
    final var syncedMonthlyBalanceDTO = toDTO(accountMonthlyBalance);
    persistBalancesAsync(productId, Collections.singletonList(syncedMonthlyBalanceDTO));

    return syncedMonthlyBalanceDTO;
  }

  @Override
  /**
   * Finds if the monthly balance associated with the movement date was already reported officially.
   *
   * @param accountId
   * @param movementPeriod
   * @return true if the monthly balance was already reported, false otherwise
   */
  public boolean findIfMonthlyBalanceWasOfficialReported(
      final ProductId accountId, final YearMonth movementPeriod) {
    final Optional<MonthlyBalanceDTO> existingMonthlyBalanceOpt =
        lifecycleSrv.findByAccountIdAndPeriod(accountId, movementPeriod);
    return existingMonthlyBalanceOpt.map(MonthlyBalanceDTO::officialMonthlyReport).orElse(false);
  }

  @Override
  public Optional<MonthlyBalanceDTO> findByAccountIdAndPeriod(
      final ProductId productId, final YearMonth movementPeriod) {
    return lifecycleSrv.findByAccountIdAndPeriod(productId, movementPeriod);
  }
}
