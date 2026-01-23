package com.jbh.account.domain.entity;

import static com.jbh.account.domain.entity.MovementCategoryDomain.OTHER_INCOME_CATEGORY;
import static com.jbh.account.domain.entity.MovementCategoryDomain.PERSONAL_EXPENSE_CATEGORY;
import static com.jbh.account.domain.utils.JbhMoneyUtils.JBH_ZERO;
import static com.jbh.account.domain.utils.JbhMoneyUtils.withJBHDecimals;
import static com.jbh.account.domain.vo.MovementType.BALANCE_SNAPSHOT;
import static com.jbh.account.domain.vo.MovementType.DEPOSIT;
import static com.jbh.account.domain.vo.MovementType.WITHDRAWAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jbh.account.domain.exceptions.ProductBusinessException;
import com.jbh.account.domain.vo.ProductId;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class AccountMonthlyBalanceDomainTest {
  MonthlyBalanceDomain ceroMonthlyBalance;
  MonthlyBalanceDomain oneHundredMonthlyBalance;
  LocalDate today = LocalDate.now();
  YearMonth todayYM = YearMonth.now();
  ProductId accountId = ProductId.generate();

  @BeforeEach
  public void setUp() throws ProductBusinessException {
    ceroMonthlyBalance = MonthlyBalanceDomain.withPeriod(accountId, todayYM);
    ceroMonthlyBalance =
        new MonthlyBalanceDomain(
            null,
            accountId,
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

    oneHundredMonthlyBalance = MonthlyBalanceDomain.withPeriod(accountId, todayYM);
    oneHundredMonthlyBalance.assignMovement(
        EntityBuilder.with(
            accountId,
            today,
            new BigDecimal("100.00"),
            new BigDecimal("100.00"),
            DEPOSIT,
            OTHER_INCOME_CATEGORY));
  }

  @Test
  public void shouldCreateWithConstructor() {
    final MonthlyBalanceDomain expected =
        new MonthlyBalanceDomain(
            null,
            accountId,
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
  public void shouldCreateWithPeriod() throws ProductBusinessException {
    assertNull(ceroMonthlyBalance.getId());
    assertNotNull(ceroMonthlyBalance.getAccountId());
    assertEquals(accountId, ceroMonthlyBalance.getAccountId());
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
        EntityBuilder.with(accountId, today, JBH_ZERO, JBH_ZERO, DEPOSIT, OTHER_INCOME_CATEGORY));
    assertEquals(JBH_ZERO, ceroMonthlyBalance.getMonthlyNetProfit());
  }

  @Test
  public void shouldCreateWithOpeningBalance() {
    MonthlyBalanceDomain previousMonthlyBalance =
        EntityBuilder.withInitialDataForNextMonth(
            accountId, todayYM, new BigDecimal("350.00"), false);
    ceroMonthlyBalance.assignOpeningBalance(previousMonthlyBalance);
    assertEquals(new BigDecimal("350.00"), ceroMonthlyBalance.getOpeningBalance());

    previousMonthlyBalance =
        EntityBuilder.withInitialDataForNextMonth(accountId, todayYM, BigDecimal.ZERO, false);
    ceroMonthlyBalance.assignOpeningBalance(previousMonthlyBalance);
    assertEquals(JBH_ZERO, ceroMonthlyBalance.getOpeningBalance());
  }

  @Test
  public void shouldSetOfficialReportAndAdjustClosingBalanceForMonth()
      throws ProductBusinessException {

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
      throws ProductBusinessException {

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
        EntityBuilder.withInitialDataForNextMonth(accountId, todayYM, closingBalance, false);

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
  public void shouldSyncDepositAndSnapshotMovement() throws ProductBusinessException {
    final var totalAmount = new BigDecimal("100.00");
    final var balanceSnapshot = new BigDecimal("120.00");
    final var newMovement =
        EntityBuilder.with(
            accountId, today, totalAmount, balanceSnapshot, DEPOSIT, OTHER_INCOME_CATEGORY);

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
  public void shouldSyncDepositWithoutSnapshotMovement() throws ProductBusinessException {
    final var totalAmount = new BigDecimal("100.00");
    final var newMovement =
        EntityBuilder.with(accountId, today, totalAmount, null, DEPOSIT, OTHER_INCOME_CATEGORY);

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
  public void shouldSyncDepositAndSnapshotMovementOneHundredBalance()
      throws ProductBusinessException {
    final var totalAmount = new BigDecimal("115.00");
    final var balanceSnapshot = new BigDecimal("230.00");
    final var newMovement =
        EntityBuilder.with(
            accountId, today, totalAmount, balanceSnapshot, DEPOSIT, OTHER_INCOME_CATEGORY);

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
  public void shouldSyncDepositWithoutSnapshotMovementOneHundred() throws ProductBusinessException {
    final var totalAmount = new BigDecimal("50.00");
    final var newMovement =
        EntityBuilder.with(accountId, today, totalAmount, null, DEPOSIT, OTHER_INCOME_CATEGORY);
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
  public void shouldSyncWithdrawalAndSnapshotMovement() throws ProductBusinessException {
    final var totalAmount = new BigDecimal("-100.00");
    final var balanceSnapshot = new BigDecimal("120.00");
    final var newMovement =
        EntityBuilder.with(
            accountId, today, totalAmount, balanceSnapshot, WITHDRAWAL, PERSONAL_EXPENSE_CATEGORY);

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
        EntityBuilder.with(
            accountId, today, totalAmount, balanceSnapshot, DEPOSIT, OTHER_INCOME_CATEGORY);

    // TOTAL Earn 20.00 on 2nd deposit
    final var totalAmount2 = new BigDecimal("30.00");
    final var balanceSnapshot2 = new BigDecimal("170.00");
    final var newMovement2 =
        EntityBuilder.with(
            accountId, today, totalAmount2, balanceSnapshot2, DEPOSIT, OTHER_INCOME_CATEGORY);

    // Earn 0 on the withdrawal
    final var totalAmount3 = new BigDecimal("-20.00");
    final var balanceSnapshot3 = new BigDecimal("170.00");
    final var newMovement3 =
        EntityBuilder.with(
            accountId,
            today,
            totalAmount3,
            balanceSnapshot3,
            WITHDRAWAL,
            PERSONAL_EXPENSE_CATEGORY);

    // oneHundredMonthlyBalance.syncMovements(List.of(newMovement, newMovement2, newMovement3));
    List.of(newMovement, newMovement2, newMovement3)
        .forEach(
            mov -> {
              try {
                oneHundredMonthlyBalance.assignMovement(mov);
              } catch (final ProductBusinessException e) {
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
  public void shouldSortByPeriodAscending() throws ProductBusinessException {
    final var period1 = YearMonth.of(2023, 10);
    final var period2 = YearMonth.of(2023, 11);
    final var period3 = YearMonth.of(2023, 12);
    final var period4 = YearMonth.of(2024, 1);
    final var period5 = YearMonth.of(2024, 2);

    final var balance1 = MonthlyBalanceDomain.withPeriod(accountId, period1);
    final var balance2 = MonthlyBalanceDomain.withPeriod(accountId, period2);
    final var balance3 = MonthlyBalanceDomain.withPeriod(accountId, period3);
    final var balance4 = MonthlyBalanceDomain.withPeriod(accountId, period4);
    final var balance5 = MonthlyBalanceDomain.withPeriod(accountId, period5);

    balance1.assignMovement(
        EntityBuilder.with(
            accountId,
            period1.atDay(1),
            new BigDecimal("10"),
            new BigDecimal("10"),
            DEPOSIT,
            OTHER_INCOME_CATEGORY));
    balance2.assignMovement(
        EntityBuilder.with(
            accountId,
            period2.atDay(1),
            new BigDecimal("20"),
            new BigDecimal("20"),
            DEPOSIT,
            OTHER_INCOME_CATEGORY));
    balance3.assignMovement(
        EntityBuilder.with(
            accountId,
            period3.atDay(1),
            new BigDecimal("30"),
            new BigDecimal("30"),
            DEPOSIT,
            OTHER_INCOME_CATEGORY));
    balance4.assignMovement(
        EntityBuilder.with(
            accountId,
            period4.atDay(1),
            new BigDecimal("40"),
            new BigDecimal("40"),
            DEPOSIT,
            OTHER_INCOME_CATEGORY));
    balance5.assignMovement(
        EntityBuilder.with(
            accountId,
            period5.atDay(1),
            new BigDecimal("50"),
            new BigDecimal("50"),
            DEPOSIT,
            OTHER_INCOME_CATEGORY));

    assertEquals(period1, balance1.getPeriod());
    assertEquals(period2, balance2.getPeriod());
    assertEquals(period3, balance3.getPeriod());
    assertEquals(period4, balance4.getPeriod());
    assertEquals(period5, balance5.getPeriod());
  }

  @Test
  public void shouldThrowExceptionWhenDifferentPeriod() throws ProductBusinessException {
    final LocalDate initBalanceDate = LocalDate.of(2024, 8, 1);
    final YearMonth initBalancePeriod = YearMonth.of(2024, 8);
    final MonthlyBalanceDomain accountMonthlyBalance =
        MonthlyBalanceDomain.withPeriod(accountId, initBalancePeriod);

    // Add movement with balance snapshot of 2105192.00
    final var initBalanceSnapshot = new BigDecimal("2105192.00");
    accountMonthlyBalance.assignMovement(
        EntityBuilder.with(
            accountId, initBalanceDate, null, initBalanceSnapshot, BALANCE_SNAPSHOT, null));

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
        ProductBusinessException.class,
        () -> {
          accountMonthlyBalance.assignMovement(
              EntityBuilder.with(
                  accountId, septBalanceDate, null, septBalanceSnapshot, BALANCE_SNAPSHOT, null));
        });
  }
}
