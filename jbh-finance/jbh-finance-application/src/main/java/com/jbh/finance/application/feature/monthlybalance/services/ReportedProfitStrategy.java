package com.jbh.finance.application.feature.monthlybalance.services;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.monthlybalance.commands.AddMonthlyBalanceCommand;
import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.finance.application.feature.movement.services.ProcessMovementService;
import com.jbh.finance.application.feature.product.services.ProductLifecycleService;
import com.jbh.finance.domain.product.vo.ProductId;
import com.jbh.finance.domain.product.vo.ProductPK;
import java.math.BigDecimal;

public class ReportedProfitStrategy implements MonthlyBalanceProfitStrategy {

  private final MonthlyBalanceLifecycleService monthlyBalanceService;
  private final ProductLifecycleService accountService;
  private final ProcessMovementService movementApplicationService;

  public ReportedProfitStrategy(
      final MonthlyBalanceLifecycleService monthlyBalanceService,
      final ProductLifecycleService accountService,
      final ProcessMovementService movementApplicationService) {
    this.movementApplicationService = movementApplicationService;
    this.accountService = accountService;
    this.monthlyBalanceService = monthlyBalanceService;
  }

  @Override
  public MonthlyBalanceDTO registerOfficialMonthlyBalance(
      final ProductPK accountPK,
      final MonthlyBalanceDTO monthlyBalanceDomain,
      final AddMonthlyBalanceCommand command)
      throws BusinessException {
    final BigDecimal currentBalance = command.closingBalance();
    final BigDecimal monthlyProfitReported = command.monthlyProfitReported();
    final BigDecimal incomeWithholdingTaxAmount =
        command.incomeWithholdingTaxAmount() != null
            ? command.incomeWithholdingTaxAmount()
            : BigDecimal.ZERO;

    assert monthlyProfitReported != null;

    // The monthly reported profit is subtracted from the closing balance
    // The withholding tax is added to the closing balance
    BigDecimal updatedMonthlyClosedBalance;

    // A deposit will be created with dividends category for next month.
    updatedMonthlyClosedBalance = currentBalance.subtract(monthlyProfitReported);
    // A withdrawal will be created with income withholding tax category for next month.
    updatedMonthlyClosedBalance = updatedMonthlyClosedBalance.add(incomeWithholdingTaxAmount);

    // Then

    final var profitCommand = command.withClosingBalance(updatedMonthlyClosedBalance);
    final MonthlyBalanceDTO savedMonthlyReported =
        monthlyBalanceService.updateOfficialReportedBalance(monthlyBalanceDomain, profitCommand);

    // Run in background to async task (One for each movement)
    movementApplicationService.addDividendsMovementForNextMonth(accountPK, command);

    // This is important to be after the dividends movement because it will update the productDTO
    // net
    // profit  with the monthly reported profit
    if (monthlyBalanceService.isLastOfficialReport(savedMonthlyReported)) {
      final BigDecimal calculatedNetProfit = sumNetProfitOfficialReported(accountPK.productId());
      accountService.updateClosingProfitBalances(
          accountPK.productId(), currentBalance, calculatedNetProfit);
    }

    return savedMonthlyReported;
  }

  private BigDecimal sumNetProfitOfficialReported(final ProductId accountId) {
    return monthlyBalanceService.sumNetProfitOfficialReported(accountId);
  }
}
