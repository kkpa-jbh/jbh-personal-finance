package com.jbh.account.application.accounts.services;

import static com.jbh.account.domain.utils.MoneyUtils.JBH_ZERO;

import com.jbh.account.application.accounts.ports.output.AccountMonthlyBalanceRepository;
import com.jbh.account.application.common.logging.LoggerFactory;
import com.jbh.account.domain.entity.AccountMonthlyBalanceDomain;
import com.jbh.account.domain.entity.AccountMovementDomain;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountMonthlyBalanceDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;

public class MonthlyBalanceSyncerAppService {

  private static final Logger LOG = LoggerFactory.getLogger(MonthlyBalanceSyncerAppService.class);

  private final AccountMonthlyBalanceRepository accountMonthlyBalanceRepo;

  public MonthlyBalanceSyncerAppService(
      final AccountMonthlyBalanceRepository accountMonthlyBalanceRepo) {
    this.accountMonthlyBalanceRepo = accountMonthlyBalanceRepo;
  }

  public AccountMonthlyBalanceDomain syncMonthlyBalance(final AccountMovementDomain newMovement) {
    // Implementation for syncing monthly balances
    final AccountId accountId = newMovement.getAccountId();
    LOG.info("Syncing monthly balance  {}", newMovement.getAccountId());
    final LocalDate movementDate = newMovement.getMovementDate();
    final int txnYear = movementDate.getYear();
    final int txnMonth = movementDate.getMonthValue();
    final Optional<AccountMonthlyBalanceDTO> accountMonthlyBalanceOpt =
        accountMonthlyBalanceRepo.findByAccountIdYearAndMonth(accountId, txnYear, txnMonth);
    final AccountMonthlyBalanceDTO accountMonthlyBalance =
        accountMonthlyBalanceOpt.orElseGet(
            () -> AccountMonthlyBalanceDomain.of(accountId, txnYear, txnMonth).toDTO());

    accountMonthlyBalance.syncMovement(newMovement);

    saveMonthlyBalancesASYNC(accountId, Collections.singletonList(accountMonthlyBalance));

    return accountMonthlyBalance;
  }

  public CompletableFuture<List<AccountMonthlyBalanceDTO>> saveMonthlyBalancesASYNC(
      final AccountId accountId, final List<AccountMonthlyBalanceDTO> monthlyBalances) {

    if (monthlyBalances == null || monthlyBalances.isEmpty()) {
      LOG.warn("No monthly balances available for saving them ASYNC");
      return CompletableFuture.completedFuture(Collections.emptyList());
    }

    final YearMonth initPeriod = monthlyBalances.getFirst().getPeriod();
    final YearMonth lastPeriod = monthlyBalances.getLast().getPeriod();
    LOG.info(
        String.format(
            "Starting virtual thread to sync balances from %s to %s", initPeriod, lastPeriod));

    // Create a CompletableFuture that will be completed by a virtual thread
    final CompletableFuture<List<AccountMonthlyBalanceDTO>> future = new CompletableFuture<>();

    try (ExecutorService executorService = Executors.newFixedThreadPool(1)) {
      // Start a virtual thread to do the work
      executorService.submit(
          () -> {
            final String threadName = Thread.currentThread().getName();
            LOG.info("Starting balance processing on virtual thread: " + threadName);

            // Step 1: Save balances (executes first)
            final List<AccountMonthlyBalanceDTO> savedBalances =
                saveBalancesTask(monthlyBalances).get();

            LOG.info("Balances saved on thread: " + Thread.currentThread().getName());

            // Step 2: Sync profit data (executes IMMEDIATELY after step 1 completes)
            final List<AccountMonthlyBalanceDTO> profitBalances =
                syncProfitMonthlyTask(accountId, initPeriod, lastPeriod).get();

            LOG.info("Profit sync completed on thread: " + Thread.currentThread().getName());

            // Step 3: Combine results
            final List<AccountMonthlyBalanceDTO> combined =
                combineResults().apply(savedBalances, profitBalances);

            LOG.info("Task completed successfully, virtual thread will be garbage collected");

            // Complete the future with success
            future.complete(combined);
          });
    }

    return future;
  }

  private Supplier<List<AccountMonthlyBalanceDTO>> saveBalancesTask(
      final List<AccountMonthlyBalanceDTO> monthlyBalance) {
    return () -> save(monthlyBalance);
  }

  private Supplier<List<AccountMonthlyBalanceDTO>> syncProfitMonthlyTask(
      final AccountId accountId, final YearMonth fromPeriod, final YearMonth toPeriod) {
    return () -> syncProfitMonthlyFromPeriod(accountId, fromPeriod, toPeriod);
  }

  private BiFunction<
          List<AccountMonthlyBalanceDTO>,
          List<AccountMonthlyBalanceDTO>,
          List<AccountMonthlyBalanceDTO>>
      combineResults() {
    return (savedBalances, openingBalancesSynced) -> openingBalancesSynced;
  }

  private List<AccountMonthlyBalanceDTO> save(
      final List<AccountMonthlyBalanceDTO> monthlyBalanceToPersist) {
    LOG.info("[REPO] Saving Monthly Balances {}", monthlyBalanceToPersist.size());
    return accountMonthlyBalanceRepo.save(
        monthlyBalanceToPersist.stream().map(AccountMonthlyBalanceDomain::toDTO).toList());
  }

