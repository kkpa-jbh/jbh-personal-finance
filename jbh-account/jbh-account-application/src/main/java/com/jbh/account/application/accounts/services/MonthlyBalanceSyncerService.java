package com.jbh.account.application.accounts.services;

import com.jbh.account.application.accounts.ports.output.AccountMonthlyBalanceRepository;
import com.jbh.account.application.common.logging.LoggerFactory;
import com.jbh.accounts_mgmt.accounts.AccountId;
import com.jbh.accounts_mgmt.accounts.AccountMonthlyBalanceDomain;
import com.jbh.accounts_mgmt.movements.AccountMovementDomain;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.slf4j.Logger;

public class MonthlyBalanceSyncerService {

  private static final Logger log = LoggerFactory.getLogger(MonthlyBalanceSyncerService.class);

  private final AccountMonthlyBalanceRepository accountMonthlyBalanceRepo;

  public MonthlyBalanceSyncerService(AccountMonthlyBalanceRepository accountMonthlyBalanceRepo) {
    this.accountMonthlyBalanceRepo = accountMonthlyBalanceRepo;
  }

  public AccountMonthlyBalanceDomain syncMonthlyBalance(AccountMovementDomain newMovement) {
    // Implementation for syncing monthly balances
    AccountId accountId = newMovement.getAccountId();
    log.info("Syncing monthly balance asynchronously {}", newMovement.getAccountId());
    int txnYear = newMovement.getMovementDate().getYear();
    int txnMonth = newMovement.getMovementDate().getMonthValue();
    Optional<AccountMonthlyBalanceDomain> accountMonthlyBalanceOpt = accountMonthlyBalanceRepo.findByAccountIdYearAndMonth(
        accountId, txnYear, txnMonth);
    AccountMonthlyBalanceDomain accountMonthlyBalance = accountMonthlyBalanceOpt.orElseGet(
        () -> AccountMonthlyBalanceDomain.of(accountId, txnYear, txnMonth));

    accountMonthlyBalance.syncMovement(newMovement);

    log.debug("Monthly balance updated for {}-{}", txnYear, txnMonth);

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
      log.debug("Processing {} movements for period {}-{}", movementsInPeriod.size(), year, month);

      AccountMonthlyBalanceDomain accountMonthlyBalance = accountMonthlyBalanceRepo.findByAccountIdYearAndMonth(
          accountId, year, month).orElseGet(
          () -> AccountMonthlyBalanceDomain.of(accountId, year, month));

      accountMonthlyBalance.syncMovements(movementsInPeriod);
      log.debug("Monthly balance updated for {}-{}", year, month);
      monthlyBalancesToPersist.add(accountMonthlyBalance);
    });

    saveMonthlyBalancesASYNC(accountId, monthlyBalancesToPersist);

    return monthlyBalancesToPersist;
  }

  private List<AccountMonthlyBalanceDomain> calculateProfitMonthly(AccountId accountId,
      List<AccountMonthlyBalanceDomain> persistedMonthlyBalances) {

    System.out.println("Adjusting Opening Balances");
    log.info("Adjusting Opening Balances for account {}", accountId.value());
    List<AccountMonthlyBalanceDomain> balancesToSave = new ArrayList<>(persistedMonthlyBalances);

    for (AccountMonthlyBalanceDomain currentMonthlyBalance : persistedMonthlyBalances) {

      //TODO Validate ID monthly balance Domain

      YearMonth nextPeriod = currentMonthlyBalance.getPeriod().plusMonths(1);
      // Syncing Next month opening balance with current month closing balance
      AccountMonthlyBalanceDomain nextMonthlyBalanceOfCurrent = persistedMonthlyBalances.stream()
          .filter(mb -> mb.getPeriod().equals(nextPeriod))
          .findFirst()
          .orElse(null);

      // If not found in memory list, try to fetch from DB or create new one
      if (nextMonthlyBalanceOfCurrent == null) {

        nextMonthlyBalanceOfCurrent = accountMonthlyBalanceRepo.findByPeriod(nextPeriod)
            .orElseGet(() -> AccountMonthlyBalanceDomain.of(accountId, nextPeriod)
            );
      }
      nextMonthlyBalanceOfCurrent.setOpeningBalance(currentMonthlyBalance.getClosingBalance());

      currentMonthlyBalance.refreshProfitMonthly();

      if (!balancesToSave.contains(nextMonthlyBalanceOfCurrent)) {
        balancesToSave.add(nextMonthlyBalanceOfCurrent);
      }

    }

    return balancesToSave;

  }


  public CompletableFuture<List<AccountMonthlyBalanceDomain>> saveMonthlyBalancesASYNC(
      AccountId accountId,
      List<AccountMonthlyBalanceDomain> monthlyBalances) {

    log.info("Initiating asynchronous save of monthly balances");

    CompletableFuture<List<AccountMonthlyBalanceDomain>> saveTask =
        CompletableFuture
            .supplyAsync(createSaveSupplier(monthlyBalances))
            .exceptionally(ex -> {
              log.error("Error saving balances: {}", ex.getMessage(), ex);
              return Collections.emptyList(); // fallback value
            });

    CompletableFuture<List<AccountMonthlyBalanceDomain>> openingTask =
        CompletableFuture
            .supplyAsync(createOpeningBalanceSupplier(accountId, monthlyBalances))
            .exceptionally(ex -> {
              log.error("Error saving opening balances: {}", ex.getMessage(), ex);
              return Collections.emptyList();
            });

    return saveTask.thenCombine(openingTask, combineResults());
  }


  private BiFunction<List<AccountMonthlyBalanceDomain>, List<AccountMonthlyBalanceDomain>, List<AccountMonthlyBalanceDomain>> combineResults() {
    return (savedBalances, openingBalancesSynced) -> {
      return openingBalancesSynced;
    };
  }

  private List<AccountMonthlyBalanceDomain> save(List<AccountMonthlyBalanceDomain> monthlyBalanceToPersist) {
    log.info("Saving Monthly Balances {}", monthlyBalanceToPersist.size());
    System.out.println("Saving Monthly Balances...");
    return accountMonthlyBalanceRepo.save(monthlyBalanceToPersist);
  }

  private Supplier<List<AccountMonthlyBalanceDomain>> createSaveSupplier(
      List<AccountMonthlyBalanceDomain> monthlyBalance) {
    return () -> save(monthlyBalance);
  }

  private Supplier<List<AccountMonthlyBalanceDomain>> createOpeningBalanceSupplier(
      AccountId accountId,
      List<AccountMonthlyBalanceDomain> monthlyBalances) {
    return () -> calculateProfitMonthly(accountId, monthlyBalances);
  }

}
