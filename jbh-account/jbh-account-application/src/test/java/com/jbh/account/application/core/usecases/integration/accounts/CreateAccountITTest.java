package com.jbh.account.application.core.usecases.integration.accounts;

import static com.jbh.account.application.builders.CommandTestBuilder.createBasicAccountCommand;
import static com.jbh.account.application.builders.CommandTestBuilder.createCDTCommand;
import static com.jbh.account.application.builders.CommandTestBuilder.createCreditCardCommand;
import static com.jbh.account.application.builders.CommandTestBuilder.createInvestmentCommand;
import static com.jbh.account.application.builders.UseCaseBuilder.addMovementUseCase;
import static com.jbh.account.application.builders.UseCaseBuilder.delayTests;
import static com.jbh.account.application.core.usecases.utils.AccountITUtils.assertAccount;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jbh.account.application.builders.AccountEntityBuilder;
import com.jbh.account.application.builders.CommandTestBuilder;
import com.jbh.account.application.builders.UseCaseBuilder;
import com.jbh.account.application.core.dto.AccountDTO;
import com.jbh.account.application.core.mappers.AccountMapper;
import com.jbh.account.application.core.ports.output.account.InMemoryAccountRepository;
import com.jbh.account.application.core.usecases.CreateAccountUseCase;
import com.jbh.account.application.core.vo.commands.CreateAccountCommand;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.exceptions.GenericSpecificationException;
import com.jbh.account.domain.vo.ExpenseCategory;
import com.jbh.account.domain.vo.MovementCategoryDTO;
import com.jbh.account.domain.vo.ProductMetadataKey;
import com.jbh.account.domain.vo.ProductType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CreateAccountITTest {
  static UUID userId = UUID.randomUUID();
  static BigDecimal creditLimit = new BigDecimal("1000000");
  private static CreateAccountUseCase createAccountUseCase;
  private static InMemoryAccountRepository inMemoryAccountRepo;

  @BeforeEach
  public void setUp() {
    createAccountUseCase = UseCaseBuilder.buildCreateAccountUseCase();
    inMemoryAccountRepo = UseCaseBuilder.getAccountRepository();
  }

  @Test
  void accountCreationValidations() throws AccountBusinessException {
    assertThrows(GenericSpecificationException.class, () -> createAccountUseCase.execute(null));
    assertThrows(
        GenericSpecificationException.class,
        () -> createAccountUseCase.execute(createBasicAccountCommand(null, null, null)));
    assertThrows(
        GenericSpecificationException.class,
        () -> createAccountUseCase.execute(createBasicAccountCommand(userId, null, null)));
    assertThrows(
        GenericSpecificationException.class,
        () -> createAccountUseCase.execute(createBasicAccountCommand(userId, "Test", null)));

    // Test 1: Credit Card account WITHOUT required metadata should fail
    final CreateAccountCommand missingTagsCreditCardAccount =
        createBasicAccountCommand(userId, "Test Credit Card", ProductType.CREDIT_CARD);

    final AccountBusinessException exception =
        assertThrows(
            AccountBusinessException.class,
            () -> createAccountUseCase.execute(missingTagsCreditCardAccount),
            "Expected creation to fail when CREDIT_CARD account is missing required metadata");

    assertNotNull(exception.getMessage());

    // Test 2: Investment account WITHOUT BROKER_NAME should fail
    final CreateAccountCommand missingBrokerInvestmentAccount =
        createBasicAccountCommand(userId, "Test Investment", ProductType.INVESTMENT);

    final AccountBusinessException investmentException =
        assertThrows(
            AccountBusinessException.class,
            () -> createAccountUseCase.execute(missingBrokerInvestmentAccount),
            "Expected creation to fail when INVESTMENT account is missing BROKER_NAME");

    assertNotNull(investmentException.getMessage());

    // Test 3: Savings account WITHOUT metadata should succeed (no required metadata)
    final CreateAccountCommand validSavingsAccount =
        createBasicAccountCommand(userId, "Test Savings", ProductType.SAVINGS);

    final AccountDTO savingsAccountDTO = createAccountUseCase.execute(validSavingsAccount);
    assertNotNull(savingsAccountDTO);
    assertEquals("Test Savings", savingsAccountDTO.name());
    assertEquals(ProductType.SAVINGS, savingsAccountDTO.type());

    // Test 4: CDT account WITHOUT metadata should FAILS
    final CreateAccountCommand validCdtAccount =
        createBasicAccountCommand(userId, "Test CDT", ProductType.CDT);

    final AccountDTO cdtAccountDTO = createAccountUseCase.execute(validCdtAccount);
    assertNotNull(cdtAccountDTO);
    assertEquals("Test CDT", cdtAccountDTO.name());
    assertEquals(ProductType.CDT, cdtAccountDTO.type());
  }

  @Test
  void creditCardAccountValidations() {
    final Map<ProductMetadataKey, Object> metadata = new HashMap<>();
    final CreateAccountCommand invalidCommand =
        createCreditCardCommand(userId, "Test CDT", metadata);
    assertThrows(
        AccountBusinessException.class, () -> createAccountUseCase.execute(invalidCommand));
  }

  @Test
  void createInvestmentAccount() throws AccountBusinessException {
    // Investment WITH required metadata should succeed
    final CreateAccountCommand validInvestmentCommand =
        createInvestmentCommand(userId, "Fidelity Portfolio", "Fidelity");

    final AccountDTO investmentAccountDTO = createAccountUseCase.execute(validInvestmentCommand);

    assertNotNull(investmentAccountDTO);
    assertEquals("Fidelity Portfolio", investmentAccountDTO.name());
    assertEquals(ProductType.INVESTMENT, investmentAccountDTO.type());
    assertNotNull(investmentAccountDTO.id());
  }

  @Test
  void createCreditCardWithInvalidCreditLimit() {
    // Credit Card WITH zero credit limit should fail
    final CreateAccountCommand invalidCreditLimitCommand =
        createCreditCardCommand(userId, "Invalid Card", BigDecimal.ZERO, 15);

    assertThrows(
        AccountBusinessException.class,
        () -> createAccountUseCase.execute(invalidCreditLimitCommand),
        "Expected creation to fail when credit limit is zero");
  }

  @Test
  void createCreditCardWithInvalidPaymentDueDay() {
    // Credit Card WITH invalid payment due day (out of range) should fail
    final CreateAccountCommand invalidPaymentDayCommand =
        createCreditCardCommand(userId, "Invalid Card", creditLimit, 32); // Day 32 is invalid

    assertThrows(
        AccountBusinessException.class,
        () -> createAccountUseCase.execute(invalidPaymentDayCommand),
        "Expected creation to fail when payment due day is out of valid range (1-31)");
  }

  @Test
  void createCreditCardAccount() throws AccountBusinessException {
    // Credit Card WITH required metadata should succeed
    final CreateAccountCommand validCreditCardCommand =
        createCreditCardCommand(userId, "LULO Credit Card", creditLimit, 15);
    final LocalDate mvmDate = LocalDate.of(2023, 1, 1);
    final AccountDTO creditCardAccountDTO = createAccountUseCase.execute(validCreditCardCommand);

    assertNotNull(creditCardAccountDTO);
    assertEquals("LULO Credit Card", creditCardAccountDTO.name());
    final var creditCardType = ProductType.CREDIT_CARD;
    assertEquals(creditCardType, creditCardAccountDTO.type());
    assertNotNull(creditCardAccountDTO.id());

    final var personalExpense1 = new BigDecimal("100.00");
    addMovementUseCase.addMovement(
        userId,
        creditCardAccountDTO.id(),
        CommandTestBuilder.createMovement(
            mvmDate, personalExpense1, MovementCategoryDTO.withType(ExpenseCategory.PERSONAL)));

    AccountDTO updatedAccount =
        inMemoryAccountRepo.findByAccountId(creditCardAccountDTO.id()).get();
    assertNotNull(updatedAccount);
    var expectedAccountBuilder =
        AccountEntityBuilder.withBuilder(
            creditCardAccountDTO.id(),
            userId,
            personalExpense1.negate(),
            personalExpense1.negate(),
            creditCardAccountDTO.name(),
            creditCardType);

    AccountDTO expectedAccount = AccountMapper.toDTO(expectedAccountBuilder.build());

    assertAccount(expectedAccount, updatedAccount);

    delayTests();

    final var publicServicesExpense1 = new BigDecimal("500.00");
    addMovementUseCase.addMovement(
        userId,
        creditCardAccountDTO.id(),
        CommandTestBuilder.createMovement(
            mvmDate.plus(1, ChronoUnit.DAYS),
            publicServicesExpense1,
            MovementCategoryDTO.withType(ExpenseCategory.PUBLIC_SERVICES)));

    updatedAccount = inMemoryAccountRepo.findByAccountId(creditCardAccountDTO.id()).get();

    expectedAccountBuilder =
        AccountEntityBuilder.withBuilder(
            creditCardAccountDTO.id(),
            userId,
            personalExpense1.add(publicServicesExpense1).negate(),
            personalExpense1.add(publicServicesExpense1).negate(),
            creditCardAccountDTO.name(),
            creditCardType);

    expectedAccount = AccountMapper.toDTO(expectedAccountBuilder.build());

    assertAccount(expectedAccount, updatedAccount);
  }

  @Test
  public void createCDTAccount() throws AccountBusinessException {

    createAccountUseCase.execute(createCDTCommand(userId, "My CDT", new HashMap<>()));
  }
}
