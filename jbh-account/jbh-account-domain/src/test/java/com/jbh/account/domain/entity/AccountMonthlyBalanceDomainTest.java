package com.jbh.account.domain.entity;

import static com.jbh.account.domain.utils.MoneyUtils.JBH_ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.jbh.account.domain.vo.AccountId;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class AccountMonthlyBalanceDomainTest {
  AccountMonthlyBalanceDomain ceroMonthlyBalance;
  AccountMonthlyBalanceDomain oneHundredMonthlyBalance;
  LocalDate today = LocalDate.now();
  YearMonth todayYM = YearMonth.now();
  AccountId accountId = AccountId.generate();

  @BeforeEach
  public void setUp() {
    ceroMonthlyBalance =
        AccountMonthlyBalanceDomain.withPeriod(accountId, today.getYear(), today.getMonthValue());

    oneHundredMonthlyBalance =
        AccountMonthlyBalanceDomain.withPeriod(accountId, today.getYear(), today.getMonthValue());
    oneHundredMonthlyBalance.syncMovement(
        AccountMovementDomain.withBalances(
            accountId, today, new BigDecimal("100.00"), new BigDecimal("100.00")));
  }

  @Test
  public void shouldCreateWithPeriod() {
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
    assertEquals(JBH_ZERO, ceroMonthlyBalance.getMonthlyProfit());

    ceroMonthlyBalance.syncMovement(
        AccountMovementDomain.withBalances(accountId, today, JBH_ZERO, JBH_ZERO));
    assertEquals(JBH_ZERO, ceroMonthlyBalance.getMonthlyProfit());
  }

  @Test
  public void shouldCreateWithOpeningBalance() {
    ceroMonthlyBalance.withOpeningBalance(new BigDecimal("350.00"));
    assertEquals(new BigDecimal("350.00"), ceroMonthlyBalance.getOpeningBalance());

    ceroMonthlyBalance.withOpeningBalance(null);
    assertEquals(JBH_ZERO, ceroMonthlyBalance.getOpeningBalance());
  }

  @Test
  public void shouldCreateWithClosingBalance() {
    final var closingBalance = new BigDecimal("200.00");
    final var newBalance =
        AccountMonthlyBalanceDomain.withClosingBalance(accountId, todayYM, closingBalance, false);

    assertEquals(closingBalance, newBalance.getClosingBalance());
    assertEquals(JBH_ZERO, newBalance.getTotalDebits());
    assertEquals(JBH_ZERO, newBalance.getTotalCredits());
    assertEquals(0, newBalance.getTotalMovements());
    assertEquals(JBH_ZERO, newBalance.getOpeningBalance());
    assertEquals(closingBalance, newBalance.getClosingBalance());
    assertEquals(JBH_ZERO, newBalance.getMonthlyProfit());
    assertFalse(newBalance.isGapPeriod());
  }

  @Test
  public void shouldSyncDepositAndSnapshotMovement() {
    final var totalAmount = new BigDecimal("100.00");
    final var balanceSnapshot = new BigDecimal("120.00");
    final var newMovement =
        AccountMovementDomain.withBalances(accountId, today, totalAmount, balanceSnapshot);

    ceroMonthlyBalance.syncMovement(newMovement);

    assertEquals(totalAmount, ceroMonthlyBalance.getTotalDebits());
    assertEquals(JBH_ZERO, ceroMonthlyBalance.getTotalCredits());
    assertEquals(totalAmount, ceroMonthlyBalance.getMovementBalance());
    assertEquals(JBH_ZERO, ceroMonthlyBalance.getOpeningBalance());
    assertEquals(balanceSnapshot, ceroMonthlyBalance.getClosingBalance());
    assertEquals(1, ceroMonthlyBalance.getTotalMovements());
    assertEquals(new BigDecimal("20.00"), ceroMonthlyBalance.getMonthlyProfit());
    assertFalse(ceroMonthlyBalance.isGapPeriod());
  }

  @Test
  @DisplayName("First deposit to monthly balance should not have profit")
  public void shouldSyncDepositWithoutSnapshotMovement() {
    final var totalAmount = new BigDecimal("100.00");
    final var newMovement = AccountMovementDomain.withBalances(accountId, today, totalAmount, null);

    ceroMonthlyBalance.syncMovement(newMovement);

    assertEquals(totalAmount, ceroMonthlyBalance.getTotalDebits());
    assertEquals(JBH_ZERO, ceroMonthlyBalance.getTotalCredits());
    assertEquals(totalAmount, ceroMonthlyBalance.getMovementBalance());
    assertEquals(JBH_ZERO, ceroMonthlyBalance.getOpeningBalance());
    assertEquals(totalAmount, ceroMonthlyBalance.getClosingBalance());
    assertEquals(1, ceroMonthlyBalance.getTotalMovements());
    assertEquals(JBH_ZERO, ceroMonthlyBalance.getMonthlyProfit());
    assertFalse(ceroMonthlyBalance.isGapPeriod());
  }

  @Test
  public void shouldSyncDepositAndSnapshotMovementOneHundredBalance() {
    final var totalAmount = new BigDecimal("115.00");
    final var balanceSnapshot = new BigDecimal("230.00");
    final var newMovement =
        AccountMovementDomain.withBalances(accountId, today, totalAmount, balanceSnapshot);

    final BigDecimal existingTotalDebits = oneHundredMonthlyBalance.getTotalDebits();

    oneHundredMonthlyBalance.syncMovement(newMovement);

    assertEquals(totalAmount.add(existingTotalDebits), oneHundredMonthlyBalance.getTotalDebits());
    assertEquals(JBH_ZERO, oneHundredMonthlyBalance.getTotalCredits());
    assertEquals(
        totalAmount.add(existingTotalDebits), oneHundredMonthlyBalance.getMovementBalance());
    assertEquals(JBH_ZERO, oneHundredMonthlyBalance.getOpeningBalance());
    assertEquals(balanceSnapshot, oneHundredMonthlyBalance.getClosingBalance());
    assertEquals(2, oneHundredMonthlyBalance.getTotalMovements());
    assertEquals(new BigDecimal("15.00"), oneHundredMonthlyBalance.getMonthlyProfit());
    assertFalse(oneHundredMonthlyBalance.isGapPeriod());
  }

  @Test
  public void shouldSyncDepositWithoutSnapshotMovementOneHundred() {
    final var totalAmount = new BigDecimal("50.00");
    final var newMovement = AccountMovementDomain.withBalances(accountId, today, totalAmount, null);
    final BigDecimal existingTotalDebits = oneHundredMonthlyBalance.getTotalDebits();
    oneHundredMonthlyBalance.syncMovement(newMovement);

    final AccountMonthlyBalanceDomain newBalance = oneHundredMonthlyBalance;

    assertEquals(totalAmount.add(existingTotalDebits), newBalance.getTotalDebits());
    assertEquals(JBH_ZERO, newBalance.getTotalCredits());
    assertEquals(totalAmount.add(existingTotalDebits), newBalance.getMovementBalance());
    assertEquals(JBH_ZERO, newBalance.getOpeningBalance());
    assertEquals(totalAmount.add(existingTotalDebits), newBalance.getClosingBalance());
    assertEquals(2, newBalance.getTotalMovements());
    assertEquals(JBH_ZERO, newBalance.getMonthlyProfit());
    assertFalse(newBalance.isGapPeriod());
  }

  @Test
  public void shouldSyncWithdrawalAndSnapshotMovement() {
    final var totalAmount = new BigDecimal("-100.00");
    final var balanceSnapshot = new BigDecimal("120.00");
    final var newMovement =
        AccountMovementDomain.withBalances(accountId, today, totalAmount, balanceSnapshot);

    ceroMonthlyBalance.syncMovement(newMovement);

    assertEquals(JBH_ZERO, ceroMonthlyBalance.getTotalDebits());
    assertEquals(totalAmount.abs(), ceroMonthlyBalance.getTotalCredits());
    assertEquals(totalAmount, ceroMonthlyBalance.getMovementBalance());
    assertEquals(JBH_ZERO, ceroMonthlyBalance.getOpeningBalance());
    assertEquals(balanceSnapshot, ceroMonthlyBalance.getClosingBalance());
    assertEquals(1, ceroMonthlyBalance.getTotalMovements());
    assertEquals(new BigDecimal("220.00"), ceroMonthlyBalance.getMonthlyProfit());
    assertFalse(ceroMonthlyBalance.isGapPeriod());
  }

  @Test
  public void shouldSyncMultiMovementsOneHundredBalance() {
    // Earn 10.00 on 1st deposit
    final var totalAmount = new BigDecimal("10.00");
    final var balanceSnapshot = new BigDecimal("120.00");
    final var newMovement =
        AccountMovementDomain.withBalances(accountId, today, totalAmount, balanceSnapshot);

    // Earn 5.00 on 2nd deposit
    final var totalAmount2 = new BigDecimal("30.00");
    final var balanceSnapshot2 = new BigDecimal("170.00");
    final var newMovement2 =
        AccountMovementDomain.withBalances(accountId, today, totalAmount2, balanceSnapshot2);

    // Earn 0 on the withdrawal
    final var totalAmount3 = new BigDecimal("-20.00");
    final var balanceSnapshot3 = new BigDecimal("170.00");
    final var newMovement3 =
        AccountMovementDomain.withBalances(accountId, today, totalAmount3, balanceSnapshot3);

    oneHundredMonthlyBalance.syncMovements(List.of(newMovement, newMovement2, newMovement3));

    final AccountMonthlyBalanceDomain newBalance = oneHundredMonthlyBalance;
    assertEquals(4, newBalance.getTotalMovements());
    assertEquals(new BigDecimal("140.00"), newBalance.getTotalDebits());
    assertEquals(new BigDecimal("20.00"), newBalance.getTotalCredits());
    assertEquals(new BigDecimal("120.00"), newBalance.getMovementBalance());
    assertEquals(new BigDecimal("170.00"), newBalance.getClosingBalance());
    assertEquals(new BigDecimal("0.00"), newBalance.getOpeningBalance());
    assertEquals(new BigDecimal("50.00"), newBalance.getMonthlyProfit());
    assertFalse(newBalance.isGapPeriod());
  }

  @Test
  public void shouldSortByPeriodAscending() {
    final var period1 = YearMonth.of(2023, 10);
    final var period2 = YearMonth.of(2023, 11);
    final var period3 = YearMonth.of(2023, 12);
    final var period4 = YearMonth.of(2024, 1);
    final var period5 = YearMonth.of(2024, 2);

    final var balance1 =
        AccountMonthlyBalanceDomain.withPeriod(
            accountId, period1.getYear(), period1.getMonthValue());
    final var balance2 =
        AccountMonthlyBalanceDomain.withPeriod(
            accountId, period2.getYear(), period2.getMonthValue());
    final var balance3 =
        AccountMonthlyBalanceDomain.withPeriod(
            accountId, period3.getYear(), period3.getMonthValue());
    final var balance4 =
        AccountMonthlyBalanceDomain.withPeriod(
            accountId, period4.getYear(), period4.getMonthValue());
    final var balance5 =
        AccountMonthlyBalanceDomain.withPeriod(
            accountId, period5.getYear(), period5.getMonthValue());

    balance1.syncMovement(
        AccountMovementDomain.withBalances(
            accountId, today, new BigDecimal("10"), new BigDecimal("10")));
    balance2.syncMovement(
        AccountMovementDomain.withBalances(
            accountId, today, new BigDecimal("20"), new BigDecimal("20")));
    balance3.syncMovement(
        AccountMovementDomain.withBalances(
            accountId, today, new BigDecimal("30"), new BigDecimal("30")));
    balance4.syncMovement(
        AccountMovementDomain.withBalances(
            accountId, today, new BigDecimal("40"), new BigDecimal("40")));
    balance5.syncMovement(
        AccountMovementDomain.withBalances(
            accountId, today, new BigDecimal("50"), new BigDecimal("50")));

    assertEquals(period1, balance1.getPeriod());
    assertEquals(period2, balance2.getPeriod());
    assertEquals(period3, balance3.getPeriod());
    assertEquals(period4, balance4.getPeriod());
    assertEquals(period5, balance5.getPeriod());

    final var allMonthlyBalances =
        new java.util.ArrayList<>(List.of(balance5, balance3, balance2, balance4, balance1));
    Collections.sort(allMonthlyBalances);
    assertEquals(period1, allMonthlyBalances.get(0).getPeriod());
    assertEquals(period2, allMonthlyBalances.get(1).getPeriod());
    assertEquals(period3, allMonthlyBalances.get(2).getPeriod());
    assertEquals(period4, allMonthlyBalances.get(3).getPeriod());
    assertEquals(period5, allMonthlyBalances.get(4).getPeriod());
  }
}
