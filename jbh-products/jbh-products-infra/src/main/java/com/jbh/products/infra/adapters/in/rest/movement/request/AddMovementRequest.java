package com.jbh.products.infra.adapters.in.rest.movement.request;

import com.jbh.products.domain.movement.vo.MovementType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record AddMovementRequest(
    LocalDate entryDate,
    BigDecimal totalAmount,
    BigDecimal balanceSnapshot,
    MovementType movementType,
    String categoryName,
    String description) {}
