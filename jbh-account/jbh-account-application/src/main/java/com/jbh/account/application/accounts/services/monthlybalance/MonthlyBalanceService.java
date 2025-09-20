package com.jbh.account.application.accounts.services.monthlybalance;

import com.jbh.account.application.accounts.dto.AccountMonthlyBalanceDTO;
import com.jbh.account.application.accounts.ports.output.monthlybalance.AccountMonthlyBalanceQueryRepo;
import com.jbh.account.application.accounts.ports.output.monthlybalance.AccountMonthlyBalanceWriterRepository;

public interface MonthlyBalanceService
    extends AccountMonthlyBalanceQueryRepo, AccountMonthlyBalanceWriterRepository {

  AccountMonthlyBalanceDTO updateOpeningBalanceNextMonth(
      AccountMonthlyBalanceDTO currentMonthlyBalance);
}
