package com.jbh.account.domain.entity;

import static com.jbh.account.domain.utils.JbhMoneyUtils.JBH_ZERO;

import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.exceptions.GenericSpecificationException;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountMovementMetadata;
import com.jbh.account.domain.vo.MovementType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

public class EntityBuilder {

  public static AccountMonthlyBalanceDomain withInitialDataForNextMonth(
      final AccountId accountId,
      final YearMonth period,
      final BigDecimal closingBalance,
      final boolean gapPeriod) {
    return new AccountMonthlyBalanceDomain(
        null,
        accountId,
        period.getYear(),
        period.getMonthValue(),
        period,
        JBH_ZERO, // net growth rate
        JBH_ZERO, // total debits
        JBH_ZERO, // total credits
        JBH_ZERO, // opening balance
        closingBalance, // closing balance
        JBH_ZERO, // monthly net profit
        0, // total movements
        gapPeriod,
        false, // official report
        JBH_ZERO,
        null); // monthly profit reported
  }

  public static AccountMovementDomain with(
      final AccountId accountId,
      final LocalDate movementDate,
      final BigDecimal totalAmount,
      final BigDecimal balanceSnapshot,
      final MovementType movementType,
      final MovementCategoryDomain category) {

    final AccountMovementDomain movDomain =
        new AccountMovementDomain(
            accountId,
            movementType,
            movementDate,
            totalAmount,
            balanceSnapshot,
            AccountMovementMetadata.createEmpty(),
            category);

    try {
      movDomain.validate();
    } catch (final AccountBusinessException e) {
      throw new GenericSpecificationException(e.getMessage());
    }

    return movDomain;
  }
}
