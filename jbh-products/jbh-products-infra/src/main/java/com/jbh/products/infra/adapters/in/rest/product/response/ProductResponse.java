package com.jbh.products.infra.adapters.in.rest.product.response;

import com.jbh.products.application.core.dto.ProductDTO;
import com.jbh.products.domain.vo.ProductId;
import com.jbh.products.domain.vo.ProductMetadata;
import com.jbh.products.domain.vo.ProductType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProductResponse(
    ProductId id,
    String name,
    ProductType type,
    UUID userId,
    BigDecimal movementBalance,
    BigDecimal currentBalance,
    BigDecimal netProfitBalance,
    boolean isActive,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    BigDecimal netGrowthRate,
    ProductMetadata metadata
) {
  public static ProductResponse fromDTO(final ProductDTO dto) {
    return new ProductResponse(
        dto.id(), dto.name(), dto.type(), dto.userId(),
        dto.movementBalance(), dto.currentBalance(), dto.netProfitBalance(),
        dto.isActive(), dto.createdAt(), dto.updatedAt(),
        dto.netGrowthRate(), dto.metadata()
    );
  }
}
