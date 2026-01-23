package com.jbh.account.application.services;

import static com.jbh.account.domain.entity.MovementCategoryDomain.OTHER_INCOME_CATEGORY;
import static com.jbh.account.domain.entity.MovementCategoryDomain.PERSONAL_EXPENSE_CATEGORY;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jbh.account.application.builders.AccountEntityBuilder;
import com.jbh.account.application.builders.UseCaseBuilder;
import com.jbh.account.application.core.services.account.AccountService;
import com.jbh.account.domain.entity.MovementCategoryDomain;
import com.jbh.account.domain.entity.ProductDomain;
import com.jbh.account.domain.entity.ProductMovementDomain;
import com.jbh.account.domain.exceptions.GenericSpecificationException;
import com.jbh.account.domain.exceptions.ProductBusinessException;
import com.jbh.account.domain.vo.AccountMovementMetadata;
import com.jbh.account.domain.vo.MovementType;
import com.jbh.account.domain.vo.ProductId;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AccountServiceTest {

  static UUID userId = UUID.randomUUID();
  private static AccountService accountService;
  ProductDomain accountDomain;
  LocalDate today = LocalDate.now();

  @BeforeEach
  void setup() {
    accountService = UseCaseBuilder.buildAccountService();
  }

  @Test
  public void shouldSyncMultiBalances() throws ProductBusinessException {
    int totalMovements = 2;
    final var accountMovementBalance = new BigDecimal("100.00");
    final var accountCurrentBalance = new BigDecimal("100.00");
    final ProductDomain accountDomain =
        AccountEntityBuilder.withBasicMovementForExisting(
            ProductId.generate(), userId, accountMovementBalance, accountCurrentBalance);

    final var amount1 = new BigDecimal("100.00");
    final var balance1 = new BigDecimal("205.00");

    final var newMovement1 =
        with(
            accountDomain.getId(),
            today.plusDays(-1 * --totalMovements),
            amount1,
            balance1,
            MovementType.DEPOSIT,
            OTHER_INCOME_CATEGORY);

    final var amount2 = new BigDecimal("-50.00");
    final var balance2 = new BigDecimal("155.00");

    final var newMovement2 =
        with(
            accountDomain.getId(),
            today.plusDays(-1 * --totalMovements),
            amount2,
            balance2,
            MovementType.WITHDRAWAL,
            PERSONAL_EXPENSE_CATEGORY);

    accountService.syncByUploadedMovements(accountDomain, List.of(newMovement1, newMovement2));

    assertEquals(new BigDecimal("150.00"), accountDomain.getMovementBalance());
    assertEquals(new BigDecimal("155.00"), accountDomain.getCurrentBalance());
    assertEquals(new BigDecimal("5.00"), accountDomain.getNetProfitBalance());
  }

  public static ProductMovementDomain with(
      final ProductId accountId,
      final LocalDate movementDate,
      final BigDecimal totalAmount,
      final BigDecimal balanceSnapshot,
      final MovementType movementType,
      final MovementCategoryDomain category) {

    final ProductMovementDomain movDomain =
        new ProductMovementDomain(
            accountId,
            movementType,
            movementDate,
            totalAmount,
            balanceSnapshot,
            AccountMovementMetadata.createEmpty(),
            category);

    try {
      movDomain.validate();
    } catch (final ProductBusinessException e) {
      throw new GenericSpecificationException(e.getMessage());
    }

    return movDomain;
  }

  @Test
  public void shouldSyncSingleBalance() throws ProductBusinessException {
    final var movementBalance = new BigDecimal("100.00");
    final var currentBalance = new BigDecimal("200.00");
    final ProductDomain accountDomain =
        AccountEntityBuilder.withBasicMovementForExisting(
            ProductId.generate(), userId, movementBalance, currentBalance);

    final var totalAmount = new BigDecimal("100.00");
    final var balanceSnapshot = new BigDecimal("210.00");
    final var newMovement =
        with(
            accountDomain.getId(),
            today,
            totalAmount,
            balanceSnapshot,
            MovementType.DEPOSIT,
            OTHER_INCOME_CATEGORY);
    accountService.syncByUploadedMovements(accountDomain, Collections.singletonList(newMovement));

    assertEquals(movementBalance.add(totalAmount), accountDomain.getMovementBalance());
    assertEquals(balanceSnapshot, accountDomain.getCurrentBalance());
    assertEquals(new BigDecimal("10.00"), accountDomain.getNetProfitBalance());
  }
}
