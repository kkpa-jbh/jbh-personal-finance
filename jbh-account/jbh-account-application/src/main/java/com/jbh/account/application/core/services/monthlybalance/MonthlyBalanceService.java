package com.jbh.account.application.core.services.monthlybalance;

import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.application.core.dto.MovementDTO;
import com.jbh.account.application.core.ports.output.monthlybalance.AccountMonthlyBalanceQueryRepo;
import com.jbh.account.application.core.ports.output.monthlybalance.AccountMonthlyBalanceWriterRepository;
import com.jbh.account.domain.vo.AccountId;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface MonthlyBalanceService
    extends AccountMonthlyBalanceQueryRepo, AccountMonthlyBalanceWriterRepository {

  MonthlyBalanceDTO updateOpeningBalanceNextMonth(MonthlyBalanceDTO currentMonthlyBalance);

  boolean isLastOfficialReport(MonthlyBalanceDTO monthlyBalanceDTO);

  MonthlyBalanceDTO syncForNewMovement(MovementDTO newMovement);

  /**
   * Saves the monthly balances in the database and syncs them asynchronously. This is called when
   * uploading movements from file or creating a new movement.
   */
  CompletableFuture<List<MonthlyBalanceDTO>> persistBalancesAsync(
      AccountId accountId, List<MonthlyBalanceDTO> monthlyBalances);
}
