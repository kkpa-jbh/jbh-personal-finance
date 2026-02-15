package com.jbh.finance.domain.entity;

import static com.jbh.commons.util.JbhMoneyUtils.JBH_ZERO;
import static com.jbh.finance.domain.entity.ProductDomainTest.userId;
import static com.jbh.finance.domain.movement.CategoryDomain.OTHER_INCOME_CATEGORY;
import static com.jbh.finance.domain.movement.CategoryDomain.PERSONAL_EXPENSE_CATEGORY;
import static com.jbh.finance.domain.movement.vo.MovementType.DEPOSIT;
import static com.jbh.finance.domain.movement.vo.MovementType.WITHDRAWAL;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.finance.domain.movement.MovementDomain;
import com.jbh.finance.domain.movement.vo.MovementMetadataKey;
import com.jbh.finance.domain.movement.vo.MovementType;
import com.jbh.finance.domain.product.ProductDomain;
import com.jbh.finance.domain.product.vo.ProductId;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MovementTest {
  ProductDomain productCeroBalance;
  ProductDomain product100Balance;
  MovementType BALANCE_SNAPSHOT_TESTSCOPE = MovementType.BALANCE_SNAPSHOT;
  LocalDate today = LocalDate.now();
  LocalDateTime importedAt = LocalDateTime.now();

  @BeforeEach
  public void setUp() {
    productCeroBalance =
        ProductDomainTestBuilder.createSavingProductWithBalance(
            ProductId.generate(), userId, JBH_ZERO, JBH_ZERO);

    product100Balance =
        ProductDomainTestBuilder.createSavingProductWithBalance(
            ProductId.generate(), userId, new BigDecimal("100.00"), new BigDecimal("100.00"));
  }

  @Test
  public void shouldCreateMovementWithFileImport() throws BusinessException {
    final var movementBalance = new BigDecimal("100.00");
    final var currentBalance = new BigDecimal("200.00");

    final ProductDomain accountDomain =
        ProductDomainTestBuilder.createSavingProductWithBalance(
            ProductId.generate(), userId, movementBalance, currentBalance);

    final var totalAmount = new BigDecimal("100.00");
    final var balanceSnapshot = new BigDecimal("210.00");
    final var newMovement =
        MovementDomain.withFileImport(
            accountDomain.getId(),
            today,
            totalAmount,
            balanceSnapshot,
            BALANCE_SNAPSHOT_TESTSCOPE,
            importedAt);

    assertTrue(newMovement.hasMetadata(MovementMetadataKey.FILE_IMPORTED_AT_TAG));
    assertTrue(newMovement.hasMetadata(MovementMetadataKey.FILE_IMPORTED_AT_TAG));
    assertEquals(
        importedAt, newMovement.getMetadataField(MovementMetadataKey.FILE_IMPORTED_AT_TAG));
    assertEquals(BALANCE_SNAPSHOT_TESTSCOPE, newMovement.getMovementType());
  }

  @Test
  public void shouldCreateWithdrawalMovement() {
    final var totalAmount = new BigDecimal("-100.00");
    final var balanceSnapshot = new BigDecimal("0.00");
    final var newMovement =
        EntityBuilder.with(
            productCeroBalance.getId(),
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
            product100Balance.getId(),
            today,
            totalAmount,
            balanceSnapshot,
            BALANCE_SNAPSHOT_TESTSCOPE,
            null);

    assertEquals(BALANCE_SNAPSHOT_TESTSCOPE, newMovement.getMovementType());

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
        () -> EntityBuilder.with(productCeroBalance.getId(), today, null, null, null, null));

    assertThrows(
        GenericSpecificationException.class,
        () ->
            EntityBuilder.with(
                productCeroBalance.getId(), today, new BigDecimal("100.00"), null, null, null));

    assertThrows(
        GenericSpecificationException.class,
        () ->
            EntityBuilder.with(
                productCeroBalance.getId(), today, new BigDecimal("100.00"), null, DEPOSIT, null));

    assertDoesNotThrow(
        () ->
            EntityBuilder.with(
                productCeroBalance.getId(),
                today,
                JBH_ZERO,
                null,
                BALANCE_SNAPSHOT_TESTSCOPE,
                null));
    assertDoesNotThrow(
        () ->
            EntityBuilder.with(
                productCeroBalance.getId(),
                today,
                new BigDecimal("100"),
                JBH_ZERO,
                DEPOSIT,
                OTHER_INCOME_CATEGORY));

    assertDoesNotThrow(
        () ->
            EntityBuilder.with(
                productCeroBalance.getId(),
                today,
                new BigDecimal("-23.00"),
                JBH_ZERO,
                WITHDRAWAL,
                PERSONAL_EXPENSE_CATEGORY));
  }
}
