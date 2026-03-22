package com.jbh.finance.application.feature.monthlybalance.services;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.domain.product.vo.ProductId;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ProcessMonthlyBalanceService {

  void validateNewMovementForOfficialMonthlyReport(MovementDTO movementDTO)
      throws BusinessException;

  MonthlyBalanceDTO syncForNewMovement(MovementDTO newMovement) throws BusinessException;

  /**
   * Saves the monthly balances in the database and syncs them asynchronously. This is called when
   * uploading movements from file or creating a new movement.
   */
  CompletableFuture<List<MonthlyBalanceDTO>> persistBalancesAsync(
      ProductId accountId, List<MonthlyBalanceDTO> monthlyBalances);

  MonthlyBalanceDTO syncForReversedMovement(MovementDTO movementDTO) throws BusinessException;

  boolean findIfMonthlyBalanceWasOfficialReported(ProductId productId, YearMonth movementPeriod);

  Optional<MonthlyBalanceDTO> findByAccountIdAndPeriod(
      ProductId accountId, YearMonth monthlyPeriodKey);
}
