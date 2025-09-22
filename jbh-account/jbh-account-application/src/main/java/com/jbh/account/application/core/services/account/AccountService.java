package com.jbh.account.application.core.services.account;

import com.jbh.account.application.core.dto.AccountDTO;
import com.jbh.account.application.core.dto.AccountMonthlyBalanceDTO;
import com.jbh.account.application.core.ports.output.AccountRepository;
import com.jbh.account.domain.entity.AccountDomain;

/** Account Service Interface for CRUD operations */
public interface AccountService extends AccountRepository {

  AccountDTO save(AccountDomain account);

  AccountDTO save(AccountDTO account);

  AccountDTO syncByMonthlyReport(AccountMonthlyBalanceDTO monthlyBalance);
}
