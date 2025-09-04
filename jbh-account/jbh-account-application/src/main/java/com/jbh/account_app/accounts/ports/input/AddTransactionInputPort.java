package com.jbh.account_app.accounts.ports.input;

import com.jbh.account_app.accounts.ports.output.AccountMonthlyBalanceRepository;
import com.jbh.account_app.accounts.ports.output.AccountRepository;
import com.jbh.account_app.accounts.usecases.AddTransactionUseCase;
import com.jbh.account_app.accounts.vo.AddTransactionWithDateAmount;
import com.jbh.account_app.acid.UnitOfWork;
import com.jbh.account_app.transactions.ports.output.TransactionRepository;
import com.jbh.accounts_mgmt.accounts.domain.AccountDomain;
import com.jbh.accounts_mgmt.accounts.domain.AccountId;
import com.jbh.accounts_mgmt.accounts.domain.AccountMonthlyBalanceDomain;
import com.jbh.accounts_mgmt.transactions.TransactionDomain;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class AddTransactionInputPort implements AddTransactionUseCase {

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
    requestVO.validate();

    AccountDomain accountDomain = accountRepository.findByAccountId(requestVO.userId(), accountId)
        .orElseThrow(() -> new IllegalArgumentException("Account not found"));

    TransactionDomain txnDomain = TransactionDomain.of(accountDomain.getId(), requestVO.txnDate(),
        requestVO.totalAmount());

    // Sync account balance
    accountDomain.syncBalances(txnDomain);

    // Sync monthly balance
    int txnYear = txnDomain.getTxnDate().getYear();
    int txnMonth = txnDomain.getTxnDate().getMonthValue();
    Optional<AccountMonthlyBalanceDomain> accountMonthlyBalanceOpt = accountMonthlyBalanceRepository.findByAccountIdYearAndMonth(
        accountId, txnYear, txnMonth);
    AccountMonthlyBalanceDomain accountMonthlyBalance = accountMonthlyBalanceOpt.orElseGet(
        () -> AccountMonthlyBalanceDomain.of(accountDomain.getId(), txnYear, txnMonth));
    accountMonthlyBalance.syncTransaction(txnDomain);

    unitOfWork.execute(() -> {
      transactionRepository.save(txnDomain);
      accountRepository.save(accountDomain);
    });
    CompletableFuture.runAsync(() -> persistMonthlyBalance(accountMonthlyBalance));

  }

  private void persistMonthlyBalance(AccountMonthlyBalanceDomain accountMonthlyBalance) {
    accountMonthlyBalanceRepository.save(accountMonthlyBalance);
  }
}
