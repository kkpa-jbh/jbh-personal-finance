package com.jbh.account.application.accounts.mappers;

import com.jbh.account.application.accounts.dto.MovementDTO;
import com.jbh.account.domain.entity.AccountMovementDomain;

public class MovementMapper {

  public static MovementDTO toDTO(final AccountMovementDomain domain) {
    if (domain == null) {
      return null;
    }

    return MovementDTO.builder()
        .id(domain.getId())
        .accountId(domain.getAccountId())
        .movementType(domain.getMovementType())
        .category(domain.getCategory())
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
        dto.category(),
        dto.movementAmount(),
        dto.movementDate(),
        dto.balanceSnapshot(),
        dto.metadata());
  }
}
