package com.jbh.account.application.core.usecases.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jbh.account.application.core.dto.AccountDTO;

public class AccountITUtils {

  public static void assertAccount(
      final AccountDTO expected,
      final AccountDTO actual,
      final IgnoreAccountOptions... ignoreOptions) {
    boolean ignoreAccountName = false;
    boolean ignoreAccountType = false;
    boolean ignoreAccountProfit = false;
    // Process provided ignore options
    if (ignoreOptions != null) {
      for (final IgnoreAccountOptions option : ignoreOptions) {

        switch (option) {
          case IGNORE_ACCOUNT_NAME -> ignoreAccountName = true;
          case IGNORE_ACCOUNT_TYPE -> ignoreAccountType = true;
          case IGNORE_ACCOUNT_PROFIT -> ignoreAccountProfit = true;
        }
      }
    }

    assertEquals(expected.id(), actual.id(), "Account ID");
    if (!ignoreAccountName) {
      assertEquals(expected.name(), actual.name(), "Account Name");
    }
    if (!ignoreAccountType) {
      assertEquals(expected.type(), actual.type(), "Account Type");
    }
    assertEquals(expected.userId(), actual.userId(), "User ID");
    assertEquals(expected.movementBalance(), actual.movementBalance(), "Movement Balance");
    assertEquals(expected.currentBalance(), actual.currentBalance(), "Current Balance");
    if (!ignoreAccountProfit) {
      assertEquals(expected.profitBalance(), actual.profitBalance(), "Profit Balance");
    }
    assertEquals(expected.isActive(), actual.isActive(), "Is Active");
    assertEquals(
        expected.advertisedAnnualRate(), actual.advertisedAnnualRate(), "Advertised Annual Rate");
    assertEquals(
        expected.estimatedAnnualYield(), actual.estimatedAnnualYield(), "Estimated Annual Yield");
  }

  public static void assertAccount(final AccountDTO expected, final AccountDTO actual) {
    assertEquals(expected.id(), actual.id(), "Account ID");
    assertEquals(expected.name(), actual.name(), "Account Name");
    assertEquals(expected.type(), actual.type(), "Account Type");
    assertEquals(expected.userId(), actual.userId(), "User ID");
    assertEquals(expected.movementBalance(), actual.movementBalance(), "Account Movement Balance");
    assertEquals(expected.currentBalance(), actual.currentBalance(), "Account Current Balance");
    assertEquals(expected.profitBalance(), actual.profitBalance(), "Account Profit Balance");
    assertEquals(expected.isActive(), actual.isActive(), "Is Active");
    assertEquals(
        expected.advertisedAnnualRate(), actual.advertisedAnnualRate(), "Advertised Annual Rate");
    assertEquals(
        expected.estimatedAnnualYield(), actual.estimatedAnnualYield(), "Estimated Annual Yield");
  }
}
