package com.jbh.finance.application.feature.movement.usecases;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.movement.commands.LiquidateProductCommand;
import com.jbh.finance.application.feature.movement.dto.LiquidationResultDTO;
import com.jbh.finance.domain.product.vo.ProductId;
import java.util.UUID;

/**
 * Completely liquidates an investment, CDT, or any productDTO managed by a third-party broker, and
 * transfers the proceeds to a designated productDTO.
 *
 * <p><strong>User Explanation:</strong> "Close an investment or broker-managed productDTO by selling
 * all holdings and transferring the funds to another productDTO. This represents a complete exit from
 * your investment position."
 *
 * <p><strong>Business Rules:</strong>
 *
 * <ul>
 *   <li>All shares/units are sold at liquidation
 *   <li>The investment productDTO is emptied and closed
 *   <li>Proceeds can be transferred to an internal productDTO or an external productDTO
 *   <li>If transferring to internal productDTO, creates liquidation movement and deposit movement
 * </ul>
 */
public interface LiquidateProductUseCase {

  /**
   * Liquidates the specified productDTO and optionally transfers proceeds to a designated internal
   * productDTO.
   *
   * <p><strong>Validations:</strong>
   *
   * <ul>
   *   <li>Command fields validation (via command.validate())
   *   <li>Source productDTO must exist
   *   <li>Target productDTO must exist (if internal transfer specified)
   * </ul>
   *
   * <p><strong>Database Operations:</strong>
   *
   * <ul>
   *   <li>INSERT: Liquidation movement in source productDTO
   *   <li>UPDATE: Source productDTO balance set to zero
   *   <li>INSERT: Deposit/dividend movement in target productDTO (if internal transfer specified)
   *   <li>UPDATE: Target productDTO balance (if internal transfer specified)
   * </ul>
   *
   * @param userId the user initiating the liquidation
   * @param accountId the productDTO to be liquidated
   * @param liquidationCommand contains liquidation date, closing balance, and optional target
   *     productDTO
   * @return liquidation result containing success status and transaction details
   * @throws BusinessException if liquidation cannot be processed or products not found
   */
  LiquidationResultDTO liquidateAccount(
      UUID userId, ProductId accountId, LiquidateProductCommand liquidationCommand)
      throws BusinessException;
}
