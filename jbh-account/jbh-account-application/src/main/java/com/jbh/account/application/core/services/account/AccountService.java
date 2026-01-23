package com.jbh.account.application.core.services.account;

import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.application.core.dto.MovementDTO;
import com.jbh.account.application.core.dto.ProductDTO;
import com.jbh.account.domain.entity.AccountMovementDomain;
import com.jbh.account.domain.entity.ProductDomain;
import com.jbh.account.domain.exceptions.ProductBusinessException;
import com.jbh.account.domain.vo.ProductId;
import com.jbh.account.domain.vo.ProductPK;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Account Service Interface for CRUD operations
 *
 * <p>Like a Repository abstraction.
 */
public interface AccountService {
  ProductDTO findByUserAndAccountId(UUID userId, ProductId accountId)
      throws ProductBusinessException;

  ProductDTO findAccountOrThrow(ProductId accountId);

  ProductDTO save(ProductDTO account);

  ProductDTO save(ProductDomain account);

  /**
   * @param accountId
   * @param closingBalance
   * @param calculatedNetProfit NULL to do nothing
   */
  void updateClosingProfitBalances(
      ProductId accountId, BigDecimal closingBalance, BigDecimal calculatedNetProfit);

  /**
   * @param accountId
   * @param closingBalance
   */
  void updateClosingBalances(ProductId accountId, BigDecimal closingBalance);

  boolean isFullyWithdrawn(ProductId accountId);

  /**
   * Updates the net growth rate of the account if it's fully withdrawn
   *
   * @param accountId Account ID
   */
  void updateWhenFullyWithdrawn(ProductId accountId, List<MonthlyBalanceDTO> monthlyBalances);

  /**
   * Syncs the account by the movement. This method will update the account current balance and net
   * profit.
   *
   * @param accountPK
   * @param movement
   * @param isMonthOfficiallyReported
   * @return
   */
  ProductDTO syncByMovement(
      ProductPK accountPK, MovementDTO movement, boolean isMonthOfficiallyReported)
      throws ProductBusinessException;

  ProductDTO syncByUploadedMovements(
      ProductDomain accountDomain, List<AccountMovementDomain> uploadedMovements)
      throws ProductBusinessException;
}
