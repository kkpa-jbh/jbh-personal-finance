package com.jbh.finance.application.services;

import static com.jbh.finance.testfixtures.CategoryFixtures.OTHER_INCOME;
import static com.jbh.finance.testfixtures.CategoryFixtures.PERSONAL_EXPENSE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.finance.application.builders.ProductEntityBuilder;
import com.jbh.finance.application.builders.UseCaseBuilder;
import com.jbh.finance.application.feature.product.services.ProductLifecycleService;
import com.jbh.finance.domain.movement.CategoryDomain;
import com.jbh.finance.domain.movement.MovementDomain;
import com.jbh.finance.domain.movement.vo.MovementMetadata;
import com.jbh.finance.domain.movement.vo.MovementType;
import com.jbh.finance.domain.product.ProductDomain;
import com.jbh.finance.domain.product.vo.ProductId;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ProductServiceTest {

  static UUID userId = UUID.randomUUID();
  private static ProductLifecycleService productLifecycleService;
  LocalDate today = LocalDate.now();

  @BeforeEach
  void setup() {
    productLifecycleService = UseCaseBuilder.buildAccountService();
  }

  @Test
  public void shouldSyncMultiBalances() throws BusinessException {
    int totalMovements = 2;
    final var accountMovementBalance = new BigDecimal("100.00");
    final var accountCurrentBalance = new BigDecimal("100.00");
    final ProductDomain accountDomain =
        ProductEntityBuilder.withBasicMovementForExisting(
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
            OTHER_INCOME);

    final var amount2 = new BigDecimal("-50.00");
    final var balance2 = new BigDecimal("155.00");

    final var newMovement2 =
        with(
            accountDomain.getId(),
            today.plusDays(-1 * --totalMovements),
            amount2,
            balance2,
            MovementType.WITHDRAWAL,
            PERSONAL_EXPENSE);

    productLifecycleService.syncByUploadedMovements(
        accountDomain, List.of(newMovement1, newMovement2));

    assertEquals(new BigDecimal("150.00"), accountDomain.getMovementBalance());
    assertEquals(new BigDecimal("155.00"), accountDomain.getCurrentBalance());
    assertEquals(new BigDecimal("5.00"), accountDomain.getNetProfitBalance());
  }

  public static MovementDomain with(
      final ProductId accountId,
      final LocalDate movementDate,
      final BigDecimal totalAmount,
      final BigDecimal balanceSnapshot,
      final MovementType movementType,
      final CategoryDomain category) {

    final MovementDomain movDomain =
        new MovementDomain(
            accountId,
            movementType,
            movementDate,
            totalAmount,
            balanceSnapshot,
            MovementMetadata.createEmpty(),
            category,
            null);

    try {
      movDomain.validate();
    } catch (final BusinessException e) {
      throw new GenericSpecificationException(e.getMessage());
    }

    return movDomain;
  }

  @Test
  public void shouldSyncSingleBalance() throws BusinessException {
    final var movementBalance = new BigDecimal("100.00");
    final var currentBalance = new BigDecimal("200.00");
    final ProductDomain accountDomain =
        ProductEntityBuilder.withBasicMovementForExisting(
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
            OTHER_INCOME);
    productLifecycleService.syncByUploadedMovements(
        accountDomain, Collections.singletonList(newMovement));

    assertEquals(movementBalance.add(totalAmount), accountDomain.getMovementBalance());
    assertEquals(balanceSnapshot, accountDomain.getCurrentBalance());
    assertEquals(new BigDecimal("10.00"), accountDomain.getNetProfitBalance());
  }
}
