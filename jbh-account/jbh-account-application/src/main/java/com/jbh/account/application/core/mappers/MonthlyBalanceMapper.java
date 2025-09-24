package com.jbh.account.application.core.mappers;

import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.domain.entity.AccountMonthlyBalanceDomain;

public final class MonthlyBalanceMapper {

  private MonthlyBalanceMapper() {}

  public static MonthlyBalanceDTO toDTO(final AccountMonthlyBalanceDomain domain) {
    if (domain == null) {
      return null;
    }

    return MonthlyBalanceDTO.defaultBuilder()
        .id(domain.getId())
        .accountId(domain.getAccountId())
        .year(domain.getYear())
        .month(domain.getMonth())
        .period(domain.getPeriod())
        .totalDebits(domain.getTotalDebits())
        .totalCredits(domain.getTotalCredits())
        .movementBalance(domain.getMovementBalance())
        .openingBalance(domain.getOpeningBalance())
        .closingBalance(domain.getClosingBalance())
        .monthlyProfit(domain.getMonthlyProfit())
        .monthlyExpenses(domain.getMonthlyExpenses())
        .totalMovements(domain.getTotalMovements())
        .gapPeriod(domain.isGapPeriod())
        .officialMonthlyReport(domain.isOfficialMonthlyReport())
        .build();
  }

  public static AccountMonthlyBalanceDomain toDomain(final MonthlyBalanceDTO dto) {
    if (dto == null) {
      return null;
    }

    return new AccountMonthlyBalanceDomain(
        dto.id(),
        dto.accountId(),
        dto.year(),
        dto.month(),
        dto.period(),
        dto.netGrowthRate(),
        dto.totalDebits(),
        dto.totalCredits(),
        dto.movementBalance(),
        dto.openingBalance(),
        dto.closingBalance(),
        dto.monthlyProfit(),
        dto.monthlyExpenses(),
        dto.totalMovements(),
        dto.gapPeriod(),
        dto.officialMonthlyReport(),
        dto.monthlyProfitReported());
  }
}
