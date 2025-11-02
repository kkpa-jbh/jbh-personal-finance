package com.jbh.account.application.core.usecases.integration.movements;

import static com.jbh.account.application.builders.CommandTestBuilder.createInitialBalance;
import static com.jbh.account.application.core.usecases.utils.AccountITUtils.assertAccount;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jbh.account.application.builders.AccountEntityBuilder;
import com.jbh.account.application.builders.CommandTestBuilder;
import com.jbh.account.application.builders.UseCaseBuilder;
import com.jbh.account.application.core.dto.AccountDTO;
import com.jbh.account.application.core.mappers.AccountMapper;
import com.jbh.account.application.core.ports.output.account.InMemoryAccountRepository;
import com.jbh.account.application.core.services.monthlybalance.MonthlyBalanceService;
import com.jbh.account.application.core.usecases.AddMovementUseCase;
import com.jbh.account.application.core.usecases.CreateAccountUseCase;
import com.jbh.account.application.movements.ports.output.AccountMovementWriterRepository;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.vo.AccountMetadataKey;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TestMethodOrder(OrderAnnotation.class)
public class RegisterCDTMovementITTest {

  static final UUID userId = UUID.randomUUID();
  private static final Logger LOG = LoggerFactory.getLogger(RegisterCDTMovementITTest.class);
  private static final String name = "CDT";
  private static final YearMonth period = YearMonth.of(2023, 1);
  private static final InMemoryAccountRepository inMemoryAccountRepo =
      UseCaseBuilder.getAccountRepository();
  private static final BigDecimal CDT_INITIAL_BALANCE = new BigDecimal("100.00");
  private static CreateAccountUseCase createAccountUseCase;
  private static AddMovementUseCase addMovementUseCase;
  private static AccountMovementWriterRepository accountMovementRepository;
  private static AccountDTO cdtAccount;
  private MonthlyBalanceService monthlyBalanceService;

  @BeforeAll
  static void beforeAll() {
    UseCaseBuilder.getInMemoryMonthlyBalanceRepos().clearStorage();
    UseCaseBuilder.getAccountRepository().clearStorage();
  }

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    accountMovementRepository = UseCaseBuilder.getAccountMovementWriterRepository();
    createAccountUseCase = UseCaseBuilder.buildCreateAccountUseCase();
    addMovementUseCase = UseCaseBuilder.buildAddMovementUseCase(accountMovementRepository);
    monthlyBalanceService = UseCaseBuilder.buildMonthlyBalanceService();

    UseCaseBuilder.delayTests();
  }

  @Test
  @Order(0)
  void createAccount() throws AccountBusinessException {
    final LocalDate mvmDate = period.atDay(1);
    final Map<AccountMetadataKey, Object> metadata = new HashMap<>();
    metadata.put(AccountMetadataKey.MATURITY_DATE, period.plusMonths(1).atDay(1));
    cdtAccount =
        createAccountUseCase.execute(CommandTestBuilder.createCDTCommand(userId, name, metadata));
    assertNotNull(cdtAccount);
    assertNotNull(cdtAccount.id());

    addMovementUseCase.addMovement(
        userId, cdtAccount.id(), createInitialBalance(mvmDate, CDT_INITIAL_BALANCE));

    final AccountDTO updatedAccount = inMemoryAccountRepo.findByAccountId(cdtAccount.id()).get();
    assertNotNull(updatedAccount);

    final var expectedAccountBuilder =
        AccountEntityBuilder.withBuilder(
            cdtAccount.id(),
            userId,
            CDT_INITIAL_BALANCE,
            CDT_INITIAL_BALANCE,
            cdtAccount.name(),
            cdtAccount.type());

    final AccountDTO expectedAccount = AccountMapper.toDTO(expectedAccountBuilder.build());

    assertAccount(expectedAccount, updatedAccount);

    // Should not be able to add a new income movement to the account
    assertThrows(
        AccountBusinessException.class,
        () -> {
          addMovementUseCase.addMovement(
              userId, cdtAccount.id(), createInitialBalance(mvmDate, CDT_INITIAL_BALANCE));
        });
  }

  @Test
  @Order(2)
  void withdrawalCDTMovement() {
    final LocalDate mvmDate = period.plusMonths(1).atDay(1);
  }
}
