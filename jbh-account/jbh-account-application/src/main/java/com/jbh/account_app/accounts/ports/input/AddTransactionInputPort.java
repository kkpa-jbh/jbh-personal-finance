package com.jbh.account_app.accounts.ports.input;

import com.jbh.account_app.accounts.ports.output.AccountRepository;
import com.jbh.account_app.accounts.usecases.AddTransactionUseCase;
import com.jbh.account_app.accounts.vo.AddTransactionWithDateAmount;
import com.jbh.account_app.transactions.ports.output.TransactionRepository;
import com.jbh.accounts_mgmt.accounts.domain.AccountDomain;
import com.jbh.accounts_mgmt.accounts.domain.AccountId;
import com.jbh.accounts_mgmt.transactions.TransactionDomain;

public class AddTransactionInputPort implements AddTransactionUseCase {

  private final AccountRepository accountRepository;
  private final TransactionRepository transactionRepository;

  public AddTransactionInputPort(AccountRepository accountRepository,
      TransactionRepository transactionRepository) {
    this.accountRepository = accountRepository;
    this.transactionRepository = transactionRepository;
  }


  @Override
  public void addTransaction(AccountId accountId, AddTransactionWithDateAmount requestVO) {

    requestVO.validate();

    AccountDomain accountDomain = accountRepository.findByAccountId(requestVO.userId(), accountId)
        .orElseThrow(() -> new IllegalArgumentException("Account not found"));

    TransactionDomain transactionDomain = TransactionDomain.of(accountDomain.getId(), requestVO.txnDate(),
        requestVO.totalAmount());

    accountDomain.syncBalances(transactionDomain);
    
    transactionRepository.save(transactionDomain);
    accountRepository.save(accountDomain);
  }
}
