package com.jbh.account.domain.entity;

import static com.jbh.account.domain.entity.MovementCategoryDomain.OTHER_INCOME_CATEGORY;
import static com.jbh.account.domain.entity.MovementCategoryDomain.PERSONAL_EXPENSE_CATEGORY;
import static com.jbh.account.domain.entity.ProductDomainTest.userId;
import static com.jbh.account.domain.vo.MovementType.BALANCE_SNAPSHOT;
import static com.jbh.account.domain.vo.MovementType.DEPOSIT;
import static com.jbh.account.domain.vo.MovementType.WITHDRAWAL;
import static com.jbh.commons.util.JbhMoneyUtils.JBH_ZERO;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.account.domain.vo.AccountMovementMetadataKey;
import com.jbh.account.domain.vo.MovementType;
import com.jbh.account.domain.vo.ProductId;
import com.jbh.commons.exception.BusinessException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AccountMovementTest {
  ProductDomain accountCeroBalance;
  ProductDomain account100Balance;

  LocalDate today = LocalDate.now();
  LocalDateTime importedAt = LocalDateTime.now();

  @BeforeEach
  public void setUp() {
    accountCeroBalance =
        AccountDomainTestBuilder.createSavingProductWithBalance(
            ProductId.generate(), userId, JBH_ZERO, JBH_ZERO);

    account100Balance =
        AccountDomainTestBuilder.createSavingProductWithBalance(
            ProductId.generate(), userId, new BigDecimal("100.00"), new BigDecimal("100.00"));
  }

  @Test
  public void shouldCreateMovementWithFileImport() throws BusinessException {
    final var movementBalance = new BigDecimal("100.00");
    final var currentBalance = new BigDecimal("200.00");

    final ProductDomain accountDomain =
        AccountDomainTestBuilder.createSavingProductWithBalance(
            ProductId.generate(), userId, movementBalance, currentBalance);

    final var totalAmount = new BigDecimal("100.00");
    final var balanceSnapshot = new BigDecimal("210.00");
    final var newMovement =
        ProductMovementDomain.withFileImport(
            accountDomain.getId(),
            today,
            totalAmount,
            balanceSnapshot,
            BALANCE_SNAPSHOT,
            importedAt);

    assertTrue(newMovement.hasMetadata(AccountMovementMetadataKey.FILE_IMPORTED_AT_TAG));
    assertTrue(newMovement.hasMetadata(AccountMovementMetadataKey.FILE_IMPORTED_AT_TAG));
    assertEquals(
        importedAt, newMovement.getMetadataField(AccountMovementMetadataKey.FILE_IMPORTED_AT_TAG));
    assertEquals(BALANCE_SNAPSHOT, newMovement.getMovementType());
  }

  @Test
  public void shouldCreateWithdrawalMovement() {
    final var totalAmount = new BigDecimal("-100.00");
    final var balanceSnapshot = new BigDecimal("0.00");
    final var newMovement =
        EntityBuilder.with(
            accountCeroBalance.getId(),
            today,
            totalAmount,
            balanceSnapshot,
            WITHDRAWAL,
            PERSONAL_EXPENSE_CATEGORY);

    assertEquals(WITHDRAWAL, newMovement.getMovementType());
    assertEquals(totalAmount, newMovement.getMovementAmount());
    assertEquals(balanceSnapshot, newMovement.getBalanceSnapshot());
    assertEquals(today, newMovement.getMovementDate());
  }

  @Test
  public void shouldCreateBalanceSnapshotMovement() {
    final BigDecimal totalAmount = null;
    final var balanceSnapshot = new BigDecimal("200.00");
    final var newMovement =
        EntityBuilder.with(
            account100Balance.getId(), today, totalAmount, balanceSnapshot, BALANCE_SNAPSHOT, null);

    assertEquals(BALANCE_SNAPSHOT, newMovement.getMovementType());

    assertEquals(balanceSnapshot, newMovement.getBalanceSnapshot());
    assertEquals(today, newMovement.getMovementDate());

    assertNull(newMovement.getMovementAmount());
  }

  @Test
  public void throwExceptionWhenRequiredFieldsAreNull() {

    assertThrows(
        GenericSpecificationException.class,
        () -> EntityBuilder.with(null, null, null, null, null, null));

    assertThrows(
        GenericSpecificationException.class,
        () -> EntityBuilder.with(ProductId.generate(), null, null, null, null, null));

    assertThrows(
        GenericSpecificationException.class,
        () -> EntityBuilder.with(accountCeroBalance.getId(), today, null, null, null, null));

    assertThrows(
        GenericSpecificationException.class,
        () ->
            EntityBuilder.with(
                accountCeroBalance.getId(), today, new BigDecimal("100.00"), null, null, null));

    assertThrows(
        GenericSpecificationException.class,
        () ->
            EntityBuilder.with(
                accountCeroBalance.getId(),
                today,
                new BigDecimal("100.00"),
                null,
                MovementType.DEPOSIT,
                null));

    assertDoesNotThrow(
        () ->
            EntityBuilder.with(
                accountCeroBalance.getId(), today, JBH_ZERO, null, BALANCE_SNAPSHOT, null));
    assertDoesNotThrow(
        () ->
            EntityBuilder.with(
                accountCeroBalance.getId(),
                today,
                new BigDecimal("100"),
                JBH_ZERO,
                DEPOSIT,
                OTHER_INCOME_CATEGORY));

    assertDoesNotThrow(
        () ->
            EntityBuilder.with(
                accountCeroBalance.getId(),
                today,
                new BigDecimal("-23.00"),
                JBH_ZERO,
                WITHDRAWAL,
                PERSONAL_EXPENSE_CATEGORY));
  }
}
