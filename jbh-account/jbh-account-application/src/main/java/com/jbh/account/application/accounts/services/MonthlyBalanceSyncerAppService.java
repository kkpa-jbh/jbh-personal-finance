package com.jbh.account.application.accounts.services;

import static com.jbh.accounts_mgmt.utils.MoneyUtils.JBH_ZERO;

import com.jbh.account.application.accounts.ports.output.AccountMonthlyBalanceRepository;
import com.jbh.account.application.common.logging.LoggerFactory;
import com.jbh.accounts_mgmt.accounts.AccountId;
import com.jbh.accounts_mgmt.accounts.AccountMonthlyBalanceDomain;
import com.jbh.accounts_mgmt.movements.AccountMovementDomain;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;

public class MonthlyBalanceSyncerAppService {

  private static final Logger LOG = LoggerFactory.getLogger(MonthlyBalanceSyncerAppService.class);

  private final AccountMonthlyBalanceRepository accountMonthlyBalanceRepo;

  public MonthlyBalanceSyncerAppService(AccountMonthlyBalanceRepository accountMonthlyBalanceRepo) {
    this.accountMonthlyBalanceRepo = accountMonthlyBalanceRepo;
  }

  public AccountMonthlyBalanceDomain syncMonthlyBalance(AccountMovementDomain newMovement) {
    // Implementation for syncing monthly balances
    AccountId accountId = newMovement.getAccountId();
    LOG.info("Syncing monthly balance  {}", newMovement.getAccountId());
    int txnYear = newMovement.getMovementDate().getYear();
    int txnMonth = newMovement.getMovementDate().getMonthValue();
    Optional<AccountMonthlyBalanceDomain> accountMonthlyBalanceOpt = accountMonthlyBalanceRepo.findByAccountIdYearAndMonth(
        accountId, txnYear, txnMonth);
    AccountMonthlyBalanceDomain accountMonthlyBalance = accountMonthlyBalanceOpt.orElseGet(
        () -> AccountMonthlyBalanceDomain.of(accountId, txnYear, txnMonth));

    accountMonthlyBalance.syncMovement(newMovement);

    saveMonthlyBalancesASYNC(accountId,
        Collections.singletonList(accountMonthlyBalance));

    return accountMonthlyBalance;
  }

  public List<AccountMonthlyBalanceDomain> syncMonthlyBalance(AccountId accountId,
      List<AccountMovementDomain> multipleMovementsDomain) {
    // Group movements by Year-Month based on the movementDate attribute
    var movementsByYearMonth = multipleMovementsDomain.stream().collect(
        java.util.stream.Collectors.groupingBy(movement -> {
          int year = movement.getMovementDate().getYear();
          int month = movement.getMovementDate().getMonthValue();
          return YearMonth.of(year, month);
        }));
    Stream<YearMonth> movementsPeriodsSorted = movementsByYearMonth.keySet().stream().sorted();

    List<AccountMonthlyBalanceDomain> monthlyBalancesToPersist = new ArrayList<>();
    movementsPeriodsSorted.forEach(monthlyPeriodKey -> {
      List<AccountMovementDomain> movementsInPeriod = movementsByYearMonth.get(monthlyPeriodKey);
      int year = monthlyPeriodKey.getYear();
      int month = monthlyPeriodKey.getMonthValue();
      LOG.debug("Processing {} movements for period {}-{}", movementsInPeriod.size(), year, month);

      AccountMonthlyBalanceDomain accountMonthlyBalance = accountMonthlyBalanceRepo.findByAccountIdYearAndMonth(
          accountId, year, month).orElseGet(
          () -> AccountMonthlyBalanceDomain.of(accountId, year, month));

      accountMonthlyBalance.syncMovements(movementsInPeriod);
      LOG.debug("Monthly balance updated for {}-{}", year, month);
      monthlyBalancesToPersist.add(accountMonthlyBalance);
    });

    saveMonthlyBalancesASYNC(accountId, monthlyBalancesToPersist);

    return monthlyBalancesToPersist;
  }

  private boolean isAvailablePeriod(YearMonth currentPeriod, YearMonth endPeriod) {
    return currentPeriod.isBefore(getEdgePeriod()) && currentPeriod.isBefore(endPeriod.plusMonths(1));
  }

