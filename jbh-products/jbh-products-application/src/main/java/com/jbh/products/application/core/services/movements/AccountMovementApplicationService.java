package com.jbh.products.application.core.services.movements;

import com.jbh.commons.exception.BusinessException;
import com.jbh.products.application.core.dto.AddBasicMovementDTO;
import com.jbh.products.application.core.dto.MovementDTO;
import com.jbh.products.application.core.services.account.ProductsService;
import com.jbh.products.application.core.vo.commands.AddMonthlyBalanceCommand;
import com.jbh.products.application.core.vo.commands.AddMovementCommand;
import com.jbh.products.domain.vo.AccountMovementMetadata;
import com.jbh.products.domain.vo.ProductPK;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface AccountMovementApplicationService {

  /**
   * It creates a deposit movement for the next month with the dividends(monthly profit reported).
   * It creates a withdrawal movement for the next month with the income withholding tax
   * amount(Retefuente). @See {@link ProductsService#syncByMovement( ProductPK, MovementDTO, boolean
   * isMonthOfficiallyReported)}
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
   * @throws BusinessException
   */
  void addDividendsMovementForNextMonth(
      ProductPK accountPK, AddMonthlyBalanceCommand nextMonthlyBalanceCommand)
      throws BusinessException;

  /**
   * It registers the movement in the database. It will update the account and the monthly balances
   * for the month of the movement date. It will update the account current balance and net profit.
   *
   * @param accountPK
   * @param movementCommand
   * @return
   * @throws BusinessException
   */
  AddBasicMovementDTO addMovementProcessingBalances(
      ProductPK accountPK, AddMovementCommand movementCommand) throws BusinessException;

  AddBasicMovementDTO processMovement(
      MovementDTO movementDTO, ProductPK accountPK, boolean isMonthOfficiallyReported)
      throws BusinessException;

  void addDividendsMovement(
      ProductPK accountPK,
      LocalDate movementDate,
      BigDecimal dividendsAmount,
      BigDecimal balanceSnapshot,
      BigDecimal incomeWithholdingTaxAmount,
      AccountMovementMetadata metadata)
      throws BusinessException;
}
