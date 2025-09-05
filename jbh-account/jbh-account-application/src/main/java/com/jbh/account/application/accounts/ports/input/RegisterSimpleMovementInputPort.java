package com.jbh.account.application.accounts.ports.input;

import com.jbh.account.application.accounts.ports.output.AccountMonthlyBalanceRepository;
import com.jbh.account.application.accounts.ports.output.AccountRepository;
import com.jbh.account.application.accounts.usecases.RegisterMovementUseCase;
import com.jbh.account.application.accounts.vo.AddBasicMovementRequest;
import com.jbh.account.application.accounts.vo.AddBasicMovementResponse;
import com.jbh.account.application.accounts.vo.AddMultipleBasicMovementResponse;
import com.jbh.account.application.acid.UnitOfWork;
import com.jbh.account.application.common.logging.LoggerFactory;
import com.jbh.account.application.common.logging.LoggingContext;
import com.jbh.account.application.movements.ports.output.AccountMovementRepository;
import com.jbh.accounts_mgmt.accounts.AccountDomain;
import com.jbh.accounts_mgmt.accounts.AccountId;
import com.jbh.accounts_mgmt.accounts.AccountMonthlyBalanceDomain;
import com.jbh.accounts_mgmt.movements.AccountMovementDomain;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.slf4j.Logger;

public class RegisterSimpleMovementInputPort implements RegisterMovementUseCase {

  private static final Logger log = LoggerFactory.getLogger(RegisterSimpleMovementInputPort.class);

  private final AccountRepository accountRepository;
  private final AccountMovementRepository MovementRepository;
  private final AccountMonthlyBalanceRepository accountMonthlyBalanceRepo;
  private final UnitOfWork unitOfWork;

  public RegisterSimpleMovementInputPort(AccountRepository accountRepository,
      AccountMovementRepository MovementRepository,
      UnitOfWork unitOfWork,
      AccountMonthlyBalanceRepository accountMonthlyBalanceRepo) {
    this.accountMonthlyBalanceRepo = accountMonthlyBalanceRepo;
    this.accountRepository = accountRepository;
    this.MovementRepository = MovementRepository;
    this.unitOfWork = unitOfWork;
  }


  @Override
  public AddBasicMovementResponse addSimpleMovement(UUID userId, AccountId accountId,
      AddBasicMovementRequest basicMovementRequest) {
    return LoggingContext.builder()
        .accountId(accountId.value())
        .userId(userId)
        .module("account-application")
        .execute(() -> {

          log.info("Starting Movement addition for account: {}, amount: {} snapshot: {}",
              accountId.value(), basicMovementRequest.totalAmount(), basicMovementRequest.balanceSnapshot());

          // Input validations
          basicMovementRequest.validate();

          // Sync account balance
          AccountDomain accountDomain = findAccount(userId, accountId);
          AccountMovementDomain newMovement = AccountMovementDomain.of(accountDomain.getId(),
              basicMovementRequest.entryDate(),
              basicMovementRequest.totalAmount(), basicMovementRequest.balanceSnapshot());
          syncAccountBalanceByMovements(accountDomain, newMovement);

          // Sync monthly balance
          log.info("Syncing monthly balance asynchronously");
          int txnYear = newMovement.getMovementDate().getYear();
          int txnMonth = newMovement.getMovementDate().getMonthValue();
          Optional<AccountMonthlyBalanceDomain> accountMonthlyBalanceOpt = accountMonthlyBalanceRepo.findByAccountIdYearAndMonth(
              accountId, txnYear, txnMonth);
          AccountMonthlyBalanceDomain accountMonthlyBalance = accountMonthlyBalanceOpt.orElseGet(
              () -> AccountMonthlyBalanceDomain.of(accountDomain.getId(), txnYear, txnMonth));
          accountMonthlyBalance.syncMovement(newMovement);
          log.debug("Monthly balance updated for {}-{}", txnYear, txnMonth);
          persistMonthlyBalance(Collections.singletonList(accountMonthlyBalance));

          log.info("Movement addition completed successfully for account: {}", accountId.value());
          return new AddBasicMovementResponse(accountDomain, accountMonthlyBalance, newMovement);
        });

  }

