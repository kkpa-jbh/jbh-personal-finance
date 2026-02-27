package com.jbh.finance.infra.adapters.in.rest.movement.response;

import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.domain.category.vo.CategoryTypeVO;
import com.jbh.finance.domain.movement.vo.MovementId;
import com.jbh.finance.domain.movement.vo.MovementMetadata;
import com.jbh.finance.domain.movement.vo.MovementType;
import com.jbh.finance.domain.product.vo.ProductId;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MovementResponse(
    MovementId id,
    ProductId productId,
    MovementType movementType,
    CategoryTypeVO category,
    BigDecimal movementAmount,
    LocalDate movementDate,
    BigDecimal balanceSnapshot,
    MovementMetadata metadata,
    LocalDateTime createdAt,
    String description,
    boolean canBeRemoved) {
  public static MovementResponse fromDTO(final MovementDTO dto) {
    return new MovementResponse(
        dto.id(),
        dto.productId(),
        dto.movementType(),
        dto.category() != null ? dto.category().getCategoryType() : null,
        dto.movementAmount(),
        dto.movementDate(),
        dto.balanceSnapshot(),
        dto.metadata(),
        dto.createdAt(),
        dto.description(),
        dto.canBeRemoved());
  }
}
