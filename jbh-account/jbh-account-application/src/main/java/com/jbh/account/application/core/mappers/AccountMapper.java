package com.jbh.account.application.core.mappers;

import com.jbh.account.application.core.dto.AccountDTO;
import com.jbh.account.domain.entity.AccountDomain;

public final class AccountMapper {

  private AccountMapper() {}

  public static AccountDTO toDTO(final AccountDomain domain) {
    if (domain == null) {
      return null;
    }

    return new AccountDTO(
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

  public static AccountDomain toDomain(final AccountDTO dto) {
    if (dto == null) {
      return null;
    }

    final AccountDomain domain;
    domain =
        new AccountDomain(
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
            dto.metadata().asMap());

    return domain;
  }
}
