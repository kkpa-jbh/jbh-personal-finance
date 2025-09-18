package com.jbh.account.application.accounts.dto;

import com.jbh.account.domain.entity.MovementCategoryDomain;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountMovementId;
import com.jbh.account.domain.vo.MovementType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import lombok.Builder;

@Builder
public record MovementDTO(
    AccountMovementId id,
    AccountId accountId,
    MovementType movementType,
    MovementCategoryDomain category,
    BigDecimal movementAmount,
    LocalDate movementDate,
    BigDecimal balanceSnapshot,
    Map<String, Object> metadata) {}
