package com.jbh.account.domain.entity;

import static com.jbh.account.domain.utils.MoneyUtils.JBH_ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jbh.account.domain.entity.AccountDomain.AccountMetadataKey;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountMovementId;
import com.jbh.account.domain.vo.AccountType;
import com.jbh.account.domain.vo.ExpenseCategory;
import com.jbh.account.domain.vo.IncomeCategory;
import com.jbh.account.domain.vo.MovementType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class AccountDomainTest {

  static UUID userId = UUID.randomUUID();
  AccountDomain accountDomain;
  LocalDate today = LocalDate.now();

  @Test
  public void shouldCreateAccountWithBasicMovementForExistingId() {
    accountDomain =
        AccountDomain.withBasicMovementForExisting(
            AccountId.generate(), userId, JBH_ZERO, JBH_ZERO);

    assert accountDomain.getId() != null;
    assertDefaultAccountBalances(accountDomain);
  }

  private void assertDefaultAccountBalances(final AccountDomain accountDomain) {
    assertNotNull(accountDomain.getId());
    assertEquals(JBH_ZERO, accountDomain.getMovementBalance());
    assertEquals(JBH_ZERO, accountDomain.getCurrentBalance());
    assertEquals(JBH_ZERO, accountDomain.getNetProfitBalance());
    assertTrue(accountDomain.isActive());
  }

  @Test
  public void shouldCreateWithMinimumDataForCreation() {
    final String name = "Test Account";
    accountDomain =
        AccountDomain.withMinimumDataForCreation(name, AccountType.SAVINGS, UUID.randomUUID());
    assert accountDomain.getId() != null;
    assertRequiredAccount(accountDomain);
  }

  private void assertRequiredAccount(final AccountDomain accountDomain) {
    assertNotNull(accountDomain.getId());
    assertNotNull(accountDomain.getName());
    assertNotNull(accountDomain.getType());
    assertNotNull(accountDomain.getUserId());
    assertNotNull(accountDomain.getCreatedAt());
    assertNotNull(accountDomain.getCurrentBalance());
    assertNotNull(accountDomain.getMovementBalance());
    assertNotNull(accountDomain.getNetProfitBalance());
  }

  @Test
  public void shouldCreateWithBasicMovementForExisting() {
    final var movementBalance = new BigDecimal("100.00");
    final var currentBalance = new BigDecimal("200.00");
    final AccountDomain accountDomain =
        AccountDomain.withBasicMovementForExisting(
            AccountId.generate(), userId, movementBalance, currentBalance);

    assertEquals(movementBalance, accountDomain.getMovementBalance());
    assertEquals(currentBalance, accountDomain.getCurrentBalance());
    assertEquals(JBH_ZERO, accountDomain.getNetProfitBalance());
  }

  @Test
  public void shouldCreateWithConstructor() throws AccountBusinessException {
    final var accountDomain =
        new AccountDomain(
            AccountId.generate(),
            "name",
            AccountType.SAVINGS,
            userId,
            JBH_ZERO,
            JBH_ZERO,
            JBH_ZERO,
            true,
            LocalDateTime.now(),
            LocalDateTime.now(),
            JBH_ZERO,
            new HashMap<>());

    assertNotNull(accountDomain.getId());

    final var movementAmount = new BigDecimal("100.00");
    final AccountMovementDomain movement =
        new AccountMovementDomain(
            AccountMovementId.generate(),
            accountDomain.getId(),
            MovementType.DEPOSIT,
            MovementCategoryDomain.withCategoryType(IncomeCategory.OTHER),
            movementAmount,
            LocalDate.now(),
            null,
            new HashMap<>());

    accountDomain.syncBalancesByMovement(movement, false);

    assertNotNull(accountDomain.getId());
    assertFalse(accountDomain.isFullyWithdrawn());
    assertFalse(accountDomain.hasMetadata(AccountMetadataKey.FULLY_WITHDRAWN));

    final AccountMovementDomain withdrawalMovement =
        new AccountMovementDomain(
            AccountMovementId.generate(),
            accountDomain.getId(),
            MovementType.WITHDRAWAL,
            MovementCategoryDomain.withCategoryType(ExpenseCategory.SOCIAL_SECURITY),
            new BigDecimal("-100.00"),
            LocalDate.now(),
            JBH_ZERO,
            new HashMap<>());
    accountDomain.syncBalancesByMovement(withdrawalMovement, false);
    assertTrue(accountDomain.isFullyWithdrawn());
    assertTrue(accountDomain.hasMetadata(AccountMetadataKey.FULLY_WITHDRAWN));

    accountDomain.getUpdatedAt();
    accountDomain.getCreatedAt();
    accountDomain.getMoneyGrowthCalculator();
    accountDomain.getMetadata();

    final AccountMovementDomain unknownMovement =
        new AccountMovementDomain(
            AccountMovementId.generate(),
            AccountId.generate(),
            MovementType.DEPOSIT,
            MovementCategoryDomain.withCategoryType(IncomeCategory.OTHER),
            movementAmount,
            LocalDate.now(),
            JBH_ZERO,
            new HashMap<>());
    assertThrows(
        AccountBusinessException.class,
        () -> accountDomain.syncBalancesByMovement(unknownMovement, false));
  }
}
