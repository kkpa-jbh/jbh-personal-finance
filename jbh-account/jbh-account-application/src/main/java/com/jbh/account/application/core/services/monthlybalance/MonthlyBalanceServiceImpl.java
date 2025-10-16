package com.jbh.account.application.core.services.monthlybalance;

import static com.jbh.account.application.core.mappers.MonthlyBalanceMapper.toDTO;
import static com.jbh.account.application.core.mappers.MonthlyBalanceMapper.toDomain;
import static com.jbh.account.domain.utils.MoneyUtils.JBH_ZERO;

import com.jbh.account.application.async.AsyncTaskExecutor;
import com.jbh.account.application.async.vo.AsyncTask;
import com.jbh.account.application.async.vo.AsyncTaskType;
import com.jbh.account.application.core.comparator.AccountMonthlyBalanceComparators;
import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.application.core.dto.MovementDTO;
import com.jbh.account.application.core.mappers.MonthlyBalanceMapper;
import com.jbh.account.application.core.mappers.MovementMapper;
import com.jbh.account.application.core.ports.output.monthlybalance.AccountMonthlyBalanceQueryRepo;
import com.jbh.account.application.core.ports.output.monthlybalance.AccountMonthlyBalanceWriterRepository;
import com.jbh.account.application.core.validation.movement.MovementTypeValidationStrategy;
import com.jbh.account.application.core.validation.movement.MovementValidationStrategyFactory;
import com.jbh.account.application.core.vo.commands.AddMonthlyBalanceCommand;
import com.jbh.account.application.exceptions.JbhExceptionMessage;
import com.jbh.account.application.exceptions.JbhSpecificationApplication;
import com.jbh.account.domain.entity.AccountMonthlyBalanceDomain;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.MovementType;
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
public class MonthlyBalanceServiceImpl implements MonthlyBalanceService {
  private static final Logger LOG = LoggerFactory.getLogger(MonthlyBalanceServiceImpl.class);
  private final AccountMonthlyBalanceQueryRepo queryRepo;
  private final AccountMonthlyBalanceWriterRepository writerRepo;
  private final AsyncTaskExecutor asyncTaskExecutor;

  private final MovementValidationStrategyFactory movValidationStrategyFactory;

  public MonthlyBalanceServiceImpl(
      final AccountMonthlyBalanceQueryRepo monthlyBalanceRepo,
      final AccountMonthlyBalanceWriterRepository monthlyBalanceWriterRepo,
      final AsyncTaskExecutor asyncTaskExecutor) {
    this.writerRepo = monthlyBalanceWriterRepo;
    this.queryRepo = monthlyBalanceRepo;
    this.asyncTaskExecutor = asyncTaskExecutor;

    this.movValidationStrategyFactory = new MovementValidationStrategyFactory();
  }

  @Override
  public Optional<MonthlyBalanceDTO> findByAccountIdYearAndMonth(
      final AccountId accountId, final Integer balanceYear, final Integer balanceMonth) {
    return queryRepo.findByAccountIdYearAndMonth(accountId, balanceYear, balanceMonth);
  }

  @Override
  public Optional<MonthlyBalanceDTO> findByAccountIdAndPeriod(
      final AccountId accountId, final YearMonth period) {
    return queryRepo.findByAccountIdAndPeriod(accountId, period);
  }

  @Override
  public List<MonthlyBalanceDTO> findNextBalancesFromPeriodInclusive(
      final AccountId accountId, final YearMonth currentPeriod) {
    return queryRepo.findNextBalancesFromPeriodInclusive(accountId, currentPeriod);
  }

  @Override
  public Optional<MonthlyBalanceDTO> findLastOfficialReport(final AccountId accountId) {
    return queryRepo.findLastOfficialReport(accountId);
  }

  @Override
  public BigDecimal sumNetProfitOfficialReported(final AccountId accountId) {
    return queryRepo.sumNetProfitOfficialReported(accountId);
  }

  @Override
  public void updateOpeningBalanceNextMonth(final MonthlyBalanceDTO currentMonthlyBalance) {
    final YearMonth nextPeriod = currentMonthlyBalance.period().plusMonths(1);

    final AccountMonthlyBalanceDomain nextMonthlyBalance =
        queryRepo
            .findByAccountIdYearAndMonth(
                currentMonthlyBalance.accountId(), nextPeriod.getYear(), nextPeriod.getMonthValue())
            .map(MonthlyBalanceMapper::toDomain)
            .orElseGet(
                () ->
                    AccountMonthlyBalanceDomain.withPeriod(
                        currentMonthlyBalance.accountId(), nextPeriod));

    nextMonthlyBalance.assignOpeningBalance(toDomain(currentMonthlyBalance));

    final MonthlyBalanceDTO nextMonthlyBalanceDTO = toDTO(nextMonthlyBalance);
    saveBalance(nextMonthlyBalanceDTO);
  }

