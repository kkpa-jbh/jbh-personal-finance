package com.jbh.account.application.accounts.ports.input;

import com.jbh.account.application.accounts.ports.output.AccountMonthlyBalanceRepository;
import com.jbh.account.application.accounts.ports.output.AccountRepository;
import com.jbh.account.application.accounts.usecases.RegisterMovementUseCase;
import com.jbh.account.application.accounts.vo.AddMovementResponse;
import com.jbh.account.application.accounts.vo.AddSimpleMovementRequest;
import com.jbh.account.application.acid.UnitOfWork;
import com.jbh.account.application.common.logging.LoggerFactory;
import com.jbh.account.application.common.logging.LoggingContext;
import com.jbh.account.application.movements.ports.output.AccountMovementRepository;
import com.jbh.accounts_mgmt.accounts.AccountDomain;
import com.jbh.accounts_mgmt.accounts.AccountId;
import com.jbh.accounts_mgmt.accounts.AccountMonthlyBalanceDomain;
import com.jbh.accounts_mgmt.movements.AccountMovementDomain;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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
  public AddMovementResponse addSimpleMovement(UUID userId, AccountId accountId, AddSimpleMovementRequest requestVO) {
    return LoggingContext.builder()
        .accountId(accountId.value())
        .userId(userId)
        .module("account-application")
        .execute(() -> {

          log.info("Starting Movement addition for account: {}, amount: {} snapshot: {}",
              accountId.value(), requestVO.totalAmount(), requestVO.balanceSnapshot());

          // Input validations
          if (userId == null) {
            log.error("User ID cannot be null");
            throw new IllegalArgumentException("User ID cannot be null");
          }
          requestVO.validate();

          // Sync account balance
          AccountDomain accountDomain = accountRepository.findByAccountId(userId, accountId)
              .orElseThrow(() -> {
                log.error("Account not found for user: {} and account: {}",
                    userId, accountId.value());
                return new IllegalArgumentException("Account not found");
              });
          AccountMovementDomain newMovement = AccountMovementDomain.of(accountDomain.getId(), requestVO.entryDate(),
              requestVO.totalAmount(), requestVO.balanceSnapshot());
          accountDomain.syncBalances(newMovement);
          log.debug("Account balance updated to: {}", accountDomain.getMovementBalance());

          // Sync monthly balance
          int txnYear = newMovement.getMovementDate().getYear();
          int txnMonth = newMovement.getMovementDate().getMonthValue();
          Optional<AccountMonthlyBalanceDomain> accountMonthlyBalanceOpt = accountMonthlyBalanceRepo.findByAccountIdYearAndMonth(
              accountId, txnYear, txnMonth);
          AccountMonthlyBalanceDomain accountMonthlyBalance = accountMonthlyBalanceOpt.orElseGet(
              () -> AccountMonthlyBalanceDomain.of(accountDomain.getId(), txnYear, txnMonth));
          accountMonthlyBalance.syncMovement(newMovement);
          log.debug("Monthly balance updated for {}-{}", txnYear, txnMonth);

          unitOfWork.execute(() -> {
            log.info("Persisting Movement and account changes");
            MovementRepository.save(newMovement);
            accountRepository.save(accountDomain);
          });

          log.info("Syncing monthly balance asynchronously");
          CompletableFuture.runAsync(() -> persistMonthlyBalance(accountMonthlyBalance));

          log.info("Movement addition completed successfully for account: {}", accountId.value());

          return new AddMovementResponse(accountDomain, accountMonthlyBalance, newMovement);
        });

  }

  private void persistMonthlyBalance(AccountMonthlyBalanceDomain accountMonthlyBalance) {
    accountMonthlyBalanceRepo.save(accountMonthlyBalance);
  }
}
