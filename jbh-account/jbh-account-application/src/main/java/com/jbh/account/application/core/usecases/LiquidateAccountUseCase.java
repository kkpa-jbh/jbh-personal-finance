package com.jbh.account.application.core.usecases;

import com.jbh.account.application.core.dto.LiquidationResultDTO;
import com.jbh.account.application.core.vo.commands.LiquidateAccountCommand;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.vo.AccountId;
import java.util.UUID;

/**
 * Use case for completely liquidating an investment, CDT, or any account that is handle by a third
 * party broker, and transferring the proceeds to a personal account or an external account.
 *
 * <p>This represents a complete exit from an investment position where: - All shares/units are sold
 * - The investment account at the broker is closed or emptied - Proceeds are transferred to the
 * owner's designated account
 */
public interface LiquidateAccountUseCase {

  /**
   * Liquidates the specified account (investment, CDT, or broker-managed account) and transfers the
   * proceeds to the designated target account. This operation represents a complete exit from the
   * position with full withdrawal of funds.
   *
   * @param userId the user initiating the liquidation
   * @param accountId the account to be liquidated
   * @param liquidationCommand contains liquidation details and target account information
   * @return liquidation result containing transaction details and final amounts
   * @throws AccountBusinessException if liquidation cannot be processed
   */
  LiquidationResultDTO liquidateAccount(
      UUID userId, AccountId accountId, LiquidateAccountCommand liquidationCommand)
      throws AccountBusinessException;
}
