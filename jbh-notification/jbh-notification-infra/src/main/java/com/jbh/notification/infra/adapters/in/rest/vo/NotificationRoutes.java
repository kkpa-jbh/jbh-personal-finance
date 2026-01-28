package com.jbh.notification.infra.adapters.in.rest.vo;

public final class NotificationRoutes {

  public static final String NOTIFICATIONS_PATH = "/notifications";
  private static final String API_VERSION = "/v1";
  private static final String BASE_PATH = "/jbh-api";
  public static final String NOTIFICATIONS_API_PATH_V1 =
      BASE_PATH + NOTIFICATIONS_PATH + API_VERSION;

  private NotificationRoutes() {}
}
