package com.jbh.finance.infra.adapters.in.rest.movement.request;

import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.finance.domain.movement.vo.MovementType;
import com.jbh.finance.infra.adapters.in.rest.category.request.CategoryRequest;
import java.math.BigDecimal;
import java.time.LocalDate;

public record AddMovementRequest(
    LocalDate entryDate,
    BigDecimal totalAmount,
    BigDecimal balanceSnapshot,
    MovementType movementType,
    CategoryRequest categoryRequest,
    String description) {

  public void validate() {
    if (movementType != null
        && movementType == MovementType.BALANCE_SNAPSHOT
        && categoryRequest != null) {
      throw new GenericSpecificationException("Invalid category");
    }
  }
}
