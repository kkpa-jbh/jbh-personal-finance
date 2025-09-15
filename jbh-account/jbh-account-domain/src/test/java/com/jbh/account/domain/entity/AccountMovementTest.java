package com.jbh.account.domain.entity;

import static com.jbh.account.domain.utils.MoneyUtils.JBH_ZERO;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jbh.account.domain.exceptions.GenericSpecificationException;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.MovementType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AccountMovementTest {
  AccountDomain accountCeroBalance;
  AccountDomain account100Balance;

  LocalDate today = LocalDate.now();
  LocalDateTime importedAt = LocalDateTime.now();

  @BeforeEach
  public void setUp() {
    accountCeroBalance =
        AccountDomain.withBasicMovementForExisting(AccountId.generate(), JBH_ZERO, JBH_ZERO);

    account100Balance =
        AccountDomain.withBasicMovementForExisting(
            AccountId.generate(), new BigDecimal("100.00"), new BigDecimal("100.00"));
  }

  @Test
  public void shouldCreateMovementWithFileImport() {
    final var movementBalance = new BigDecimal("100.00");
    final var currentBalance = new BigDecimal("200.00");

    final AccountDomain accountDomain =
        AccountDomain.withBasicMovementForExisting(
            AccountId.generate(), movementBalance, currentBalance);

    final var totalAmount = new BigDecimal("100.00");
    final var balanceSnapshot = new BigDecimal("210.00");
    final var newMovement =
        AccountMovementDomain.withFileImport(
            accountDomain.getId(), today, totalAmount, balanceSnapshot, importedAt);

    assertTrue(newMovement.hasMetadata(AccountMovementDomain.FILE_IMPORT_TAG));
    assertTrue(newMovement.hasMetadata(AccountMovementDomain.FILE_IMPORTED_AT_TAG));
    assertEquals(
        importedAt, newMovement.getMetadataField(AccountMovementDomain.FILE_IMPORTED_AT_TAG));
    assertEquals(MovementType.DEPOSIT, newMovement.getMovementType());
  }

  @Test
  public void shouldCreateWithdrawalMovement() {
    final var totalAmount = new BigDecimal("-100.00");
    final var balanceSnapshot = new BigDecimal("0.00");
    final var newMovement =
        AccountMovementDomain.withBalances(
            accountCeroBalance.getId(), today, totalAmount, balanceSnapshot);

    assertEquals(MovementType.WITHDRAWAL, newMovement.getMovementType());
    assertEquals(totalAmount, newMovement.getMovementAmount());
    assertEquals(balanceSnapshot, newMovement.getBalanceSnapshot());
    assertEquals(today, newMovement.getMovementDate());
  }

  @Test
  public void shouldCreateBalanceSnapshotMovement() {
    final BigDecimal totalAmount = null;
    final var balanceSnapshot = new BigDecimal("200.00");
    final var newMovement =
        AccountMovementDomain.withBalances(
            account100Balance.getId(), today, totalAmount, balanceSnapshot);

    assertEquals(MovementType.BALANCE_SNAPSHOT, newMovement.getMovementType());

    assertEquals(balanceSnapshot, newMovement.getBalanceSnapshot());
    assertEquals(today, newMovement.getMovementDate());

    assertNull(newMovement.getMovementAmount());
  }

  @Test
  public void throwExceptionWhenRequiredFieldsAreNull() {

    assertThrows(
        GenericSpecificationException.class,
        () -> AccountMovementDomain.withBalances(null, null, null, null));

    assertThrows(
        GenericSpecificationException.class,
        () -> AccountMovementDomain.withBalances(AccountId.generate(), null, null, null));

    assertThrows(
        GenericSpecificationException.class,
        () -> AccountMovementDomain.withBalances(accountCeroBalance.getId(), today, null, null));

    assertDoesNotThrow(
        () ->
            AccountMovementDomain.withBalances(accountCeroBalance.getId(), today, JBH_ZERO, null));
    assertDoesNotThrow(
        () ->
            AccountMovementDomain.withBalances(accountCeroBalance.getId(), today, null, JBH_ZERO));
  }
}
