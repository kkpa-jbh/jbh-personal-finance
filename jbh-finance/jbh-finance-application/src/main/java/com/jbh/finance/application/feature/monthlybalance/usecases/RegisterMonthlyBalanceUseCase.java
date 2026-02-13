package com.jbh.finance.application.feature.monthlybalance.usecases;

import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.finance.application.feature.monthlybalance.commands.AddMonthlyBalanceCommand;
import com.jbh.finance.domain.product.vo.ProductId;
import com.jbh.commons.exception.BusinessException;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Registers the official monthly balance for a product based on the institution's reported closing
 * balance.
 *
 * <p><strong>User Explanation:</strong> "Record the official end-of-month balance from your bank
 * or broker statement. This helps track your monthly growth and validate your transaction
 * history."
 *
 * <p><strong>Business Rules:</strong>
 *
 * <ul>
 *   <li>Monthly balances must be registered in consecutive order (no gaps)
 *   <li>Can only register balances for past periods
 *   <li>If this is the first balance, an initial balance movement is automatically created
 *   <li>Calculates net growth rate and monthly expenses based on movements
 *   <li>Updates the opening balance for the next month
 * </ul>
 */
public interface RegisterMonthlyBalanceUseCase {

  /**
   * Registers the official monthly balance with institution-reported closing balance and optional
   * profit.
   *
   * <p><strong>Validations:</strong>
   *
   * <ul>
   *   <li>Command fields validation (via command.validate())
   *   <li>Period must be in the past (before running date)
   *   <li>Monthly balances must be consecutive (no gaps between months)
   * </ul>
   *
   * <p><strong>Database Operations:</strong>
   *
   * <ul>
   *   <li>INSERT: Initial balance movement (if first monthly balance)
   *   <li>UPDATE: Monthly balance record with official closing balance
   *   <li>INSERT: Dividend/profit movement (if monthly profit is reported and target productDTO
   *       specified)
   *   <li>UPDATE: Opening balance of next month's record
   * </ul>
   *
   * @param runningDate the current date for validation purposes
   * @param userId the user who owns the product
   * @param accountId the product ID for which the balance is being registered
   * @param command contains monthly period, closing balance, and optional profit information
   * @return the registered monthly balance DTO
   * @throws BusinessException if validations fail or monthly balances are not consecutive
   */
  MonthlyBalanceDTO registerOfficialMonthlyBalance(
      LocalDate runningDate, UUID userId, ProductId accountId, AddMonthlyBalanceCommand command)
      throws BusinessException;
}
