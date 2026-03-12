package com.jbh.finance.test.application.feature.movement.usecases.delete;

import com.jbh.finance.application.feature.movement.usecases.DeleteMovementUseCase;
import com.jbh.finance.test.testfixtures.builders.UseCaseFixtureBuilder;
import org.junit.jupiter.api.BeforeEach;

public class DeleteMovementUseCaseTest {

  private DeleteMovementUseCase deleteMovementUseCase;

  @BeforeEach
  public void setup() {
    deleteMovementUseCase = UseCaseFixtureBuilder.buildDeleteMovementUseCase();
  }
}
