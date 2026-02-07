package com.jbh.products.application.core.mappers;

import com.jbh.products.application.core.dto.ProductDTO;
import com.jbh.products.domain.product.ProductDomain;

public final class AccountMapper {

  private AccountMapper() {}

  public static ProductDTO toDTO(final ProductDomain domain) {
    if (domain == null) {
      return null;
    }

    return new ProductDTO(
        domain.getId(),
        domain.getName(),
        domain.getType(),
        domain.getUserId(),
        domain.getMovementBalance(),
        domain.getCurrentBalance(),
        domain.getNetProfitBalance(),
        domain.isActive(),
        domain.getCreatedAt(),
        domain.getUpdatedAt(),
        domain.getNetGrowthRate(),
        domain.getMetadata());
  }

  public static ProductDomain toDomain(final ProductDTO dto) {
    if (dto == null) {
      return null;
    }

    final ProductDomain domain;
    domain =
        new ProductDomain(
            dto.id(),
            dto.name(),
            dto.type(),
            dto.userId(),
            dto.movementBalance(),
            dto.currentBalance(),
            dto.netProfitBalance(),
            dto.isActive(),
            dto.createdAt(),
            dto.updatedAt(),
            dto.netGrowthRate(),
            dto.metadata());

    return domain;
  }
}
