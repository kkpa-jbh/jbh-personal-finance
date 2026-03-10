package com.jbh.finance.infra.adapters.in.rest.movement.response;

import com.jbh.finance.application.feature.movement.dto.AddMovementResultDTO;
import com.jbh.finance.infra.adapters.in.rest.balancehistory.response.MonthlyBalanceResponse;
import com.jbh.finance.infra.adapters.in.rest.product.response.ProductResponse;

public record AddMovementResponse(
    ProductResponse productresponse,
    MovementResponse movement,
    MonthlyBalanceResponse monthlyBalance) {
  public static AddMovementResponse fromDTO(final AddMovementResultDTO dto) {
    return new AddMovementResponse(
        ProductResponse.fromDTO(dto.productDTO()),
        MovementResponse.fromDTO(dto.movement()),
        MonthlyBalanceResponse.fromDTO(dto.monthlyBalance()));
  }
}
