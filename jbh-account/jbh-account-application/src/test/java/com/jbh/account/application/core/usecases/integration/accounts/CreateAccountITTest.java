package com.jbh.account.application.core.usecases.integration.accounts;

import static com.jbh.account.application.builders.CommandTestBuilder.createBasicAccountCommand;
import static com.jbh.account.application.builders.CommandTestBuilder.createCreditCardCommand;
import static com.jbh.account.application.builders.CommandTestBuilder.createInvestmentCommand;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jbh.account.application.builders.UseCaseBuilder;
import com.jbh.account.application.core.dto.AccountDTO;
import com.jbh.account.application.core.usecases.CreateAccountUseCase;
import com.jbh.account.application.core.vo.commands.CreateAccountCommand;
import com.jbh.account.domain.exceptions.GenericSpecificationException;
import com.jbh.account.domain.vo.AccountMetadataKey;
import com.jbh.account.domain.vo.AccountType;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CreateAccountITTest {
  static UUID userId = UUID.randomUUID();
  static BigDecimal creditLimit = new BigDecimal("1000000");
  private static CreateAccountUseCase createAccountUseCase;

  @BeforeEach
  public void setUp() {
    createAccountUseCase = UseCaseBuilder.buildCreateAccountUseCase();
  }

  @Test
  void accountCreationValidations() {
    // Test 1: Credit Card account WITHOUT required metadata should fail
    final CreateAccountCommand missingTagsCreditCardAccount =
        createBasicAccountCommand(userId, "Test Credit Card", AccountType.CREDIT_CARD);

    final GenericSpecificationException exception =
        assertThrows(
            GenericSpecificationException.class,
            () -> createAccountUseCase.execute(missingTagsCreditCardAccount),
            "Expected creation to fail when CREDIT_CARD account is missing required metadata");

    assertNotNull(exception.getMessage());

    // Test 2: Investment account WITHOUT BROKER_NAME should fail
    final CreateAccountCommand missingBrokerInvestmentAccount =
        createBasicAccountCommand(userId, "Test Investment", AccountType.INVESTMENT);

    final GenericSpecificationException investmentException =
        assertThrows(
            GenericSpecificationException.class,
            () -> createAccountUseCase.execute(missingBrokerInvestmentAccount),
            "Expected creation to fail when INVESTMENT account is missing BROKER_NAME");

    assertNotNull(investmentException.getMessage());

    // Test 3: Savings account WITHOUT metadata should succeed (no required metadata)
    final CreateAccountCommand validSavingsAccount =
        createBasicAccountCommand(userId, "Test Savings", AccountType.SAVINGS);

    final AccountDTO savingsAccountDTO = createAccountUseCase.execute(validSavingsAccount);
    assertNotNull(savingsAccountDTO);
    assertEquals("Test Savings", savingsAccountDTO.name());
    assertEquals(AccountType.SAVINGS, savingsAccountDTO.type());

    // Test 4: CDT account WITHOUT metadata should succeed (no required metadata)
    final CreateAccountCommand validCdtAccount =
        createBasicAccountCommand(userId, "Test CDT", AccountType.CDT);

    final AccountDTO cdtAccountDTO = createAccountUseCase.execute(validCdtAccount);
    assertNotNull(cdtAccountDTO);
    assertEquals("Test CDT", cdtAccountDTO.name());
    assertEquals(AccountType.CDT, cdtAccountDTO.type());
  }

  @Test
  void creditCardAccountValidations() {
    final Map<AccountMetadataKey, Object> metadata = new HashMap<>();
    final CreateAccountCommand invalidCommand =
        createCreditCardCommand(userId, "Test CDT", metadata);
    assertThrows(
        GenericSpecificationException.class, () -> createAccountUseCase.execute(invalidCommand));
  }

  @Test
  void createCreditCardAccount() {
    // Credit Card WITH required metadata should succeed
    final CreateAccountCommand validCreditCardCommand =
        createCreditCardCommand(userId, "LULO Credit Card", creditLimit, 15);

    final AccountDTO creditCardAccountDTO = createAccountUseCase.execute(validCreditCardCommand);

    assertNotNull(creditCardAccountDTO);
    assertEquals("LULO Credit Card", creditCardAccountDTO.name());
    assertEquals(AccountType.CREDIT_CARD, creditCardAccountDTO.type());
    assertNotNull(creditCardAccountDTO.id());
  }

  @Test
  void createInvestmentAccount() {
    // Investment WITH required metadata should succeed
    final CreateAccountCommand validInvestmentCommand =
        createInvestmentCommand(userId, "Fidelity Portfolio", "Fidelity");

    final AccountDTO investmentAccountDTO = createAccountUseCase.execute(validInvestmentCommand);

    assertNotNull(investmentAccountDTO);
    assertEquals("Fidelity Portfolio", investmentAccountDTO.name());
    assertEquals(AccountType.INVESTMENT, investmentAccountDTO.type());
    assertNotNull(investmentAccountDTO.id());
  }

  @Test
  void createCreditCardWithInvalidCreditLimit() {
    // Credit Card WITH zero credit limit should fail
    final CreateAccountCommand invalidCreditLimitCommand =
        createCreditCardCommand(userId, "Invalid Card", BigDecimal.ZERO, 15);

    assertThrows(
        GenericSpecificationException.class,
        () -> createAccountUseCase.execute(invalidCreditLimitCommand),
        "Expected creation to fail when credit limit is zero");
  }

  @Test
  void createCreditCardWithInvalidPaymentDueDay() {
    // Credit Card WITH invalid payment due day (out of range) should fail
    final CreateAccountCommand invalidPaymentDayCommand =
        createCreditCardCommand(userId, "Invalid Card", creditLimit, 32); // Day 32 is invalid

    assertThrows(
        GenericSpecificationException.class,
        () -> createAccountUseCase.execute(invalidPaymentDayCommand),
        "Expected creation to fail when payment due day is out of valid range (1-31)");
  }
}
