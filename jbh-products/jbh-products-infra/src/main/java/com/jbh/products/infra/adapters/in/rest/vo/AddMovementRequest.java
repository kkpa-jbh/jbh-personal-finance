package com.jbh.products.infra.adapters.in.rest.vo;

import com.jbh.products.domain.vo.MovementType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record AddMovementRequest(
    LocalDate entryDate,
    BigDecimal totalAmount,
    BigDecimal balanceSnapshot,
    MovementType movementType,
    String categoryName) {}
