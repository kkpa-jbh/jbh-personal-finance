package com.jbh.account.application.core.services.monthlybalance;

import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.application.core.services.account.AccountService;
import com.jbh.account.application.core.vo.commands.AddMonthlyBalanceCommand;
import com.jbh.account.domain.exceptions.ProductBusinessException;
import com.jbh.account.domain.vo.ProductPK;

public class UnreportedProfitStrategy implements MonthlyBalanceProfitStrategy {

  private final MonthlyBalanceService monthlyBalanceService;
  private final AccountService accountService;

  public UnreportedProfitStrategy(
      final MonthlyBalanceService monthlyBalanceService, final AccountService accountService) {
    this.monthlyBalanceService = monthlyBalanceService;
    this.accountService = accountService;
  }

  @Override
  public MonthlyBalanceDTO registerOfficialMonthlyBalance(
      final ProductPK accountPK,
      final MonthlyBalanceDTO monthlyBalanceDomain,
      final AddMonthlyBalanceCommand command)
      throws ProductBusinessException {

    final MonthlyBalanceDTO savedMonthlyReported =
        monthlyBalanceService.updateOfficialReportedBalance(monthlyBalanceDomain, command);

    if (monthlyBalanceService.isLastOfficialReport(savedMonthlyReported)) {
      accountService.updateClosingBalances(
          accountPK.accountId(), savedMonthlyReported.closingBalance());
    }

    return savedMonthlyReported;
  }
}
