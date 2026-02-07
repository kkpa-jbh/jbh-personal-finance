package com.jbh.products.infra.adapters.in.rest.movement.response;

import com.jbh.products.application.core.dto.AddBasicMovementDTO;

public record AddBasicMovementResponse(
    ProductResponse account,
    MovementResponse movement,
    MonthlyBalanceResponse monthlyBalance
) {
  public static AddBasicMovementResponse fromDTO(final AddBasicMovementDTO dto) {
    return new AddBasicMovementResponse(
        ProductResponse.fromDTO(dto.account()),
        MovementResponse.fromDTO(dto.movement()),
        MonthlyBalanceResponse.fromDTO(dto.monthlyBalance())
    );
  }
}
