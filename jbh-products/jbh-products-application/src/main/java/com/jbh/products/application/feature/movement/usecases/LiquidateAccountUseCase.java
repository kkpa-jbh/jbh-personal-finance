package com.jbh.products.application.feature.movement.usecases;

import com.jbh.products.application.feature.movement.dto.LiquidationResultDTO;
import com.jbh.products.application.feature.movement.commands.LiquidateAccountCommand;
import com.jbh.products.domain.product.vo.ProductId;
import com.jbh.commons.exception.BusinessException;
import java.util.UUID;

/**
 * Completely liquidates an investment, CDT, or any account managed by a third-party broker, and
 * transfers the proceeds to a designated account.
 *
 * <p><strong>User Explanation:</strong> "Close an investment or broker-managed account by selling
 * all holdings and transferring the funds to another account. This represents a complete exit from
 * your investment position."
 *
 * <p><strong>Business Rules:</strong>
 *
 * <ul>
 *   <li>All shares/units are sold at liquidation
 *   <li>The investment account is emptied and closed
 *   <li>Proceeds can be transferred to an internal account or an external account
 *   <li>If transferring to internal account, creates liquidation movement and deposit movement
 * </ul>
 */
public interface LiquidateAccountUseCase {

  /**
   * Liquidates the specified account and optionally transfers proceeds to a designated internal
   * account.
   *
   * <p><strong>Validations:</strong>
   *
   * <ul>
   *   <li>Command fields validation (via command.validate())
   *   <li>Source account must exist
   *   <li>Target account must exist (if internal transfer specified)
   * </ul>
   *
   * <p><strong>Database Operations:</strong>
   *
   * <ul>
   *   <li>INSERT: Liquidation movement in source account
   *   <li>UPDATE: Source account balance set to zero
   *   <li>INSERT: Deposit/dividend movement in target account (if internal transfer specified)
   *   <li>UPDATE: Target account balance (if internal transfer specified)
   * </ul>
   *
   * @param userId the user initiating the liquidation
   * @param accountId the account to be liquidated
   * @param liquidationCommand contains liquidation date, closing balance, and optional target
   *     account
   * @return liquidation result containing success status and transaction details
   * @throws BusinessException if liquidation cannot be processed or accounts not found
   */
  LiquidationResultDTO liquidateAccount(
      UUID userId, ProductId accountId, LiquidateAccountCommand liquidationCommand)
      throws BusinessException;
}
