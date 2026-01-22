package com.jbh.account.infra.adapters.in.rest.vo;

import static com.jbh.account.infra.adapters.in.rest.vo.ApiConstants.BASE_API_PATH;

@SuppressWarnings("PMD.LongVariable")
public class AccountApiRoutes {
  public static final String MOVEMENTS_INBULK_API = "/movements/upload-excel";

  // Accounts API
  public static final String PRODUCTS_API_PATH = BASE_API_PATH + "/products";
  public static final String PRODUCTS_MOVEMENTS_API_PATH = "/movements";
  public static final String PRODUCTS_MONTHLY_BALANCES_API_PATH = "/monthly-balances";
}