  private List<AccountMonthlyBalanceDTO> syncProfitMonthlyFromPeriod(
      final AccountId accountId, final YearMonth initPeriod, final YearMonth endPeriod) {

    LOG.info(
        "Adjusting Opening/Profit Balances for account {}" + " from period {} to period {}",
        accountId.value(),
        initPeriod,
        endPeriod);

    final List<AccountMonthlyBalanceDomain> profitBalancesSynced = new ArrayList<>();
    final List<AccountMonthlyBalanceDTO> existingBalancesFromPeriod =
        accountMonthlyBalanceRepo.findNextBalancesFromPeriodInclusive(accountId, initPeriod);

    if (existingBalancesFromPeriod == null || existingBalancesFromPeriod.isEmpty()) {
      LOG.warn(
          "No future balances to sync profit were found for account {} and period {}",
          accountId,
          initPeriod);
      return Collections.emptyList();
    }

    final ConcurrentMap<YearMonth, AccountMonthlyBalanceDomain> existingBalancesMap =
        existingBalancesFromPeriod.stream()
            .collect(
                Collectors.toConcurrentMap(
                    AccountMonthlyBalanceDomain::getPeriod, Function.identity()));

    YearMonth currentPeriod = initPeriod;
    while (isAvailablePeriod(currentPeriod, endPeriod)) {

      // LOG.debug("Syncing Monthly Profit for period: {}", currentPeriod);
      final AccountMonthlyBalanceDomain currentMonthlyBalance =
          existingBalancesMap.get(currentPeriod);
      currentMonthlyBalance.refreshProfitMonthly();

      final YearMonth nextPeriod = currentMonthlyBalance.getPeriod().plusMonths(1);
      // LOG.debug("Syncing Opening Balance for next period: {} ", nextPeriod);
      // Syncing Next month opening balance with current month closing balance
      AccountMonthlyBalanceDomain nextMonthlyBalanceOfCurrent =
          existingBalancesFromPeriod.stream()
              .filter(mb -> mb.getPeriod().equals(nextPeriod))
              .findFirst()
              .orElse(null);
      // If not found in database, let's create new one with the same balance snapshot from previous
      // month.
      if (nextMonthlyBalanceOfCurrent == null) {
        final boolean isEndPeriod = currentPeriod.equals(endPeriod);
        // If it is the last period, just set the opening balance (Next Future Month)
        final BigDecimal closingBalance =
            isEndPeriod ? JBH_ZERO : currentMonthlyBalance.getClosingBalance();
        nextMonthlyBalanceOfCurrent =
            AccountMonthlyBalanceDomain.of(accountId, nextPeriod, closingBalance, !isEndPeriod);
        existingBalancesMap.putIfAbsent(nextPeriod, nextMonthlyBalanceOfCurrent);
      }
      nextMonthlyBalanceOfCurrent.setOpeningBalance(currentMonthlyBalance.getClosingBalance());

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
    Collections.sort(profitBalancesSynced);

    // persist monthly balances with profit and opening balances synced.
    final List<AccountMonthlyBalanceDTO> profitBalancesSyncedDto =
        profitBalancesSynced.stream().map(AccountMonthlyBalanceDomain::toDTO).toList();
    save(profitBalancesSyncedDto);

    return profitBalancesSyncedDto;
  }

  private boolean isAvailablePeriod(final YearMonth currentPeriod, final YearMonth endPeriod) {
    return currentPeriod.isBefore(getEdgePeriod())
        && currentPeriod.isBefore(endPeriod.plusMonths(1));
  }

  private YearMonth getEdgePeriod() {
    return YearMonth.now().plusMonths(1);
  }

  public List<AccountMonthlyBalanceDomain> syncMonthlyBalance(
      final AccountId accountId, final List<AccountMovementDomain> multipleMovementsDomain) {
    // Group movements by Year-Month based on the movementDate attribute
    final Map<YearMonth, List<AccountMovementDomain>> movementsByYearMonth =
        multipleMovementsDomain.stream()
            .collect(
                Collectors.groupingBy(
                    movement -> {
                      final int year = movement.getMovementDate().getYear();
                      final int month = movement.getMovementDate().getMonthValue();
                      return YearMonth.of(year, month);
                    }));
    final Stream<YearMonth> movementsPeriodsSorted =
        movementsByYearMonth.keySet().stream().sorted();

    final List<AccountMonthlyBalanceDomain> monthlyBalancesToPersist = new ArrayList<>();
    movementsPeriodsSorted.forEach(
        monthlyPeriodKey -> {
          final List<AccountMovementDomain> movementsInPeriod =
              movementsByYearMonth.get(monthlyPeriodKey);
          final int year = monthlyPeriodKey.getYear();
          final int month = monthlyPeriodKey.getMonthValue();

          LOG.debug(
              "Processing {} movements for period {}-{}", movementsInPeriod.size(), year, month);

          final AccountMonthlyBalanceDomain accountMonthlyBalance =
              accountMonthlyBalanceRepo
                  .findByAccountIdYearAndMonth(accountId, year, month)
                  .orElseGet(() -> AccountMonthlyBalanceDomain.of(accountId, year, month).toDTO());

          accountMonthlyBalance.syncMovements(movementsInPeriod);
          LOG.debug("Monthly balance updated for {}-{}", year, month);
          monthlyBalancesToPersist.add(accountMonthlyBalance);
        });

    saveMonthlyBalancesASYNC(
        accountId,
        monthlyBalancesToPersist.stream().map(AccountMonthlyBalanceDomain::toDTO).toList());

    return monthlyBalancesToPersist;
  }
}
