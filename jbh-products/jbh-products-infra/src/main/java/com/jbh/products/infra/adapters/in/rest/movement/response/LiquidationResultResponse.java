package com.jbh.products.infra.adapters.in.rest.movement.response;

import com.jbh.products.application.core.dto.LiquidationResultDTO;

public record LiquidationResultResponse(boolean valid) {
  public static LiquidationResultResponse fromDTO(final LiquidationResultDTO dto) {
    return new LiquidationResultResponse(dto.valid());
  }
}
