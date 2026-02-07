package com.jbh.products.infra.adapters.in.rest.movement.response;

import com.jbh.products.application.core.dto.AddBasicMovementDTO;
import com.jbh.products.infra.adapters.in.rest.balancehistory.response.MonthlyBalanceResponse;
import com.jbh.products.infra.adapters.in.rest.product.response.ProductResponse;

public record AddBasicMovementResponse(
    ProductResponse account, MovementResponse movement, MonthlyBalanceResponse monthlyBalance) {
  public static AddBasicMovementResponse fromDTO(final AddBasicMovementDTO dto) {
    return new AddBasicMovementResponse(
        ProductResponse.fromDTO(dto.account()),
        MovementResponse.fromDTO(dto.movement()),
        MonthlyBalanceResponse.fromDTO(dto.monthlyBalance()));
  }
}
