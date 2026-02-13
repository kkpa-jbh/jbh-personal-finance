package com.jbh.finance.application.feature.monthlybalance.services;

import static com.jbh.commons.util.JbhMoneyUtils.JBH_ZERO;
import static com.jbh.finance.application.feature.monthlybalance.mappers.MonthlyBalanceMapper.toDTO;
import static com.jbh.finance.application.feature.monthlybalance.mappers.MonthlyBalanceMapper.toDomain;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.async.AsyncTaskExecutor;
import com.jbh.finance.application.async.vo.AsyncTask;
import com.jbh.finance.application.async.vo.AsyncTaskType;
import com.jbh.finance.application.feature.monthlybalance.commands.AddMonthlyBalanceCommand;
import com.jbh.finance.application.feature.monthlybalance.comparator.MonthlyBalanceComparators;
import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.finance.application.feature.monthlybalance.mappers.MonthlyBalanceMapper;
import com.jbh.finance.application.feature.monthlybalance.ports.output.MonthlyBalanceQueryRepo;
import com.jbh.finance.application.feature.monthlybalance.ports.output.MonthlyBalanceWriterRepo;
import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.application.feature.movement.mappers.MovementMapper;
import com.jbh.finance.application.feature.movement.validation.movement_type.MovementTypeValidatorStrategy;
import com.jbh.finance.application.feature.movement.validation.movement_type.MovementValidationStrategyFactory;
import com.jbh.finance.application.feature.product.services.ProductLifecycleService;
import com.jbh.finance.application.shared.exceptions.BusinessApplicationExceptionType;
import com.jbh.finance.domain.monthlybalance.MonthlyBalanceDomain;
import com.jbh.finance.domain.movement.vo.MovementType;
import com.jbh.finance.domain.product.vo.ProductId;
import com.jbh.finance.domain.product.vo.ProductPK;
import com.jbh.finance.domain.shared.vo.PeriodRange;
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
public class MonthlyBalanceLifecycleServiceImpl implements MonthlyBalanceLifecycleService {
  private static final Logger LOG =
      LoggerFactory.getLogger(MonthlyBalanceLifecycleServiceImpl.class);
  private final MonthlyBalanceQueryRepo queryRepo;
  private final MonthlyBalanceWriterRepo writerRepo;
  private final AsyncTaskExecutor asyncTaskExecutor;
  private final ProductLifecycleService accountService;
  private final MovementValidationStrategyFactory movValidationStrategyFactory;

  public MonthlyBalanceLifecycleServiceImpl(
      final MonthlyBalanceQueryRepo monthlyBalanceRepo,
      final MonthlyBalanceWriterRepo monthlyBalanceWriterRepo,
      final AsyncTaskExecutor asyncTaskExecutor,
      final ProductLifecycleService accountService) {
    this.writerRepo = monthlyBalanceWriterRepo;
    this.queryRepo = monthlyBalanceRepo;
    this.asyncTaskExecutor = asyncTaskExecutor;
    this.accountService = accountService;
    this.movValidationStrategyFactory = new MovementValidationStrategyFactory();
  }

  @Override
  public List<MonthlyBalanceDTO> findByAccountAndPeriods(
      final ProductPK accountPK, final YearMonth startPeriod, final YearMonth endPeriod) {
    return queryRepo.findByAccountAndPeriods(accountPK, startPeriod, endPeriod);
  }

  @Override
  public Optional<MonthlyBalanceDTO> findByAccountIdYearAndMonth(
      final ProductId accountId, final Integer balanceYear, final Integer balanceMonth) {
    return queryRepo.findByAccountIdYearAndMonth(accountId, balanceYear, balanceMonth);
  }

  @Override
  public Optional<MonthlyBalanceDTO> findByAccountIdAndPeriod(
      final ProductId accountId, final YearMonth period) {
    return queryRepo.findByAccountIdAndPeriod(accountId, period);
  }

  @Override
  public Optional<MonthlyBalanceDTO> findLastOfficialReport(final ProductId accountId) {
    return queryRepo.findLastOfficialReport(accountId);
  }

  @Override
  public BigDecimal sumNetProfitOfficialReported(final ProductId accountId) {
    return queryRepo.sumNetProfitOfficialReported(accountId);
  }

  @Override
  public List<MonthlyBalanceDTO> findNextBalancesFromPeriodInclusive(
      final ProductId accountId, final YearMonth currentPeriod) {
    return queryRepo.findNextBalancesFromPeriodInclusive(accountId, currentPeriod);
  }

  @Override
  public List<MonthlyBalanceDTO> findAllByAccountIdUntilNow(final ProductId accountId) {
    // TODO: Check if it's necessary to return until the current period
    return queryRepo.findAllByAccountIdUntilNow(accountId);
  }

