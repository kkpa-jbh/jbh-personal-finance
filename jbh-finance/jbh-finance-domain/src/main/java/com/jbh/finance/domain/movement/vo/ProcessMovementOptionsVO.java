package com.jbh.finance.domain.movement.vo;

import com.jbh.finance.domain.shared.vo.EntityOperationVO;

public record ProcessMovementOptionsVO(
    boolean isMonthOfficiallyReported, EntityOperationVO operation) {

  public ProcessMovementOptionsVO(final EntityOperationVO operation) {
    this(false, operation);
  }
}
