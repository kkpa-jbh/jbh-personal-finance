package com.jbh.account.domain.entity;

import static com.jbh.account.domain.utils.MoneyUtils.JBH_ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class AccountDomainTest {

  AccountDomain accountDomain;

  LocalDate today = LocalDate.now();

  @Test
  public void shouldCreateAccountWithBasicMovementForExistingId() {
    accountDomain =
        AccountDomain.withBasicMovementForExisting(AccountId.generate(), JBH_ZERO, JBH_ZERO);

    assert accountDomain.getId() != null;
    assertDefaultAccountBalances(accountDomain);
  }

  private void assertDefaultAccountBalances(final AccountDomain accountDomain) {
    assertNotNull(accountDomain.getId());
    assertEquals(JBH_ZERO, accountDomain.getMovementBalance());
    assertEquals(JBH_ZERO, accountDomain.getCurrentBalance());
    assertEquals(JBH_ZERO, accountDomain.getProfitBalance());
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
    assertNotNull(accountDomain.getProfitBalance());
  }

  @Test
  public void shouldCreateWithBasicMovementForExisting() {
    final var movementBalance = new BigDecimal("100.00");
    final var currentBalance = new BigDecimal("200.00");
    final AccountDomain accountDomain =
        AccountDomain.withBasicMovementForExisting(
            AccountId.generate(), movementBalance, currentBalance);

    assertEquals(movementBalance, accountDomain.getMovementBalance());
    assertEquals(currentBalance, accountDomain.getCurrentBalance());
    assertEquals(JBH_ZERO, accountDomain.getProfitBalance());
  }

  @Test
  public void shouldSyncSingleBalance() {
    final var movementBalance = new BigDecimal("100.00");
    final var currentBalance = new BigDecimal("200.00");
    final AccountDomain accountDomain =
        AccountDomain.withBasicMovementForExisting(
            AccountId.generate(), movementBalance, currentBalance);

    final var totalAmount = new BigDecimal("100.00");
    final var balanceSnapshot = new BigDecimal("210.00");
    final var newMovement =
        AccountMovementDomain.withBalances(
            accountDomain.getId(), today, totalAmount, balanceSnapshot);
    accountDomain.syncBalances(Collections.singletonList(newMovement));

    assertEquals(movementBalance.add(totalAmount), accountDomain.getMovementBalance());
    assertEquals(balanceSnapshot, accountDomain.getCurrentBalance());
    assertEquals(new BigDecimal("10.00"), accountDomain.getProfitBalance());
  }

  @Test
  public void shouldSyncMultiBalances() {
    int totalMovements = 2;
    final var movementBalance = new BigDecimal("100.00");
    final var currentBalance = new BigDecimal("100.00");
    final AccountDomain accountDomain =
        AccountDomain.withBasicMovementForExisting(
            AccountId.generate(), movementBalance, currentBalance);

    final var amount1 = new BigDecimal("100.00");
    final var balance1 = new BigDecimal("205.00");

    final var newMovement1 =
        AccountMovementDomain.withBalances(
            accountDomain.getId(), today.plusDays(-1 * --totalMovements), amount1, balance1);

    final var amount2 = new BigDecimal("-50.00");
    final var balance2 = new BigDecimal("155.00");

    final var newMovement2 =
        AccountMovementDomain.withBalances(
            accountDomain.getId(), today.plusDays(-1 * --totalMovements), amount2, balance2);

    accountDomain.syncBalances(List.of(newMovement1, newMovement2));

    assertEquals(new BigDecimal("150.00"), accountDomain.getMovementBalance());
    assertEquals(new BigDecimal("155.00"), accountDomain.getCurrentBalance());
    assertEquals(new BigDecimal("5.00"), accountDomain.getProfitBalance());
  }
}
