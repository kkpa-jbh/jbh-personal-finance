package com.jbh.account.infra.adapters.in.rest.vo;

import static com.jbh.account.infra.adapters.in.rest.vo.ApiConstants.BASE_API_PATH;

public class AccountApiRoutes {

  public static final String ACCOUNTS_API_PATH = BASE_API_PATH + "/accounts";
  public static final String ACCOUNTS_MOVEMENTS_API_PATH = "/movements";
  public static final String MOVEMENTS_INBULK_API = "/movements/upload-excel";
}
