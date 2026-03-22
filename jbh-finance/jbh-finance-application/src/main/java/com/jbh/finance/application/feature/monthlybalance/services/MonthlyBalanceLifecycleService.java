package com.jbh.finance.application.feature.monthlybalance.services;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.monthlybalance.commands.AddMonthlyBalanceCommand;
import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.finance.application.feature.monthlybalance.ports.output.MonthlyBalanceQueryRepo;
import com.jbh.finance.application.feature.monthlybalance.ports.output.MonthlyBalanceWriterRepo;

public interface MonthlyBalanceLifecycleService
    extends MonthlyBalanceQueryRepo, MonthlyBalanceWriterRepo {

  /**
   * 1. It will set/apply the closing balance, monthly profit reported and income withholding tax
   * amounts to the monthly balance. If the monthly profit reported is not null, it will set the
   * monthly net profit to the monthly profit reported.
   *
   * <p>2. It will update the monthly balance for the next month
   *
   * <p>3. It will update the productDTO current balance and net profit.
   *
   * @param reportedMonthlyBalance
   * @param command
   * @return
   */
  MonthlyBalanceDTO updateOfficialReportedBalance(
      MonthlyBalanceDTO reportedMonthlyBalance, AddMonthlyBalanceCommand command)
      throws BusinessException;

  void updateOpeningBalanceNextMonth(MonthlyBalanceDTO currentMonthlyBalance);

  boolean isLastOfficialReport(MonthlyBalanceDTO monthlyBalanceDTO);
}