  @Override
  public List<MonthlyBalanceDTO> findByProductIdsAndPeriods(
      final List<ProductId> productIds, final PeriodRange periodRange) {
    return queryRepo.findByProductIdsAndPeriods(productIds, periodRange);
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

  private static boolean isMonthOfficiallyReportedValid(
      final MonthlyBalanceDTO existingMonthlyBalance, final BigDecimal balanceSnapshot)
      throws BusinessException {
    final boolean isMonthOfficiallyReported = existingMonthlyBalance.officialMonthlyReport();
    if (isMonthOfficiallyReported && balanceSnapshot != null) {
      throw new BusinessException(BusinessApplicationExceptionType.SNAPSHOT_AFTER_OFFICIAL_REPORT);
    }
    return isMonthOfficiallyReported;
  }

  @Override
  public MonthlyBalanceDTO updateOfficialReportedBalance(
      final MonthlyBalanceDTO reportedMonthlyBalance, final AddMonthlyBalanceCommand command)
      throws BusinessException {

    final MonthlyBalanceDomain monthlyBalanceDomain = toDomain(reportedMonthlyBalance);
    final var updatedMonthlyBalance = assignOfficialReport(monthlyBalanceDomain, command);

    saveBalance(updatedMonthlyBalance);
    updateOpeningBalanceNextMonth(updatedMonthlyBalance);

    return updatedMonthlyBalance;
  }

  private MonthlyBalanceDTO assignOfficialReport(
      final MonthlyBalanceDomain monthlyBalanceDomain, final AddMonthlyBalanceCommand command)
      throws BusinessException {
    monthlyBalanceDomain.assignOfficialMonthlyReport(
        command.closingBalance(),
        command.monthlyProfitReported(),
        command.incomeWithholdingTaxAmount());
    return toDTO(monthlyBalanceDomain);
  }

  @Override
  public void saveBalance(final MonthlyBalanceDTO accountMonthlyBalance) {
    writerRepo.saveBalance(accountMonthlyBalance);
  }

  @Override
  public List<MonthlyBalanceDTO> saveMultiBalances(
      final List<MonthlyBalanceDTO> accountMonthlyBalance) {
    LOG.info(
        "Persisting in database Monthly Balances {}",
        accountMonthlyBalance.stream().map(MonthlyBalanceDTO::period).toList());
    final List<MonthlyBalanceDTO> savedBalances =
        writerRepo.saveMultiBalances(accountMonthlyBalance);
    LOG.info("Monthly Balances persisted successfully");
    return savedBalances;
  }

  @Override
  public void updateOpeningBalanceNextMonth(final MonthlyBalanceDTO currentMonthlyBalance) {
    final YearMonth nextPeriod = currentMonthlyBalance.period().plusMonths(1);

    final MonthlyBalanceDomain nextMonthlyBalance =
        queryRepo
            .findByAccountIdYearAndMonth(
                currentMonthlyBalance.productId(), nextPeriod.getYear(), nextPeriod.getMonthValue())
            .map(MonthlyBalanceMapper::toDomain)
            .orElseGet(
                () ->
                    MonthlyBalanceDomain.withPeriod(currentMonthlyBalance.productId(), nextPeriod));

    nextMonthlyBalance.assignOpeningBalance(toDomain(currentMonthlyBalance));

    final MonthlyBalanceDTO nextMonthlyBalanceDTO = toDTO(nextMonthlyBalance);
    saveBalance(nextMonthlyBalanceDTO);
  }

  @Override
  public boolean isLastOfficialReport(final MonthlyBalanceDTO monthlyBalanceDTO) {
    final Optional<MonthlyBalanceDTO> latestOfficialMonthlyReport =
        findLastOfficialReport(monthlyBalanceDTO.productId());
    final YearMonth currentPeriod = monthlyBalanceDTO.period();

    final boolean isLastOfficialReport =
        latestOfficialMonthlyReport
            .map(
                accountMonthlyBalanceDTO -> accountMonthlyBalanceDTO.period().equals(currentPeriod))
            .orElse(false);

    LOG.info("Its period {} the last official report: {}", currentPeriod, isLastOfficialReport);
    return isLastOfficialReport;
  }

  @Override
  public MonthlyBalanceDTO syncForNewMovement(final MovementDTO newMovement)
      throws BusinessException {
    // Implementation for syncing monthly balances
    final ProductId accountId = newMovement.productId();
    LOG.info(
        "Syncing monthly balance for productDTO {} and new movement date {}",
        accountId,
        newMovement.movementDate());
    final LocalDate movementDate = newMovement.movementDate();
    final YearMonth movementPeriod = YearMonth.from(movementDate);
    final MonthlyBalanceDomain accountMonthlyBalance =
        findByAccountIdAndPeriod(accountId, movementPeriod)
            .map(MonthlyBalanceMapper::toDomain)
            .orElseGet(() -> MonthlyBalanceDomain.withPeriod(accountId, movementPeriod));

    accountMonthlyBalance.assignMovement(MovementMapper.toDomain(newMovement));
    final var syncedMonthlyBalanceDTO = toDTO(accountMonthlyBalance);
    persistBalancesAsync(accountId, Collections.singletonList(syncedMonthlyBalanceDTO));

    return syncedMonthlyBalanceDTO;
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
                accountId, findAllByAccountIdUntilNow(accountId));
          }

          return updatedBalances;
        });
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
        findNextBalancesFromPeriodInclusive(accountId, initPeriod);

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
    return currentPeriod.isBefore(getEdgePeriod(now))
        && currentPeriod.isBefore(endPeriod.plusMonths(1));
  }

  private YearMonth getEdgePeriod(final YearMonth now) {
    return now.plusMonths(1);
  }
}
