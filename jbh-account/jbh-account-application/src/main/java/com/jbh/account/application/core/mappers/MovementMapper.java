package com.jbh.account.application.core.mappers;

import com.jbh.account.application.core.dto.MovementDTO;
import com.jbh.account.domain.entity.AccountMovementDomain;

public final class MovementMapper {

  private MovementMapper() {}

  public static MovementDTO toDTO(final AccountMovementDomain domain) {
    if (domain == null) {
      return null;
    }

    return MovementDTO.builder()
        .id(domain.getId())
        .accountId(domain.getAccountId())
        .movementType(domain.getMovementType())
        .category(CategoryMapper.toDTO(domain.getCategory()))
        .movementAmount(domain.getMovementAmount())
        .movementDate(domain.getMovementDate())
        .balanceSnapshot(domain.getBalanceSnapshot())
        .metadata(domain.getMetadata())
        .build();
  }

  public static AccountMovementDomain toDomain(final MovementDTO dto) {
    if (dto == null) {
      return null;
    }

    return new AccountMovementDomain(
        dto.id(),
        dto.accountId(),
        dto.movementType(),
        CategoryMapper.toDomain(dto.category()),
        dto.movementAmount(),
        dto.movementDate(),
        dto.balanceSnapshot(),
        dto.metadata());
  }
}
