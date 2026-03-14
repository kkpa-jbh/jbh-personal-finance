package com.jbh.finance.test.testfixtures.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jbh.finance.application.feature.product.dto.ProductDTO;

public class ProductITUtils {

  public static void assertProduct(
      final ProductDTO expected,
      final ProductDTO actual,
      final IgnoreProductOptions... ignoreOptions) {
    boolean ignoreAccountName = false;
    boolean ignoreAccountType = false;
    boolean ignoreAccountProfit = false;
    boolean ignoreNetGrowthRate = false;
    // Process provided ignore options
    if (ignoreOptions != null) {
      for (final IgnoreProductOptions option : ignoreOptions) {

        switch (option) {
          case IGNORE_ACCOUNT_NAME -> ignoreAccountName = true;
          case IGNORE_ACCOUNT_TYPE -> ignoreAccountType = true;
          case IGNORE_ACCOUNT_PROFIT -> ignoreAccountProfit = true;
          case IGNORE_NET_GROWTH_RATE -> ignoreNetGrowthRate = true;
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
      assertEquals(expected.netProfitBalance(), actual.netProfitBalance(), "Profit Balance");
    }
    assertEquals(expected.isActive(), actual.isActive(), "Is Active");

    if (!ignoreNetGrowthRate) {
      assertEquals(expected.netGrowthRate(), actual.netGrowthRate(), "Product Net Growth Rate");
    }
  }

  public static void assertProduct(final ProductDTO expected, final ProductDTO actual) {
    assertEquals(expected.id(), actual.id(), "Account ID");
    assertEquals(expected.name(), actual.name(), "Account Name");
    assertEquals(expected.type(), actual.type(), "Account Type");
    assertEquals(expected.userId(), actual.userId(), "User ID");
    assertEquals(expected.movementBalance(), actual.movementBalance(), "Account Movement Balance");
    assertEquals(expected.currentBalance(), actual.currentBalance(), "Account Current Balance");
    assertEquals(expected.netProfitBalance(), actual.netProfitBalance(), "Account Profit Balance");
    assertEquals(expected.isActive(), actual.isActive(), "Is Active");

    assertEquals(expected.netGrowthRate(), actual.netGrowthRate(), " Product Net Growth Rate");
  }
}
