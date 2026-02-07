package com.jbh.products.application.core.services.monthlybalance;

import com.jbh.commons.exception.BusinessException;
import com.jbh.products.application.core.dto.MonthlyBalanceDTO;
import com.jbh.products.application.core.services.account.ProductsService;
import com.jbh.products.application.core.vo.commands.AddMonthlyBalanceCommand;
import com.jbh.products.domain.product.vo.ProductPK;

public class UnreportedProfitStrategy implements MonthlyBalanceProfitStrategy {

  private final MonthlyBalanceService monthlyBalanceService;
  private final ProductsService accountService;

  public UnreportedProfitStrategy(
      final MonthlyBalanceService monthlyBalanceService, final ProductsService accountService) {
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
