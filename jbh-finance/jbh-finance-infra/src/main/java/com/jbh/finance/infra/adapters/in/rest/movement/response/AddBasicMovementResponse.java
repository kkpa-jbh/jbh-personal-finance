package com.jbh.finance.infra.adapters.in.rest.movement.response;

import com.jbh.finance.application.feature.movement.dto.AddBasicMovementDTO;
import com.jbh.finance.infra.adapters.in.rest.balancehistory.response.MonthlyBalanceResponse;
import com.jbh.finance.infra.adapters.in.rest.product.response.ProductResponse;

public record AddBasicMovementResponse(
    ProductResponse productresponse, MovementResponse movement, MonthlyBalanceResponse monthlyBalance) {
  public static AddBasicMovementResponse fromDTO(final AddBasicMovementDTO dto) {
    return new AddBasicMovementResponse(
        ProductResponse.fromDTO(dto.productDTO()),
        MovementResponse.fromDTO(dto.movement()),
        MonthlyBalanceResponse.fromDTO(dto.monthlyBalance()));
  }
}
