package com.jbh.account.application.core.services.movements;

import com.jbh.account.application.core.dto.AddBasicMovementDTO;
import com.jbh.account.application.core.dto.MovementDTO;
import com.jbh.account.application.core.vo.commands.AddMonthlyBalanceCommand;
import com.jbh.account.application.core.vo.commands.AddMovementCommand;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.vo.AccountMovementMetadata;
import com.jbh.account.domain.vo.AccountPK;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface AccountMovementApplicationService {

  /**
   * It creates a deposit movement for the next month with the dividends(monthly profit reported).
   * It creates a withdrawal movement for the next month with the income withholding tax
   * amount(Retefuente). @See {@link
   * com.jbh.account.application.core.services.account.AccountService#syncByMovement( AccountPK,
   * MovementDTO, boolean isMonthOfficiallyReported)}
   *
   * <p>It will update the monthly balance for the next month
   *
   * <p>For each movement, it will update the account current balance and net profit. <p<The monthly
   * balance for the next mont will be synced.
   *
   * @param accountPK
   * @param nextMonthlyBalanceCommand The command with the monthly balance for the next month. The
   *     closing balance should include the monthly profit reported and the income withholding tax
   *     amount.
   * @throws AccountBusinessException
   */
  void addDividendsMovementForNextMonth(
      AccountPK accountPK, AddMonthlyBalanceCommand nextMonthlyBalanceCommand)
      throws AccountBusinessException;

  /**
   * It registers the movement in the database. It will update the account and the monthly balances
   * for the month of the movement date. It will update the account current balance and net profit.
   *
   * @param accountPK
   * @param movementCommand
   * @return
   * @throws AccountBusinessException
   */
  AddBasicMovementDTO addMovementProcessingBalances(
      AccountPK accountPK, AddMovementCommand movementCommand) throws AccountBusinessException;

  AddBasicMovementDTO processMovement(
      MovementDTO movementDTO, AccountPK accountPK, boolean isMonthOfficiallyReported)
      throws AccountBusinessException;

  void addDividendsMovement(
      AccountPK accountPK,
      LocalDate movementDate,
      BigDecimal dividendsAmount,
      BigDecimal balanceSnapshot,
      BigDecimal incomeWithholdingTaxAmount,
      AccountMovementMetadata metadata)
      throws AccountBusinessException;
}