  @Override
  public boolean isLastOfficialReport(final MonthlyBalanceDTO monthlyBalanceDTO) {
    final Optional<MonthlyBalanceDTO> latestOfficialMonthlyReport =
        findLastOfficialReport(monthlyBalanceDTO.accountId());
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
  public MonthlyBalanceDTO syncForNewMovement(final MovementDTO newMovement) {
    // Implementation for syncing monthly balances
    final AccountId accountId = newMovement.accountId();
    LOG.info(
        "Syncing monthly balance for account {} and movement date {}",
        accountId.value(),
        newMovement.movementDate());
    final LocalDate movementDate = newMovement.movementDate();
    final YearMonth movementPeriod = YearMonth.from(movementDate);
    final AccountMonthlyBalanceDomain accountMonthlyBalance =
        findByAccountIdAndPeriod(accountId, movementPeriod)
            .map(MonthlyBalanceMapper::toDomain)
            .orElseGet(() -> AccountMonthlyBalanceDomain.withPeriod(accountId, movementPeriod));

    accountMonthlyBalance.assignMovement(MovementMapper.toDomain(newMovement));
    final var syncedMonthlyBalanceDTO = toDTO(accountMonthlyBalance);
    persistBalancesAsync(accountId, Collections.singletonList(syncedMonthlyBalanceDTO));

    return syncedMonthlyBalanceDTO;
  }

  @Override
  public CompletableFuture<List<MonthlyBalanceDTO>> persistBalancesAsync(
      final AccountId accountId, final List<MonthlyBalanceDTO> monthlyBalances) {

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
                "accountId", accountId.value().toString(),
                "initPeriod", initPeriod.toString(),
                "lastPeriod", lastPeriod.toString(),
                "balancesCount", monthlyBalances.size()));

    final AsyncTask asyncTask = new AsyncTask(AsyncTaskType.MONTHLY_BALANCES_SYNC, metadata);

    LOG.info(
        "Preparing to submit async task to sync balances from {} to {} for account {}",
        initPeriod,
        lastPeriod,
        accountId.value());

    // Create Callable that contains the entire business logic
    return asyncTaskExecutor.submitTask(
        asyncTask,
        () -> {
          LOG.info(
              String.format(
                  "Starting async task to sync balances from %s to %s", initPeriod, lastPeriod));

          // Step 1: Save balances (executes first)
          saveMultiBalances(monthlyBalances);

          // Step 3: Return profit/opening balances (they contain the combined results)
          // The monthly balances are already persisted in the database
          final List<MonthlyBalanceDTO> updatedBalances =
              adjustCurrentAndNextMonthlyBalancesAsync(accountId, initPeriod, lastPeriod);

          LOG.info("Syncing monthly balances completed successfully {} ", updatedBalances.size());

          return updatedBalances;
        });
  }

  @Override
  public void validateNewMovement(final MovementDTO movementDTO)
      throws JbhSpecificationApplication {
    final YearMonth movementPeriod = YearMonth.from(movementDTO.movementDate());
    final AccountId accountId = movementDTO.accountId();
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
        final MovementTypeValidationStrategy strategy =
            movValidationStrategyFactory.getStrategy(movementType);
        strategy.validateMovementAgainstOfficialBalance(movementAmount, existingMonthlyBalance);
      }
    }
  }

  @Override
  public MonthlyBalanceDTO updateOfficialReportedBalance(
      final MonthlyBalanceDTO reportedMonthlyBalance, final AddMonthlyBalanceCommand command) {

    final AccountMonthlyBalanceDomain monthlyBalanceDomain = toDomain(reportedMonthlyBalance);
    final var updatedMonthlyBalance = assignOfficialReport(monthlyBalanceDomain, command);

    saveBalance(updatedMonthlyBalance);
    updateOpeningBalanceNextMonth(updatedMonthlyBalance);

    return updatedMonthlyBalance;
  }

  private MonthlyBalanceDTO assignOfficialReport(
      final AccountMonthlyBalanceDomain monthlyBalanceDomain,
      final AddMonthlyBalanceCommand command) {
    monthlyBalanceDomain.assignOfficialMonthlyReport(
        command.closingBalance(),
        command.monthlyProfitReported(),
        command.incomeWithholdingTaxAmount());
    return toDTO(monthlyBalanceDomain);
  }

  private static boolean isMonthOfficiallyReportedValid(
      final MonthlyBalanceDTO existingMonthlyBalance, final BigDecimal balanceSnapshot)
      throws JbhSpecificationApplication {
    final boolean isMonthOfficiallyReported = existingMonthlyBalance.officialMonthlyReport();
    if (isMonthOfficiallyReported && balanceSnapshot != null) {
      throw new JbhSpecificationApplication(
          "Cannot add a snapshot after the monthly balance was officially reported",
          new JbhExceptionMessage(
              "Cannot add a snapshot after the monthly balance was officially reported",
              "No se puede añadir un snapshot después de que el balance anual fue reportado"));
    }
    return isMonthOfficiallyReported;
  }

  /**
   * Syncs the current and next monthly balances for the given periods. THe monthly balances are
   * already persisted in the database.
   */
  private List<MonthlyBalanceDTO> adjustCurrentAndNextMonthlyBalancesAsync(
      final AccountId accountId, final YearMonth initPeriod, final YearMonth endPeriod) {

    LOG.info(
        "Monthly balances {} to {} should be already persisted in the database",
        initPeriod,
        endPeriod);

    LOG.info(
        "Adjusting Opening/Profit Balances for account {}" + " from period {} to period {}",
        accountId.value(),
        initPeriod,
        endPeriod);

    final List<MonthlyBalanceDTO> existingNextPeriodBalanceDTO =
        findNextBalancesFromPeriodInclusive(accountId, initPeriod);

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
    // FIXME: Using now() is not a good idea
    final YearMonth now = YearMonth.now();
    LOG.info("Syncing current and next monthly balances {} - {} ", currentPeriod, endPeriod);
    while (isAvailablePeriod(now, currentPeriod, endPeriod)) {

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
    profitBalancesSynced.sort(AccountMonthlyBalanceComparators.BY_PERIOD_ASC);

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
}
