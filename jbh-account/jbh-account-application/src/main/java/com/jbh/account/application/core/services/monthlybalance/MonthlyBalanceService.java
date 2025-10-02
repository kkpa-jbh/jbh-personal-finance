package com.jbh.account.application.core.services.monthlybalance;

import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.application.core.dto.MovementDTO;
import com.jbh.account.application.core.ports.output.monthlybalance.AccountMonthlyBalanceQueryRepo;
import com.jbh.account.application.core.ports.output.monthlybalance.AccountMonthlyBalanceWriterRepository;
import com.jbh.account.application.core.vo.commands.AddMonthlyBalanceCommand;
import com.jbh.account.application.exceptions.JbhSpecificationApplication;
import com.jbh.account.domain.vo.AccountId;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface MonthlyBalanceService
    extends AccountMonthlyBalanceQueryRepo, AccountMonthlyBalanceWriterRepository {

  void updateOpeningBalanceNextMonth(MonthlyBalanceDTO currentMonthlyBalance);

  boolean isLastOfficialReport(MonthlyBalanceDTO monthlyBalanceDTO);

  MonthlyBalanceDTO syncForNewMovement(MovementDTO newMovement);

  /**
   * Saves the monthly balances in the database and syncs them asynchronously. This is called when
   * uploading movements from file or creating a new movement.
   */
  CompletableFuture<List<MonthlyBalanceDTO>> persistBalancesAsync(
      AccountId accountId, List<MonthlyBalanceDTO> monthlyBalances);

  void validateNewMovement(MovementDTO movementDTO) throws JbhSpecificationApplication;

  /**
   * 1. It will set/apply the closing balance, monthly profit reported and income withholding tax
   * amounts to the monthly balance. If the monthly profit reported is not null, it will set the
   * monthly net profit to the monthly profit reported.
   *
   * <p>2. It will update the monthly balance for the next month
   *
   * <p>3. It will update the account current balance and net profit.
   *
   * @param reportedMonthlyBalance
   * @param command
   * @return
   */
  MonthlyBalanceDTO updateOfficialReportedBalance(
      MonthlyBalanceDTO reportedMonthlyBalance, final AddMonthlyBalanceCommand command);
}
