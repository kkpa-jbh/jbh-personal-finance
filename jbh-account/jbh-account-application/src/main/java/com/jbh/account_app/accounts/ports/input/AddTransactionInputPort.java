package com.jbh.account_app.accounts.ports.input;

import com.jbh.account_app.accounts.ports.output.AccountMonthlyBalanceRepository;
import com.jbh.account_app.accounts.ports.output.AccountRepository;
import com.jbh.account_app.accounts.usecases.AddTransactionUseCase;
import com.jbh.account_app.accounts.vo.AddTransactionWithDateAmount;
import com.jbh.account_app.acid.UnitOfWork;
import com.jbh.account_app.common.logging.LoggerFactory;
import com.jbh.account_app.common.logging.LoggingContext;
import com.jbh.account_app.transactions.ports.output.TransactionRepository;
import com.jbh.accounts_mgmt.accounts.domain.AccountDomain;
import com.jbh.accounts_mgmt.accounts.domain.AccountId;
import com.jbh.accounts_mgmt.accounts.domain.AccountMonthlyBalanceDomain;
import com.jbh.accounts_mgmt.transactions.TransactionDomain;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;

public class AddTransactionInputPort implements AddTransactionUseCase {

  private static final Logger logger = LoggerFactory.getLogger(AddTransactionInputPort.class);

  private final AccountRepository accountRepository;
  private final TransactionRepository transactionRepository;
  private final AccountMonthlyBalanceRepository accountMonthlyBalanceRepository;
  private final UnitOfWork unitOfWork;

  public AddTransactionInputPort(AccountRepository accountRepository,
      TransactionRepository transactionRepository,
      UnitOfWork unitOfWork,
      AccountMonthlyBalanceRepository accountMonthlyBalanceRepository) {
    this.accountMonthlyBalanceRepository = accountMonthlyBalanceRepository;
    this.accountRepository = accountRepository;
    this.transactionRepository = transactionRepository;
    this.unitOfWork = unitOfWork;
  }


  @Override
  public void addTransaction(AccountId accountId, AddTransactionWithDateAmount requestVO) {
    LoggingContext.builder()
        .accountId(accountId.value())
        .userId(requestVO.userId())
        .module("account-application")
        .execute(() -> {
          logger.info("Starting transaction addition for account: {}, amount: {}",
              accountId.value(), requestVO.totalAmount());

          requestVO.validate();

          AccountDomain accountDomain = accountRepository.findByAccountId(requestVO.userId(), accountId)
              .orElseThrow(() -> {
                logger.error("Account not found for user: {} and account: {}",
                    requestVO.userId(), accountId.value());
                return new IllegalArgumentException("Account not found");
              });

          TransactionDomain txnDomain = TransactionDomain.of(accountDomain.getId(), requestVO.txnDate(),
              requestVO.totalAmount());

          // Sync account balance
          accountDomain.syncBalances(txnDomain);
          logger.debug("Account balance updated to: {}", accountDomain.getBalance());

          // Sync monthly balance
          int txnYear = txnDomain.getTxnDate().getYear();
          int txnMonth = txnDomain.getTxnDate().getMonthValue();
          Optional<AccountMonthlyBalanceDomain> accountMonthlyBalanceOpt = accountMonthlyBalanceRepository.findByAccountIdYearAndMonth(
              accountId, txnYear, txnMonth);
          AccountMonthlyBalanceDomain accountMonthlyBalance = accountMonthlyBalanceOpt.orElseGet(
              () -> AccountMonthlyBalanceDomain.of(accountDomain.getId(), txnYear, txnMonth));
          accountMonthlyBalance.syncTransaction(txnDomain);

          logger.debug("Monthly balance updated for {}-{}", txnYear, txnMonth);

          unitOfWork.execute(() -> {
            logger.info("Persisting transaction and account changes");
            transactionRepository.save(txnDomain);
            accountRepository.save(accountDomain);
          });

          CompletableFuture.runAsync(() -> persistMonthlyBalance(accountMonthlyBalance));

          logger.info("Transaction addition completed successfully for account: {}",
              accountId.value());
        });
  }

  private void persistMonthlyBalance(AccountMonthlyBalanceDomain accountMonthlyBalance) {
    accountMonthlyBalanceRepository.save(accountMonthlyBalance);
  }
}
