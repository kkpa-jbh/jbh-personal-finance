package com.jbh.account.application.core.usecases.integration.accounts;

import com.jbh.account.application.builders.UseCaseBuilder;
import com.jbh.account.application.core.usecases.CreateAccountUseCase;
import org.junit.jupiter.api.BeforeEach;

public class CreateAccountITTest {

  private static CreateAccountUseCase createAccountUseCase;

  @BeforeEach
  public void setUp() {

    createAccountUseCase = UseCaseBuilder.buildCreateAccountUseCase();
  }
}
