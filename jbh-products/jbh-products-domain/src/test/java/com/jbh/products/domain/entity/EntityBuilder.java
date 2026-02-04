package com.jbh.products.domain.entity;

import static com.jbh.commons.util.JbhMoneyUtils.JBH_ZERO;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.products.domain.vo.AccountMovementMetadata;
import com.jbh.products.domain.vo.MovementType;
import com.jbh.products.domain.vo.ProductId;
import com.jbh.products.domain.vo.ProductMetadata;
import com.jbh.products.domain.vo.ProductType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

public class EntityBuilder {

  public static ProductDomain buildProductTestObject(
      final String name, final ProductType type, final UUID userId, final ProductMetadata metadata)
      throws BusinessException {
    return ProductDomain.withMinimumDataForCreation(name, type, userId, metadata);
  }

  public static MonthlyBalanceDomain withInitialDataForNextMonth(
      final ProductId accountId,
      final YearMonth period,
      final BigDecimal closingBalance,
      final boolean gapPeriod) {
    return new MonthlyBalanceDomain(
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

  public static MovementDomain with(
      final ProductId accountId,
      final LocalDate movementDate,
      final BigDecimal totalAmount,
      final BigDecimal balanceSnapshot,
      final MovementType movementType,
      final MovementCategoryDomain category) {

    final MovementDomain movDomain =
        new MovementDomain(
            accountId,
            movementType,
            movementDate,
            totalAmount,
            balanceSnapshot,
            AccountMovementMetadata.createEmpty(),
            category,
            null);

    try {
      movDomain.validate();
    } catch (final BusinessException e) {
      throw new GenericSpecificationException(e.getMessage());
    }

    return movDomain;
  }
}
