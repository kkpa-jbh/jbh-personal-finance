package com.jbh.products.infra.adapters.in.rest.movement.response;

import com.jbh.products.application.core.dto.MovementDTO;
import com.jbh.products.domain.movement.vo.AccountMovementMetadata;
import com.jbh.products.domain.movement.vo.MovementCategoryVO;
import com.jbh.products.domain.movement.vo.MovementId;
import com.jbh.products.domain.movement.vo.MovementType;
import com.jbh.products.domain.product.vo.ProductId;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MovementResponse(
    MovementId id,
    ProductId accountId,
    MovementType movementType,
    MovementCategoryVO category,
    BigDecimal movementAmount,
    LocalDate movementDate,
    BigDecimal balanceSnapshot,
    AccountMovementMetadata metadata,
    LocalDateTime createdAt,
    String description
) {
  public static MovementResponse fromDTO(final MovementDTO dto) {
    return new MovementResponse(
        dto.id(), dto.accountId(), dto.movementType(),
        dto.category(),
        dto.movementAmount(), dto.movementDate(), dto.balanceSnapshot(),
        dto.metadata(), dto.createdAt(), dto.description()
    );
  }
}
