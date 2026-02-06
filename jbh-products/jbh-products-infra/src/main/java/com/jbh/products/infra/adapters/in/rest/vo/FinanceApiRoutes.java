package com.jbh.products.infra.adapters.in.rest.vo;

import static com.jbh.products.infra.adapters.in.rest.vo.ApiConstants.BASE_API_PATH;

@SuppressWarnings("PMD.LongVariable")
public class FinanceApiRoutes {
  public static final String MOVEMENTS_INBULK_API = "/movements/upload-excel";

  // Products API
  public static final String PRODUCTS_API_PATH = BASE_API_PATH + "/products";
  public static final String PRODUCTS_MONTHLY_BALANCES_API_PATH = "/monthly-balances";

  // Transfers API
  public static final String TRANSFERS_API_PATH = BASE_API_PATH + "/products/transfers";

  // Movements API
  public static final String MOVEMENTS_API_PATH = BASE_API_PATH + "/products/movements";

  // Product Types API
  public static final String PRODUCT_TYPES_API_PATH = BASE_API_PATH + "/product-types";

  // Categories API
  public static final String EXPENSE_CATEGORIES_API_PATH = BASE_API_PATH + "/categories/expenses";
  public static final String INCOME_CATEGORIES_API_PATH = BASE_API_PATH + "/categories/incomes";
}
