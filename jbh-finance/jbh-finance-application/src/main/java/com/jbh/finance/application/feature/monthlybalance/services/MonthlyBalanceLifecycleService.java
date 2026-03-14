package com.jbh.finance.application.feature.monthlybalance.services;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.monthlybalance.commands.AddMonthlyBalanceCommand;
import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.finance.application.feature.monthlybalance.ports.output.MonthlyBalanceQueryRepo;
import com.jbh.finance.application.feature.monthlybalance.ports.output.MonthlyBalanceWriterRepo;
import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.domain.product.vo.ProductId;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface MonthlyBalanceLifecycleService
    extends MonthlyBalanceQueryRepo, MonthlyBalanceWriterRepo {

  void validateNewMovementForOfficialMonthlyReport(MovementDTO movementDTO)
      throws BusinessException;

  /**
   * 1. It will set/apply the closing balance, monthly profit reported and income withholding tax
   * amounts to the monthly balance. If the monthly profit reported is not null, it will set the
   * monthly net profit to the monthly profit reported.
   *
   * <p>2. It will update the monthly balance for the next month
   *
   * <p>3. It will update the productDTO current balance and net profit.
   *
   * @param reportedMonthlyBalance
   * @param command
   * @return
   */
  MonthlyBalanceDTO updateOfficialReportedBalance(
      MonthlyBalanceDTO reportedMonthlyBalance, AddMonthlyBalanceCommand command)
      throws BusinessException;

  void updateOpeningBalanceNextMonth(MonthlyBalanceDTO currentMonthlyBalance);

  boolean isLastOfficialReport(MonthlyBalanceDTO monthlyBalanceDTO);

  MonthlyBalanceDTO syncForNewMovement(MovementDTO newMovement) throws BusinessException;

  /**
   * Saves the monthly balances in the database and syncs them asynchronously. This is called when
   * uploading movements from file or creating a new movement.
   */
  CompletableFuture<List<MonthlyBalanceDTO>> persistBalancesAsync(
      ProductId accountId, List<MonthlyBalanceDTO> monthlyBalances);

  MonthlyBalanceDTO syncForReversedMovement(MovementDTO movementDTO) throws BusinessException;
}
