package com.jbh.products.application.feature.movement.validation.movement_type;

import com.jbh.products.domain.movement.vo.MovementType;
import java.util.Map;

public class MovementValidationStrategyFactory {

  private final Map<MovementType, MovementTypeValidatorStrategy> strategies;

  public MovementValidationStrategyFactory() {
    this.strategies =
        Map.of(
            MovementType.DEPOSIT, new DepositValidationStrategy(),
            MovementType.WITHDRAWAL, new WithdrawalValidationStrategy());
  }

  public MovementTypeValidatorStrategy getStrategy(final MovementType movementType) {
    final MovementTypeValidatorStrategy strategy = strategies.get(movementType);
    if (strategy == null) {
      throw new IllegalArgumentException("Unknown movement type: " + movementType);
    }
    return strategy;
  }
}
