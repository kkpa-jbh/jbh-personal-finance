package com.jbh.account.application.accounts.services.monthlybalance;

import com.jbh.account.application.accounts.ports.output.monthlybalance.AccountMonthlyBalanceQueryRepo;
import com.jbh.account.application.accounts.ports.output.monthlybalance.AccountMonthlyBalanceWriterRepository;
import com.jbh.account.domain.vo.AccountMonthlyBalanceDTO;

public interface MonthlyBalanceService
    extends AccountMonthlyBalanceQueryRepo, AccountMonthlyBalanceWriterRepository {

  AccountMonthlyBalanceDTO updateOpeningBalanceNextMonth(
      AccountMonthlyBalanceDTO currentMonthlyBalance);
}
