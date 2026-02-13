package com.jbh.finance.application.feature.product.services;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.domain.movement.MovementDomain;
import com.jbh.finance.domain.product.ProductDomain;
import com.jbh.finance.domain.product.vo.ProductId;
import com.jbh.finance.domain.product.vo.ProductPK;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Account Service Interface for CRUD operations
 *
 * <p>Like a Repository abstraction.
 */
public interface ProductLifecycleService {
  ProductDTO findOrThrowByUserAndProductId(UUID userId, ProductId productId)
      throws BusinessException;

  ProductDTO findOrThrowByIdProductId(ProductId productId);

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
   * Updates the net growth rate of the productDTO if it's fully withdrawn
   *
   * @param accountId Account ID
   */
  void updateWhenFullyWithdrawn(ProductId accountId, List<MonthlyBalanceDTO> monthlyBalances);

  /**
   * Syncs the productDTO by the movement. This method will update the productDTO current balance
   * and net profit.
   *
   * @param accountPK
   * @param movement
   * @param isMonthOfficiallyReported
   * @return
   */
  ProductDTO syncByMovement(
      ProductPK accountPK, MovementDTO movement, boolean isMonthOfficiallyReported)
      throws BusinessException;

  ProductDTO syncByUploadedMovements(
      ProductDomain accountDomain, List<MovementDomain> uploadedMovements) throws BusinessException;

  List<ProductDTO> findActiveByUserId(UUID userId);

  Optional<ProductDTO> findProductById(ProductId productId);

  void deleteProduct(ProductId productId);
}