  private List<AccountMonthlyBalanceDomain> syncProfitMonthlyFromPeriod(AccountId accountId,
      YearMonth initPeriod, YearMonth endPeriod) {

    LOG.info("Adjusting Opening/Profit Balances for account {}"
        + " from period {} to period {}", accountId.value(), initPeriod, endPeriod);

    List<AccountMonthlyBalanceDomain> profitBalancesSynced = new ArrayList<>();
    List<AccountMonthlyBalanceDomain> existingBalancesFromPeriod = accountMonthlyBalanceRepo.findNextBalancesFromPeriodInclusive(
        accountId, initPeriod);

    if (existingBalancesFromPeriod == null || existingBalancesFromPeriod.isEmpty()) {
      LOG.warn("No future balances to sync profit were found for account {} and period {}", accountId, initPeriod);
      return Collections.emptyList();
    }

    ConcurrentMap<YearMonth, AccountMonthlyBalanceDomain> existingBalancesMap = existingBalancesFromPeriod
        .stream()
        .collect(Collectors.toConcurrentMap(AccountMonthlyBalanceDomain::getPeriod, Function.identity()));

    YearMonth currentPeriod = initPeriod;
    while (isAvailablePeriod(currentPeriod, endPeriod)) {

      LOG.info("Syncing Monthly Profit for period: {}", currentPeriod);
      AccountMonthlyBalanceDomain currentMonthlyBalance = existingBalancesMap.get(currentPeriod);
      currentMonthlyBalance.refreshProfitMonthly();

      YearMonth nextPeriod = currentMonthlyBalance.getPeriod().plusMonths(1);
      LOG.info("Syncing Opening Balance for next period: {} ", nextPeriod);
      // Syncing Next month opening balance with current month closing balance
      AccountMonthlyBalanceDomain nextMonthlyBalanceOfCurrent = existingBalancesFromPeriod.stream()
          .filter(mb -> mb.getPeriod().equals(nextPeriod))
          .findFirst()
          .orElse(null);
      // If not found in database, let's create new one with the same balance snapshot from previous month.
      if (nextMonthlyBalanceOfCurrent == null) {
        boolean isEndPeriod = currentPeriod.equals(endPeriod);
        // If it is the last period, just set the opening balance (Next Future Month)
        BigDecimal closingBalance = isEndPeriod ? JBH_ZERO : currentMonthlyBalance.getClosingBalance();
        nextMonthlyBalanceOfCurrent = AccountMonthlyBalanceDomain.of(
            accountId,
            nextPeriod,
            closingBalance,
            !isEndPeriod);
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
    save(profitBalancesSynced);

    return profitBalancesSynced;

  }

  private YearMonth getEdgePeriod() {
    return YearMonth.now().plusMonths(1);
  }


  public CompletableFuture<List<AccountMonthlyBalanceDomain>> saveMonthlyBalancesASYNC(
      AccountId accountId,
      List<AccountMonthlyBalanceDomain> monthlyBalances) {

    LOG.info("Init process to save  monthly balances Asynchronously...");

    if (monthlyBalances == null || monthlyBalances.isEmpty()) {
      LOG.warn("No monthly balances available for saving them");
      return CompletableFuture.completedFuture(Collections.emptyList());
    }

    LOG.info("Initiating asynchronous save of monthly balances {}", monthlyBalances.size());

    CompletableFuture<List<AccountMonthlyBalanceDomain>> saveTask =
        CompletableFuture
            .supplyAsync(createSaveSupplier(monthlyBalances))
            .exceptionally(ex -> {
              LOG.error("Error saving balances: {}", ex.getMessage(), ex);
              return Collections.emptyList(); // fallback value
            });

    YearMonth initPeriod = monthlyBalances.getFirst().getPeriod();
    YearMonth lastPeriod = monthlyBalances.getLast().getPeriod();
    CompletableFuture<List<AccountMonthlyBalanceDomain>> openingTask =
        CompletableFuture
            .supplyAsync(syncProfitMonthlyTask(accountId, initPeriod, lastPeriod))
            .exceptionally(ex -> {
              LOG.error("Error saving opening balances: {}", ex.getMessage(), ex);
              return Collections.emptyList();
            });

    return saveTask.thenCombine(openingTask, combineResults());
  }


  private BiFunction<List<AccountMonthlyBalanceDomain>, List<AccountMonthlyBalanceDomain>, List<AccountMonthlyBalanceDomain>> combineResults() {
    return (savedBalances, openingBalancesSynced) -> openingBalancesSynced;
  }

  private List<AccountMonthlyBalanceDomain> save(List<AccountMonthlyBalanceDomain> monthlyBalanceToPersist) {
    LOG.info("Saving Monthly Balances {}", monthlyBalanceToPersist.size());
    return accountMonthlyBalanceRepo.save(monthlyBalanceToPersist);
  }

  private Supplier<List<AccountMonthlyBalanceDomain>> createSaveSupplier(
      List<AccountMonthlyBalanceDomain> monthlyBalance) {
    return () -> save(monthlyBalance);
  }

  private Supplier<List<AccountMonthlyBalanceDomain>> syncProfitMonthlyTask(
      AccountId accountId,
      YearMonth fromPeriod,
      YearMonth toPeriod) {
    return () -> syncProfitMonthlyFromPeriod(accountId, fromPeriod, toPeriod);
  }

}
