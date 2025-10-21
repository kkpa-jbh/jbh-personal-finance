package com.jbh.account.application.core.services.account;

import com.jbh.account.application.core.dto.AccountDTO;
import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.application.core.dto.MovementDTO;
import com.jbh.account.domain.entity.AccountDomain;
import com.jbh.account.domain.entity.AccountMovementDomain;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountPK;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Account Service Interface for CRUD operations */
public interface AccountService {
  Optional<AccountDTO> findByUserAndAccountId(UUID userId, AccountId accountId);

  AccountDTO findAccountOrThrow(AccountId accountId);

  AccountDTO save(AccountDTO account);

  AccountDTO save(AccountDomain account);

  /**
   * @param accountId
   * @param closingBalance
   * @param calculatedNetProfit NULL to do nothing
   */
  void updateClosingProfitBalances(
      AccountId accountId, BigDecimal closingBalance, BigDecimal calculatedNetProfit);

  /**
   * @param accountId
   * @param closingBalance
   */
  void updateClosingBalances(AccountId accountId, BigDecimal closingBalance);

  boolean isFullyWithdrawn(AccountId accountId);

  /**
   * Updates the net growth rate of the account if it's fully withdrawn
   *
   * @param accountId Account ID
   */
  void updateWhenFullyWithdrawn(AccountId accountId, List<MonthlyBalanceDTO> monthlyBalances);

  /**
   * Syncs the account by the movement. This method will update the account current balance and net
   * profit.
   *
   * @param accountPK
   * @param movement
   * @param isMonthOfficiallyReported
   * @return
   */
  AccountDTO syncByMovement(
      AccountPK accountPK, MovementDTO movement, boolean isMonthOfficiallyReported)
      throws AccountBusinessException;

  AccountDTO syncByUploadedMovements(
      AccountDomain accountDomain, List<AccountMovementDomain> uploadedMovements)
      throws AccountBusinessException;
}
