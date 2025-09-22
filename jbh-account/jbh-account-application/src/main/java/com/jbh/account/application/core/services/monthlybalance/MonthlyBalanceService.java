package com.jbh.account.application.core.services.monthlybalance;

import com.jbh.account.application.core.dto.AccountMonthlyBalanceDTO;
import com.jbh.account.application.core.ports.output.monthlybalance.AccountMonthlyBalanceQueryRepo;
import com.jbh.account.application.core.ports.output.monthlybalance.AccountMonthlyBalanceWriterRepository;

public interface MonthlyBalanceService
    extends AccountMonthlyBalanceQueryRepo, AccountMonthlyBalanceWriterRepository {

  AccountMonthlyBalanceDTO updateOpeningBalanceNextMonth(
      AccountMonthlyBalanceDTO currentMonthlyBalance);

  boolean isLastOfficialReport(AccountMonthlyBalanceDTO monthlyBalanceDTO);
}