  private void syncAccountBalanceByMovements(AccountDomain accountDomain,
      AccountMovementDomain newMovement) {
    accountDomain.syncBalances(newMovement);
    log.debug("Account balance updated to: {}", accountDomain.getMovementBalance());
    persistMovements(newMovement, accountDomain);
  }

  private void syncAccountBalanceByMovements(AccountDomain accountDomain,
      List<AccountMovementDomain> newMovements) {
    accountDomain.syncBalances(newMovements);
    log.debug("Account balance updated to: {}", accountDomain.getMovementBalance());
    persistMovements(newMovements, accountDomain);
  }

  private void persistMovements(AccountMovementDomain newMovement, AccountDomain accountDomain) {
    unitOfWork.execute(() -> {
      log.info("Persisting Movement and account changes");
      MovementRepository.save(newMovement);
      accountRepository.save(accountDomain);
    });
  }

  private void persistMovements(List<AccountMovementDomain> newMovements, AccountDomain accountDomain) {
    unitOfWork.execute(() -> {
      log.info("Persisting Movement and account changes");
      MovementRepository.save(newMovements);
      accountRepository.save(accountDomain);
    });
  }

  private AccountDomain findAccount(UUID userId, AccountId accountId) {
    if (userId == null) {
      log.error("User ID cannot be null");
      throw new IllegalArgumentException("User ID cannot be null");
    }

    return accountRepository.findByAccountId(userId, accountId)
        .orElseThrow(() -> {
          log.error("Account not found for user: {} and account: {}",
              userId, accountId.value());
          return new IllegalArgumentException("Account not found");
        });
  }

  @Override
  public AddMultipleBasicMovementResponse addSimpleMovement(UUID userId, AccountId accountId,
      List<AddBasicMovementRequest> allSimpleMovements) {

    if (allSimpleMovements == null || allSimpleMovements.isEmpty()) {
      throw new IllegalArgumentException("Movement list cannot be null or empty");
    }

    // Loop through all movements to validate using Index for better error tracing
    for (int i = 0; i < allSimpleMovements.size(); i++) {
      try {
        allSimpleMovements.get(i).validate();
      } catch (Exception e) {
        throw new IllegalArgumentException("Validation failed for movement at index " + i + ": " + e.getMessage());
      }
    }

    AccountDomain accountDomain = findAccount(userId, accountId);
    List<AccountMovementDomain> multipleMovementsDomain = mapSimpleMovementsToDomain(allSimpleMovements, accountDomain);
    syncAccountBalanceByMovements(accountDomain, multipleMovementsDomain);

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
          () -> AccountMonthlyBalanceDomain.of(accountDomain.getId(), year, month));

      accountMonthlyBalance.syncMovements(movementsInPeriod);
      log.debug("Monthly balance updated for {}-{}", year, month);
      monthlyBalancesToPersist.add(accountMonthlyBalance);
    });
    persistMonthlyBalance(monthlyBalancesToPersist);

    return new AddMultipleBasicMovementResponse(accountDomain, monthlyBalancesToPersist);

  }

  private List<AccountMovementDomain> mapSimpleMovementsToDomain(List<AddBasicMovementRequest> allSimpleMovements,
      AccountDomain accountDomain) {
    return allSimpleMovements.stream().map(movementRequest -> AccountMovementDomain.of(
            accountDomain.getId(),
            movementRequest.entryDate(),
            movementRequest.totalAmount(),
            movementRequest.balanceSnapshot()))
        .toList();
  }

  private void persistMonthlyBalance(List<AccountMonthlyBalanceDomain> accountMonthlyBalance) {
    CompletableFuture.runAsync(() ->
        accountMonthlyBalanceRepo.save(accountMonthlyBalance));
  }
}
