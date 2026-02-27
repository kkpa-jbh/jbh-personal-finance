package com.jbh.finance.domain.entity;

import static com.jbh.commons.util.JbhMoneyUtils.JBH_ZERO;
import static com.jbh.commons.util.JbhMoneyUtils.withJBHDecimals;
import static com.jbh.finance.domain.movement.vo.MovementType.DEPOSIT;
import static com.jbh.finance.domain.movement.vo.MovementType.WITHDRAWAL;
import static com.jbh.finance.testfixtures.CategoryFixturesDomain.OTHER_INCOME;
import static com.jbh.finance.testfixtures.CategoryFixturesDomain.UNKNOWN_EXPENSE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.domain.monthlybalance.MonthlyBalanceDomain;
import com.jbh.finance.domain.movement.vo.MovementType;
import com.jbh.finance.domain.product.vo.ProductId;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class MonthlyBalanceDomainTest {

  private static final MovementType BALANCE_SNAPSHOT_TESTSCOPE = MovementType.BALANCE_SNAPSHOT;
  MonthlyBalanceDomain ceroMonthlyBalance;
  MonthlyBalanceDomain oneHundredMonthlyBalance;
  LocalDate today = LocalDate.now();
  YearMonth todayYM = YearMonth.now();
  ProductId productId = ProductId.generate();

  @BeforeEach
  public void setUp() throws BusinessException {
    ceroMonthlyBalance = MonthlyBalanceDomain.withPeriod(productId, todayYM);
    ceroMonthlyBalance =
        new MonthlyBalanceDomain(
            null,
            productId,
            todayYM.getYear(),
            todayYM.getMonthValue(),
            todayYM,
            JBH_ZERO,
            JBH_ZERO,
            JBH_ZERO,
            JBH_ZERO,
            JBH_ZERO,
            JBH_ZERO,
            0,
            false,
            false,
            JBH_ZERO,
            null);

    oneHundredMonthlyBalance = MonthlyBalanceDomain.withPeriod(productId, todayYM);
    oneHundredMonthlyBalance.assignMovement(
        EntityBuilder.with(
            productId,
            today,
            new BigDecimal("100.00"),
            new BigDecimal("100.00"),
            DEPOSIT,
            OTHER_INCOME));
  }

  @Test
  public void shouldCreateWithConstructor() {
    final MonthlyBalanceDomain expected =
        new MonthlyBalanceDomain(
            null,
            productId,
            todayYM.getYear(),
            todayYM.getMonthValue(),
            todayYM,
            JBH_ZERO,
            JBH_ZERO,
            JBH_ZERO,
            JBH_ZERO,
            JBH_ZERO,
            JBH_ZERO,
            0,
            false,
            false,
            JBH_ZERO,
            null);

    assertEquals(expected, ceroMonthlyBalance);
    assertTrue(expected.equals(ceroMonthlyBalance));

    assertNotNull(ceroMonthlyBalance.hashCode());
  }

  @Test
  public void shouldCreateWithPeriod() throws BusinessException {
    assertNull(ceroMonthlyBalance.getId());
    assertNotNull(ceroMonthlyBalance.getProductId());
    assertEquals(productId, ceroMonthlyBalance.getProductId());
    assertEquals(today.getYear(), ceroMonthlyBalance.getYear());
    assertEquals(today.getMonthValue(), ceroMonthlyBalance.getMonth());
    assertEquals(todayYM, ceroMonthlyBalance.getPeriod());
    assertEquals(JBH_ZERO, ceroMonthlyBalance.getTotalDebits());
    assertEquals(JBH_ZERO, ceroMonthlyBalance.getTotalCredits());
    assertEquals(0, ceroMonthlyBalance.getTotalMovements());
    assertEquals(JBH_ZERO, ceroMonthlyBalance.getOpeningBalance());
    assertEquals(JBH_ZERO, ceroMonthlyBalance.getClosingBalance());
    assertEquals(JBH_ZERO, ceroMonthlyBalance.getMonthlyNetProfit());

    ceroMonthlyBalance.assignMovement(
        EntityBuilder.with(productId, today, JBH_ZERO, JBH_ZERO, DEPOSIT, OTHER_INCOME));
    assertEquals(JBH_ZERO, ceroMonthlyBalance.getMonthlyNetProfit());
  }

  @Test
  public void shouldCreateWithOpeningBalance() {
    MonthlyBalanceDomain previousMonthlyBalance =
        EntityBuilder.withInitialDataForNextMonth(
            productId, todayYM, new BigDecimal("350.00"), false);
    ceroMonthlyBalance.assignOpeningBalance(previousMonthlyBalance);
    assertEquals(new BigDecimal("350.00"), ceroMonthlyBalance.getOpeningBalance());

    previousMonthlyBalance =
        EntityBuilder.withInitialDataForNextMonth(productId, todayYM, BigDecimal.ZERO, false);
    ceroMonthlyBalance.assignOpeningBalance(previousMonthlyBalance);
    assertEquals(JBH_ZERO, ceroMonthlyBalance.getOpeningBalance());
  }

  @Test
  public void shouldSetOfficialReportAndAdjustClosingBalanceForMonth() throws BusinessException {

    final var closingBalance = (new BigDecimal("150.00"));
    final var monthlyProfitReported = (new BigDecimal("20.00"));
    oneHundredMonthlyBalance.assignOfficialMonthlyReport(
        closingBalance, monthlyProfitReported, null);

    assertEquals(closingBalance, oneHundredMonthlyBalance.getClosingBalance());
    assertEquals(monthlyProfitReported, oneHundredMonthlyBalance.getMonthlyProfitReported());
    assertTrue(oneHundredMonthlyBalance.isOfficialMonthlyReport());
    assertEquals(monthlyProfitReported, oneHundredMonthlyBalance.getMonthlyNetProfit());
  }

  @Test
  public void shouldSetOfficialReportWithZeroProfitAndRegularNetProfitFormula()
      throws BusinessException {

    final var closingBalance = (new BigDecimal("150.00"));
    final var monthlyProfitReported = BigDecimal.ZERO;
    oneHundredMonthlyBalance.assignOfficialMonthlyReport(
        closingBalance, monthlyProfitReported, null);

    assertEquals(closingBalance, oneHundredMonthlyBalance.getClosingBalance());
    assertEquals(
        withJBHDecimals(monthlyProfitReported),
        oneHundredMonthlyBalance.getMonthlyProfitReported());
    assertTrue(oneHundredMonthlyBalance.isOfficialMonthlyReport());
    assertEquals(new BigDecimal("50.00"), oneHundredMonthlyBalance.getMonthlyNetProfit());
  }

  @Test
  public void shouldCreateWithInitialDataForNextMonth() {
    final var closingBalance = new BigDecimal("200.00");
    final var newBalance =
        EntityBuilder.withInitialDataForNextMonth(productId, todayYM, closingBalance, false);

    assertEquals(closingBalance, newBalance.getClosingBalance());
    assertEquals(JBH_ZERO, newBalance.getTotalDebits());
    assertEquals(JBH_ZERO, newBalance.getTotalCredits());
    assertEquals(0, newBalance.getTotalMovements());
    assertEquals(JBH_ZERO, newBalance.getOpeningBalance());
    assertEquals(closingBalance, newBalance.getClosingBalance());
    assertEquals(JBH_ZERO, newBalance.getMonthlyNetProfit());
    assertFalse(newBalance.isGapPeriod());
  }

  @Test
  public void shouldSyncDepositAndSnapshotMovement() throws BusinessException {
    final var totalAmount = new BigDecimal("100.00");
    final var balanceSnapshot = new BigDecimal("120.00");
    final var newMovement =
        EntityBuilder.with(productId, today, totalAmount, balanceSnapshot, DEPOSIT, OTHER_INCOME);

    ceroMonthlyBalance.assignMovement(newMovement);

    assertEquals(totalAmount, ceroMonthlyBalance.getTotalDebits());
    assertEquals(JBH_ZERO, ceroMonthlyBalance.getTotalCredits());
    assertEquals(totalAmount, ceroMonthlyBalance.getMovementBalance());
    assertEquals(JBH_ZERO, ceroMonthlyBalance.getOpeningBalance());
    assertEquals(balanceSnapshot, ceroMonthlyBalance.getClosingBalance());
    assertEquals(1, ceroMonthlyBalance.getTotalMovements());
    assertEquals(new BigDecimal("20.00"), ceroMonthlyBalance.getMonthlyNetProfit());
    assertFalse(ceroMonthlyBalance.isGapPeriod());
  }

  @Test
  @DisplayName("First deposit to monthly balance should not have profit")
  public void shouldSyncDepositWithoutSnapshotMovement() throws BusinessException {
    final var totalAmount = new BigDecimal("100.00");
    final var newMovement =
        EntityBuilder.with(productId, today, totalAmount, null, DEPOSIT, OTHER_INCOME);

    ceroMonthlyBalance.assignMovement(newMovement);

    assertEquals(totalAmount, ceroMonthlyBalance.getTotalDebits());
    assertEquals(JBH_ZERO, ceroMonthlyBalance.getTotalCredits());
    assertEquals(totalAmount, ceroMonthlyBalance.getMovementBalance());
    assertEquals(JBH_ZERO, ceroMonthlyBalance.getOpeningBalance());
    assertEquals(totalAmount, ceroMonthlyBalance.getClosingBalance());
    assertEquals(1, ceroMonthlyBalance.getTotalMovements());
    assertEquals(JBH_ZERO, ceroMonthlyBalance.getMonthlyNetProfit());
    assertFalse(ceroMonthlyBalance.isGapPeriod());
  }

  @Test
  public void shouldSyncDepositAndSnapshotMovementOneHundredBalance() throws BusinessException {
    final var totalAmount = new BigDecimal("115.00");
    final var balanceSnapshot = new BigDecimal("230.00");
    final var newMovement =
        EntityBuilder.with(productId, today, totalAmount, balanceSnapshot, DEPOSIT, OTHER_INCOME);

    final BigDecimal existingTotalDebits = oneHundredMonthlyBalance.getTotalDebits();

    oneHundredMonthlyBalance.assignMovement(newMovement);

    assertEquals(totalAmount.add(existingTotalDebits), oneHundredMonthlyBalance.getTotalDebits());
    assertEquals(JBH_ZERO, oneHundredMonthlyBalance.getTotalCredits());
    assertEquals(
        totalAmount.add(existingTotalDebits), oneHundredMonthlyBalance.getMovementBalance());
    assertEquals(JBH_ZERO, oneHundredMonthlyBalance.getOpeningBalance());
    assertEquals(balanceSnapshot, oneHundredMonthlyBalance.getClosingBalance());
    assertEquals(2, oneHundredMonthlyBalance.getTotalMovements());
    assertEquals(new BigDecimal("15.00"), oneHundredMonthlyBalance.getMonthlyNetProfit());
    assertFalse(oneHundredMonthlyBalance.isGapPeriod());
  }

  @Test
  public void shouldSyncDepositWithoutSnapshotMovementOneHundred() throws BusinessException {
    final var totalAmount = new BigDecimal("50.00");
    final var newMovement =
        EntityBuilder.with(productId, today, totalAmount, null, DEPOSIT, OTHER_INCOME);
    final BigDecimal existingTotalDebits = oneHundredMonthlyBalance.getTotalDebits();
    oneHundredMonthlyBalance.assignMovement(newMovement);

    final MonthlyBalanceDomain newBalance = oneHundredMonthlyBalance;

    assertEquals(totalAmount.add(existingTotalDebits), newBalance.getTotalDebits());
    assertEquals(JBH_ZERO, newBalance.getTotalCredits());
    assertEquals(totalAmount.add(existingTotalDebits), newBalance.getMovementBalance());
    assertEquals(JBH_ZERO, newBalance.getOpeningBalance());
    assertEquals(totalAmount.add(existingTotalDebits), newBalance.getClosingBalance());
    assertEquals(2, newBalance.getTotalMovements());
    assertEquals(JBH_ZERO, newBalance.getMonthlyNetProfit());
    assertFalse(newBalance.isGapPeriod());
  }

  @Test
  public void shouldSyncWithdrawalAndSnapshotMovement() throws BusinessException {
    final var totalAmount = new BigDecimal("-100.00");
    final var balanceSnapshot = new BigDecimal("120.00");
    final var newMovement =
        EntityBuilder.with(
            productId, today, totalAmount, balanceSnapshot, WITHDRAWAL, UNKNOWN_EXPENSE);

    ceroMonthlyBalance.assignMovement(newMovement);

    assertEquals(JBH_ZERO, ceroMonthlyBalance.getTotalDebits());
    assertEquals(totalAmount.abs(), ceroMonthlyBalance.getTotalCredits());
    assertEquals(totalAmount, ceroMonthlyBalance.getMovementBalance());
    assertEquals(JBH_ZERO, ceroMonthlyBalance.getOpeningBalance());
    assertEquals(balanceSnapshot, ceroMonthlyBalance.getClosingBalance());
    assertEquals(1, ceroMonthlyBalance.getTotalMovements());
    assertEquals(new BigDecimal("220.00"), ceroMonthlyBalance.getMonthlyNetProfit());
    assertFalse(ceroMonthlyBalance.isGapPeriod());
  }

  @Test
  public void shouldSyncMultiMovementsOneHundredBalance() {
    // total Earn 10.00 on 1st deposit
    final var totalAmount = new BigDecimal("10.00");
    final var balanceSnapshot = new BigDecimal("120.00");
    final var newMovement =
        EntityBuilder.with(productId, today, totalAmount, balanceSnapshot, DEPOSIT, OTHER_INCOME);

    // TOTAL Earn 20.00 on 2nd deposit
    final var totalAmount2 = new BigDecimal("30.00");
    final var balanceSnapshot2 = new BigDecimal("170.00");
    final var newMovement2 =
        EntityBuilder.with(productId, today, totalAmount2, balanceSnapshot2, DEPOSIT, OTHER_INCOME);

    // Earn 0 on the withdrawal
    final var totalAmount3 = new BigDecimal("-20.00");
    final var balanceSnapshot3 = new BigDecimal("170.00");
    final var newMovement3 =
        EntityBuilder.with(
            productId, today, totalAmount3, balanceSnapshot3, WITHDRAWAL, UNKNOWN_EXPENSE);

    // oneHundredMonthlyBalance.syncMovements(List.of(newMovement, newMovement2, newMovement3));
    List.of(newMovement, newMovement2, newMovement3)
        .forEach(
            mov -> {
              try {
                oneHundredMonthlyBalance.assignMovement(mov);
              } catch (final BusinessException e) {
                throw new RuntimeException(e);
              }
            });

    final MonthlyBalanceDomain newBalance = oneHundredMonthlyBalance;
    assertEquals(4, newBalance.getTotalMovements());
    assertEquals(new BigDecimal("140.00"), newBalance.getTotalDebits());
    assertEquals(new BigDecimal("20.00"), newBalance.getTotalCredits());
    assertEquals(new BigDecimal("120.00"), newBalance.getMovementBalance());
    assertEquals(new BigDecimal("170.00"), newBalance.getClosingBalance());
    assertEquals(new BigDecimal("0.00"), newBalance.getOpeningBalance());
    assertEquals(new BigDecimal("50.00"), newBalance.getMonthlyNetProfit());
    assertFalse(newBalance.isGapPeriod());
  }

  @Test
  public void shouldSortByPeriodAscending() throws BusinessException {
    final var period1 = YearMonth.of(2023, 10);
    final var period2 = YearMonth.of(2023, 11);
    final var period3 = YearMonth.of(2023, 12);
    final var period4 = YearMonth.of(2024, 1);
    final var period5 = YearMonth.of(2024, 2);

    final var balance1 = MonthlyBalanceDomain.withPeriod(productId, period1);
    final var balance2 = MonthlyBalanceDomain.withPeriod(productId, period2);
    final var balance3 = MonthlyBalanceDomain.withPeriod(productId, period3);
    final var balance4 = MonthlyBalanceDomain.withPeriod(productId, period4);
    final var balance5 = MonthlyBalanceDomain.withPeriod(productId, period5);

    balance1.assignMovement(
        EntityBuilder.with(
            productId,
            period1.atDay(1),
            new BigDecimal("10"),
            new BigDecimal("10"),
            DEPOSIT,
            OTHER_INCOME));
    balance2.assignMovement(
        EntityBuilder.with(
            productId,
            period2.atDay(1),
            new BigDecimal("20"),
            new BigDecimal("20"),
            DEPOSIT,
            OTHER_INCOME));
    balance3.assignMovement(
        EntityBuilder.with(
            productId,
            period3.atDay(1),
            new BigDecimal("30"),
            new BigDecimal("30"),
            DEPOSIT,
            OTHER_INCOME));
    balance4.assignMovement(
        EntityBuilder.with(
            productId,
            period4.atDay(1),
            new BigDecimal("40"),
            new BigDecimal("40"),
            DEPOSIT,
            OTHER_INCOME));
    balance5.assignMovement(
        EntityBuilder.with(
            productId,
            period5.atDay(1),
            new BigDecimal("50"),
            new BigDecimal("50"),
            DEPOSIT,
            OTHER_INCOME));

    assertEquals(period1, balance1.getPeriod());
    assertEquals(period2, balance2.getPeriod());
    assertEquals(period3, balance3.getPeriod());
    assertEquals(period4, balance4.getPeriod());
    assertEquals(period5, balance5.getPeriod());
  }

  @Test
  public void shouldThrowExceptionWhenDifferentPeriod() throws BusinessException {
    final LocalDate initBalanceDate = LocalDate.of(2024, 8, 1);
    final YearMonth initBalancePeriod = YearMonth.of(2024, 8);
    final MonthlyBalanceDomain accountMonthlyBalance =
        MonthlyBalanceDomain.withPeriod(productId, initBalancePeriod);

    // Add movement with balance snapshot of 2105192.00
    final var initBalanceSnapshot = new BigDecimal("2105192.00");
    accountMonthlyBalance.assignMovement(
        EntityBuilder.with(
            productId,
            initBalanceDate,
            null,
            initBalanceSnapshot,
            BALANCE_SNAPSHOT_TESTSCOPE,
            null));

    assertEquals(initBalanceSnapshot, accountMonthlyBalance.getClosingBalance());
    assertEquals(initBalancePeriod, accountMonthlyBalance.getPeriod());
    assertEquals(JBH_ZERO, accountMonthlyBalance.getTotalDebits());
    assertEquals(JBH_ZERO, accountMonthlyBalance.getTotalCredits());
    assertEquals(JBH_ZERO, accountMonthlyBalance.getMovementBalance());
    assertEquals(JBH_ZERO, accountMonthlyBalance.getOpeningBalance());
    assertEquals(JBH_ZERO, accountMonthlyBalance.getMonthlyNetProfit());
    assertEquals(0, accountMonthlyBalance.getTotalMovements());

    // Add balance snapshot of 223.00 on 2024-09-01
    final var septBalanceSnapshot = new BigDecimal("223.00");
    final var septBalancePeriod = YearMonth.of(2024, 9);
    final var septBalanceDate = septBalancePeriod.atDay(1);

    assertThrows(
        BusinessException.class,
        () -> {
          accountMonthlyBalance.assignMovement(
              EntityBuilder.with(
                  productId,
                  septBalanceDate,
                  null,
                  septBalanceSnapshot,
                  BALANCE_SNAPSHOT_TESTSCOPE,
                  null));
        });
  }
}
