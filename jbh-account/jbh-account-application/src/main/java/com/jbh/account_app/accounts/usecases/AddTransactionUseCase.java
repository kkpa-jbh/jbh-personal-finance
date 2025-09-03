package com.jbh.account_app.accounts.usecases;

import com.jbh.account_app.accounts.vo.AddTransactionWithDateAmount;
import com.jbh.accounts_mgmt.accounts.domain.AccountId;

public interface AddTransactionUseCase {

  void addTransaction(AccountId accountId, AddTransactionWithDateAmount requestVO);
}
