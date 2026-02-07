package com.jbh.products.application.core.usecases.integration.accounts;

import static com.jbh.products.application.builders.CommandTestBuilder.createBasicAccountCommand;
import static com.jbh.products.application.builders.CommandTestBuilder.createCDTCommand;
import static com.jbh.products.application.builders.CommandTestBuilder.createCreditCardCommand;
import static com.jbh.products.application.builders.CommandTestBuilder.createInvestmentCommand;
import static com.jbh.products.application.builders.CommandTestBuilder.createLoanCommand;
import static com.jbh.products.application.builders.CommandTestBuilder.createRealEstateCommand;
import static com.jbh.products.application.builders.UseCaseBuilder.addMovementUseCase;
import static com.jbh.products.application.builders.UseCaseBuilder.delayTests;
import static com.jbh.products.application.core.usecases.utils.AccountITUtils.assertAccount;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.products.application.builders.AccountEntityBuilder;
import com.jbh.products.application.builders.CommandTestBuilder;
import com.jbh.products.application.builders.UseCaseBuilder;
import com.jbh.products.application.core.dto.ProductDTO;
import com.jbh.products.application.core.mappers.AccountMapper;
import com.jbh.products.application.core.ports.output.account.InMemoryAccountRepository;
import com.jbh.products.application.core.usecases.CreateProductUseCase;
import com.jbh.products.application.core.vo.commands.CreateProductCommand;
import com.jbh.products.domain.movement.vo.ExpenseCategory;
import com.jbh.products.domain.movement.vo.MovementCategoryVO;
import com.jbh.products.domain.product.vo.ProductMetadata;
import com.jbh.products.domain.product.vo.ProductType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CreateProductITTest {
  static UUID userId = UUID.randomUUID();
  static BigDecimal creditLimit = new BigDecimal("1000000");
  private static CreateProductUseCase createAccountUseCase;
  private static InMemoryAccountRepository inMemoryAccountRepo;

  @BeforeEach
  public void setUp() {
    createAccountUseCase = UseCaseBuilder.buildCreateAccountUseCase();
    inMemoryAccountRepo = UseCaseBuilder.getAccountRepository();
  }

  @Test
  void accountCreationValidations() throws BusinessException {
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
    final CreateProductCommand missingTagsCreditCardAccount =
        createBasicAccountCommand(userId, "Test Credit Card", ProductType.CREDIT_CARD);

    final BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> createAccountUseCase.execute(missingTagsCreditCardAccount),
            "Expected creation to fail when CREDIT_CARD account is missing required metadata");

    assertNotNull(exception.getMessage());

    // Test 2: Investment account WITHOUT BROKER_NAME should fail
    final CreateProductCommand missingBrokerInvestmentAccount =
        createBasicAccountCommand(userId, "Test Investment", ProductType.INVESTMENT);

    final BusinessException investmentException =
        assertThrows(
            BusinessException.class,
            () -> createAccountUseCase.execute(missingBrokerInvestmentAccount),
            "Expected creation to fail when INVESTMENT account is missing BROKER_NAME");

    assertNotNull(investmentException.getMessage());

    // Test 3: Savings account WITHOUT metadata should succeed (no required metadata)
    final CreateProductCommand validSavingsAccount =
        createBasicAccountCommand(userId, "Test Savings", ProductType.SAVINGS);

    final ProductDTO savingsAccountDTO = createAccountUseCase.execute(validSavingsAccount);
    assertNotNull(savingsAccountDTO);
    assertEquals("Test Savings", savingsAccountDTO.name());
    assertEquals(ProductType.SAVINGS, savingsAccountDTO.type());

    // Test 4: CDT account WITHOUT metadata should FAILS
    final CreateProductCommand validCdtAccount =
        createBasicAccountCommand(userId, "Test CDT", ProductType.CDT);

    final ProductDTO cdtAccountDTO = createAccountUseCase.execute(validCdtAccount);
    assertNotNull(cdtAccountDTO);
    assertEquals("Test CDT", cdtAccountDTO.name());
    assertEquals(ProductType.CDT, cdtAccountDTO.type());
  }

  @Test
  void creditCardAccountValidations() {
    final CreateProductCommand invalidCommand =
        createCreditCardCommand(userId, "Test CDT", ProductMetadata.empty());
    assertThrows(BusinessException.class, () -> createAccountUseCase.execute(invalidCommand));
  }

  @Test
  void createInvestmentAccount() throws BusinessException {
    // Investment WITH required metadata should succeed
    final CreateProductCommand validInvestmentCommand =
        createInvestmentCommand(userId, "Fidelity Portfolio", "Fidelity");

    final ProductDTO investmentAccountDTO = createAccountUseCase.execute(validInvestmentCommand);

    assertNotNull(investmentAccountDTO);
    assertEquals("Fidelity Portfolio", investmentAccountDTO.name());
    assertEquals(ProductType.INVESTMENT, investmentAccountDTO.type());
    assertNotNull(investmentAccountDTO.id());
  }

  @Test
  void createCreditCardWithInvalidCreditLimit() {
    // Credit Card WITH zero credit limit should fail
    final CreateProductCommand invalidCreditLimitCommand =
        createCreditCardCommand(userId, "Invalid Card", BigDecimal.ZERO, 15);

    assertThrows(
        BusinessException.class,
        () -> createAccountUseCase.execute(invalidCreditLimitCommand),
        "Expected creation to fail when credit limit is zero");
  }

  @Test
  void createCreditCardWithInvalidPaymentDueDay() {
    // Credit Card WITH invalid payment due day (out of range) should fail
    final CreateProductCommand invalidPaymentDayCommand =
        createCreditCardCommand(userId, "Invalid Card", creditLimit, 32); // Day 32 is invalid

    assertThrows(
        BusinessException.class,
        () -> createAccountUseCase.execute(invalidPaymentDayCommand),
        "Expected creation to fail when payment due day is out of valid range (1-31)");
  }

  @Test
  void createCreditCardAccount() throws BusinessException {
    // Credit Card WITH required metadata should succeed
    final CreateProductCommand validCreditCardCommand =
        createCreditCardCommand(userId, "LULO Credit Card", creditLimit, 15);
    final LocalDate mvmDate = LocalDate.of(2023, 1, 1);
    final ProductDTO creditCardAccountDTO = createAccountUseCase.execute(validCreditCardCommand);

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
            mvmDate, personalExpense1, MovementCategoryVO.withType(ExpenseCategory.PERSONAL)));

    ProductDTO updatedAccount =
        inMemoryAccountRepo.findByProductId(creditCardAccountDTO.id()).get();
    assertNotNull(updatedAccount);
    var expectedAccountBuilder =
        AccountEntityBuilder.withBuilder(
            creditCardAccountDTO.id(),
            userId,
            personalExpense1.negate(),
            personalExpense1.negate(),
            creditCardAccountDTO.name(),
            creditCardType);

    ProductDTO expectedAccount = AccountMapper.toDTO(expectedAccountBuilder.build());

    assertAccount(expectedAccount, updatedAccount);

    delayTests();

    final var publicServicesExpense1 = new BigDecimal("500.00");
    addMovementUseCase.addMovement(
        userId,
        creditCardAccountDTO.id(),
        CommandTestBuilder.createMovement(
            mvmDate.plus(1, ChronoUnit.DAYS),
            publicServicesExpense1,
            MovementCategoryVO.withType(ExpenseCategory.PUBLIC_SERVICES)));

    updatedAccount = inMemoryAccountRepo.findByProductId(creditCardAccountDTO.id()).get();

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
  public void createCDTAccount() throws BusinessException {

    createAccountUseCase.execute(createCDTCommand(userId, "My CDT", ProductMetadata.empty()));
  }

  @Test
  public void createLoanProductMissingValidations() throws BusinessException {
    final ProductMetadata metadata = ProductMetadata.empty();
    assertThrows(
        BusinessException.class,
        () -> createAccountUseCase.execute(createLoanCommand(userId, "Bancolombia", metadata)));

    final var expected = new BigDecimal("100.00");
    metadata.findLoanMetadata().putPrincipalAmount(expected);
    assertThrows(
        BusinessException.class,
        () -> createAccountUseCase.execute(createLoanCommand(userId, "Bancolombia", metadata)));

    metadata.findLoanMetadata().putTotalAmountPaid(expected);
    assertThrows(
        BusinessException.class,
        () -> createAccountUseCase.execute(createLoanCommand(userId, "Bancolombia", metadata)));

    metadata.findLoanMetadata().putPayoffAmountToday(expected);
    final ProductDTO createdLoan =
        createAccountUseCase.execute(createLoanCommand(userId, "Bancolombia", metadata));

    assertNotNull(createdLoan);
    assertEquals(expected, createdLoan.metadata().findLoanMetadata().getPrincipalAmount());
    assertEquals(expected, createdLoan.metadata().findLoanMetadata().getTotalAmountPaid());
    assertEquals(expected, createdLoan.metadata().findLoanMetadata().getPayoffAmountToday());
  }

  @Test
  public void createRealEstateProductMissingValidations() throws BusinessException {
    final ProductMetadata metadata = ProductMetadata.empty();
    BusinessException error =
        assertThrows(BusinessException.class, () -> createRealEstateProduct(metadata));
    assertNotNull(error);

    // Adding PURCHASE_DATE
    metadata.findRealEstateMetadata().putPurchaseDate(LocalDate.now());
    error = assertThrows(BusinessException.class, () -> createRealEstateProduct(metadata));

    // Adding REAL_ESTATE_PURCHASE_PRICE
    metadata.findRealEstateMetadata().putPurchasePrice(new BigDecimal(1000));
    error = assertThrows(BusinessException.class, () -> createRealEstateProduct(metadata));

    // Adding REAL_ESTATE_PROPERTY_SIZE
    metadata.findRealEstateMetadata().putPropertySize(new BigDecimal("100.00"));
    error = assertThrows(BusinessException.class, () -> createRealEstateProduct(metadata));

    // Adding REAL_ESTATE_FINANCED_AMOUNT
    metadata.findRealEstateMetadata().putFinancedAmount(BigDecimal.ONE);
    error = assertThrows(BusinessException.class, () -> createRealEstateProduct(metadata));

    // Adding REAL_ESTATE_DOWN_PAYMENT_PERCENTAGE
    metadata.findRealEstateMetadata().putDownPaymentPercentage(new BigDecimal("171.00"));
    error = assertThrows(BusinessException.class, () -> createRealEstateProduct(metadata));

    assertNotNull(error);
    System.out.println(error);
  }

  private static ProductDTO createRealEstateProduct(final ProductMetadata metadata)
      throws BusinessException {
    return createAccountUseCase.execute(createRealEstateCommand(userId, metadata));
  }

  @Test
  public void createRealEstateProductSuccesfully() throws BusinessException {
    final ProductMetadata metadata = ProductMetadata.empty();

    // Adding PURCHASE_DATE
    metadata.findRealEstateMetadata().putPurchaseDate(LocalDate.now());

    // Adding REAL_ESTATE_PURCHASE_PRICE
    metadata.findRealEstateMetadata().putPurchasePrice(new BigDecimal(1000));

    // Adding REAL_ESTATE_PROPERTY_SIZE
    metadata.findRealEstateMetadata().putPropertySize(new BigDecimal("100.00"));

    // Adding REAL_ESTATE_FINANCED_AMOUNT
    metadata.findRealEstateMetadata().putFinancedAmount(BigDecimal.ONE);

    // Adding REAL_ESTATE_DOWN_PAYMENT_PERCENTAGE
    metadata.findRealEstateMetadata().putDownPaymentPercentage(new BigDecimal("30.00"));

    // Adding REAL_ESTATE_DOWN_PAYMENT_PAID_TO_DATE
    metadata.findRealEstateMetadata().putDownPaymentPaidToDate(BigDecimal.ONE);

    final ProductDTO realEstateAccount = createRealEstateProduct(metadata);
    assertNotNull(realEstateAccount);
    assertEquals(
        metadata.findRealEstateMetadata().getDownPaymentAmount(), new BigDecimal("30000.00"));
  }
}
