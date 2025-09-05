package com.jbh.account_app.accounts.ports.input;

import com.jbh.account_app.accounts.ports.output.AccountMonthlyBalanceRepository;
import com.jbh.account_app.accounts.ports.output.AccountRepository;
import com.jbh.account_app.accounts.usecases.RegisterMovementUseCase;
import com.jbh.account_app.accounts.vo.AddMovementWithDateAmount;
import com.jbh.account_app.acid.UnitOfWork;
import com.jbh.account_app.common.logging.LoggerFactory;
import com.jbh.account_app.common.logging.LoggingContext;
import com.jbh.account_app.movements.ports.output.AccountMovementRepository;
import com.jbh.accounts_mgmt.accounts.AccountDomain;
import com.jbh.accounts_mgmt.accounts.AccountId;
import com.jbh.accounts_mgmt.accounts.AccountMonthlyBalanceDomain;
import com.jbh.accounts_mgmt.movements.AccountMovement;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;

public class RegisterSimpleMovementInputPort implements RegisterMovementUseCase {

  private static final Logger logger = LoggerFactory.getLogger(RegisterSimpleMovementInputPort.class);

  private final AccountRepository accountRepository;
  private final AccountMovementRepository MovementRepository;
  private final AccountMonthlyBalanceRepository accountMonthlyBalanceRepository;
  private final UnitOfWork unitOfWork;

  public RegisterSimpleMovementInputPort(AccountRepository accountRepository,
      AccountMovementRepository MovementRepository,
      UnitOfWork unitOfWork,
      AccountMonthlyBalanceRepository accountMonthlyBalanceRepository) {
    this.accountMonthlyBalanceRepository = accountMonthlyBalanceRepository;
    this.accountRepository = accountRepository;
    this.MovementRepository = MovementRepository;
    this.unitOfWork = unitOfWork;
  }


  @Override
  public void addSimpleMovement(UUID userId, AccountId accountId, AddMovementWithDateAmount requestVO) {
    LoggingContext.builder()
        .accountId(accountId.value())
        .userId(userId)
        .module("account-application")
        .execute(() -> {
          logger.info("Starting Movement addition for account: {}, amount: {}",
              accountId.value(), requestVO.totalAmount());

          if (userId == null) {
            logger.error("User ID cannot be null");
            throw new IllegalArgumentException("User ID cannot be null");
          }

          requestVO.validate();

          AccountDomain accountDomain = accountRepository.findByAccountId(userId, accountId)
              .orElseThrow(() -> {
                logger.error("Account not found for user: {} and account: {}",
                    userId, accountId.value());
                return new IllegalArgumentException("Account not found");
              });

          AccountMovement txnDomain = AccountMovement.of(accountDomain.getId(), requestVO.entryDate(),
              requestVO.totalAmount(), requestVO.balanceSnapshot());

          // Sync account balance
          accountDomain.syncBalances(txnDomain);
          logger.debug("Account balance updated to: {}", accountDomain.getBalance());

          // Sync monthly balance
          int txnYear = txnDomain.getMovementDate().getYear();
          int txnMonth = txnDomain.getMovementDate().getMonthValue();
          Optional<AccountMonthlyBalanceDomain> accountMonthlyBalanceOpt = accountMonthlyBalanceRepository.findByAccountIdYearAndMonth(
              accountId, txnYear, txnMonth);
          AccountMonthlyBalanceDomain accountMonthlyBalance = accountMonthlyBalanceOpt.orElseGet(
              () -> AccountMonthlyBalanceDomain.of(accountDomain.getId(), txnYear, txnMonth));
          accountMonthlyBalance.syncMovement(txnDomain);

          logger.debug("Monthly balance updated for {}-{}", txnYear, txnMonth);

          unitOfWork.execute(() -> {
            logger.info("Persisting Movement and account changes");
            MovementRepository.save(txnDomain);
            accountRepository.save(accountDomain);
          });

          CompletableFuture.runAsync(() -> persistMonthlyBalance(accountMonthlyBalance));

          logger.info("Movement addition completed successfully for account: {}",
              accountId.value());
        });
  }

  private void persistMonthlyBalance(AccountMonthlyBalanceDomain accountMonthlyBalance) {
    accountMonthlyBalanceRepository.save(accountMonthlyBalance);
  }
}
