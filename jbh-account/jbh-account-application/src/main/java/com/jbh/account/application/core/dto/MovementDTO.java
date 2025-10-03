package com.jbh.account.application.core.dto;

import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountMovementId;
import com.jbh.account.domain.vo.MovementCategoryDTO;
import com.jbh.account.domain.vo.MovementType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Builder;

@Builder
public record MovementDTO(
    AccountMovementId id,
    AccountId accountId,
    MovementType movementType,
    MovementCategoryDTO category,
    BigDecimal movementAmount,
    LocalDate movementDate,
    BigDecimal balanceSnapshot,
    Map<String, Object> metadata,
    LocalDateTime createdAt,
    String description) {}
