package com.jbh.finance.application.feature.monthlybalance.services;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.monthlybalance.commands.AddMonthlyBalanceCommand;
import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.finance.application.feature.product.services.ProductLifecycleService;
import com.jbh.finance.domain.product.vo.ProductPK;

public class UnreportedProfitStrategy implements MonthlyBalanceProfitStrategy {

  private final MonthlyBalanceLifecycleService monthlyBalanceService;
  private final ProductLifecycleService accountService;

  public UnreportedProfitStrategy(
      final MonthlyBalanceLifecycleService monthlyBalanceService,
      final ProductLifecycleService accountService) {
    this.monthlyBalanceService = monthlyBalanceService;
    this.accountService = accountService;
  }

  @Override
  public MonthlyBalanceDTO registerOfficialMonthlyBalance(
      final ProductPK accountPK,
      final MonthlyBalanceDTO monthlyBalanceDomain,
      final AddMonthlyBalanceCommand command)
      throws BusinessException {

    final MonthlyBalanceDTO savedMonthlyReported =
        monthlyBalanceService.updateOfficialReportedBalance(monthlyBalanceDomain, command);

    if (monthlyBalanceService.isLastOfficialReport(savedMonthlyReported)) {
      accountService.updateClosingBalances(
          accountPK.accountId(), savedMonthlyReported.closingBalance());
    }

    return savedMonthlyReported;
  }
}
