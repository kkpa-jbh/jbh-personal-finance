package com.jbh.account.application.core.validation.movementtype;

import com.jbh.account.domain.vo.MovementType;
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
