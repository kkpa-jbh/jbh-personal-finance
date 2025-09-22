package com.jbh.account.application.core.dto;

import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AccountDTO(
    AccountId id,
    String name,
    AccountType type,
    UUID userId,
    BigDecimal movementBalance,
    BigDecimal currentBalance,
    BigDecimal profitBalance,
    boolean isActive,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    BigDecimal advertisedAnnualRate,
    BigDecimal estimatedAnnualYield) {}
