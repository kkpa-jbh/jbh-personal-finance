package com.jbh.products.application.core.services.monthlybalance;

import com.jbh.commons.exception.BusinessException;
import com.jbh.products.application.core.dto.MonthlyBalanceDTO;
import com.jbh.products.application.core.services.account.ProductsService;
import com.jbh.products.application.core.services.movements.AccountMovementApplicationService;
import com.jbh.products.application.core.vo.commands.AddMonthlyBalanceCommand;
import com.jbh.products.domain.product.vo.ProductId;
import com.jbh.products.domain.product.vo.ProductPK;
import java.math.BigDecimal;

public class ReportedProfitStrategy implements MonthlyBalanceProfitStrategy {

  private final MonthlyBalanceService monthlyBalanceService;
  private final ProductsService accountService;
  private final AccountMovementApplicationService movementApplicationService;

  public ReportedProfitStrategy(
      final MonthlyBalanceService monthlyBalanceService,
      final ProductsService accountService,
      final AccountMovementApplicationService movementApplicationService) {
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

    // This is important to be after the dividends movement because it will update the account net
    // profit  with the monthly reported profit
    if (monthlyBalanceService.isLastOfficialReport(savedMonthlyReported)) {
      final BigDecimal calculatedNetProfit = sumNetProfitOfficialReported(accountPK.accountId());
      accountService.updateClosingProfitBalances(
          accountPK.accountId(), currentBalance, calculatedNetProfit);
    }

    return savedMonthlyReported;
  }

  private BigDecimal sumNetProfitOfficialReported(final ProductId accountId) {
    return monthlyBalanceService.sumNetProfitOfficialReported(accountId);
  }
}
