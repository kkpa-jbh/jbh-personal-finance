package com.jbh.finance.infra.adapters.in.rest.movement.response;

import com.jbh.finance.application.feature.movement.dto.LiquidationResultDTO;

public record LiquidationResultResponse(boolean valid) {
  public static LiquidationResultResponse fromDTO(final LiquidationResultDTO dto) {
    return new LiquidationResultResponse(dto.valid());
  }
}
