package com.jbh.account.application.core.validation.movement;

import com.jbh.account.domain.vo.MovementType;
import java.util.Map;

public class MovementValidationStrategyFactory {

  private final Map<MovementType, MovementTypeValidationStrategy> strategies;

  public MovementValidationStrategyFactory() {
    this.strategies =
        Map.of(
            MovementType.DEPOSIT, new DepositValidationStrategy(),
            MovementType.WITHDRAWAL, new WithdrawalValidationStrategy());
  }

  public MovementTypeValidationStrategy getStrategy(final MovementType movementType) {
    final MovementTypeValidationStrategy strategy = strategies.get(movementType);
    if (strategy == null) {
      throw new IllegalArgumentException("Unknown movement type: " + movementType);
    }
    return strategy;
  }
}
