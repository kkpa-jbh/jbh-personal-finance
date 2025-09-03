package com.jbh.account_app.transactions.ports.output;

import com.jbh.accounts_mgmt.transactions.TransactionDomain;

public interface TransactionRepository {

  TransactionDomain save(TransactionDomain transactionDomain);
}
