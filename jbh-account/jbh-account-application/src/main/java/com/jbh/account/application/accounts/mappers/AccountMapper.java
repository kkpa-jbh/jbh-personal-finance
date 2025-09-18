package com.jbh.account.application.accounts.mappers;

import com.jbh.account.application.accounts.dto.AccountDTO;
import com.jbh.account.domain.entity.AccountDomain;

public class AccountMapper {

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
        domain.getProfitBalance(),
        domain.isActive(),
        domain.getCreatedAt(),
        domain.getUpdatedAt(),
        domain.getAdvertisedAnnualRate(),
        domain.getEstimatedAnnualYield());
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
            dto.profitBalance(),
            dto.isActive(),
            dto.createdAt(),
            dto.updatedAt(),
            dto.advertisedAnnualRate(),
            dto.estimatedAnnualYield());

    return domain;
  }
}
