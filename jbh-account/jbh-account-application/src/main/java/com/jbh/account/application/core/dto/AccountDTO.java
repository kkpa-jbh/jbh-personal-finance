package com.jbh.account.application.core.dto;

import static com.jbh.account.domain.utils.MoneyUtils.JBH_ZERO;

import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder(builderMethodName = "notUseThisInternalBuilder")
public record AccountDTO(
    AccountId id,
    String name,
    AccountType type,
    UUID userId,
    BigDecimal movementBalance,
    BigDecimal currentBalance,
    BigDecimal netProfitBalance,
    boolean isActive,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    BigDecimal advertisedAnnualRate,
    BigDecimal estimatedAnnualYield) {

  public static AccountDTO.AccountDTOBuilder defaultBuilder(
      final UUID userId, final AccountId accountId, final String name, final AccountType type) {
    return AccountDTO.notUseThisInternalBuilder()
        .userId(userId)
        .id(accountId)
        .name(name)
        .type(type)
        .movementBalance(JBH_ZERO)
        .currentBalance(JBH_ZERO)
        .netProfitBalance(JBH_ZERO)
        .isActive(true)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .advertisedAnnualRate(JBH_ZERO)
        .estimatedAnnualYield(JBH_ZERO);
  }
}
