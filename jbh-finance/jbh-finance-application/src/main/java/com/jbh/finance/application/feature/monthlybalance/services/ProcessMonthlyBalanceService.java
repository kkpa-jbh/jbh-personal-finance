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

  /**
   * Assigns the movement to its monthly balance, saves it, and starts the background adjust.
   *
   * <p>Precondition: the caller holds the product lock ({@code lockForUpdate}) in an open
   * transaction.
   */
  MonthlyBalanceDTO syncForNewMovement(MovementDTO newMovement) throws BusinessException;

  /**
   * Saves the monthly balances on the caller thread and adjusts them in the background. This is
   * called when uploading movements from file or creating a new movement. The returned future holds
   * the adjusted balances.
   *
   * <p>Precondition: the caller holds the product lock ({@code lockForUpdate}) in an open
   * transaction.
   */
  CompletableFuture<List<MonthlyBalanceDTO>> persistBalancesAsync(
      ProductId accountId, List<MonthlyBalanceDTO> monthlyBalances);

  /**
   * Removes the movement from its monthly balance, saves it, and starts the background adjust.
   *
   * <p>Precondition: the caller holds the product lock ({@code lockForUpdate}) in an open
   * transaction.
   */
  MonthlyBalanceDTO syncForReversedMovement(MovementDTO movementDTO) throws BusinessException;

  /**
   * Waits until the background adjust tasks started before this call, on this instance, have
   * finished. Never call it inside a transaction: the tasks need the product lock, and the caller
   * would hold it while it waits.
   *
   * @throws IllegalStateException on timeout or interrupt
   */
  void awaitPendingSync();

  boolean findIfMonthlyBalanceWasOfficialReported(ProductId productId, YearMonth movementPeriod);

  Optional<MonthlyBalanceDTO> findByAccountIdAndPeriod(
      ProductId accountId, YearMonth monthlyPeriodKey);
}
