package com.jbh.notification.infra.adapters.in.rest.vo;

/**
 * API path constants for the Notification module.
 *
 * <p>Each infrastructure module defines its own BASE_PATH because Quarkus's {@code
 * quarkus.rest.path} property applies globally to ALL JAX-RS endpoints in the application. In a
 * modular monolith where multiple modules expose REST APIs, using a global prefix would force all
 * modules to share the same base path, which breaks domain separation.
 *
 * <p>By defining the base path per module, each bounded context maintains full control over its API
 * structure and can be independently evolved or extracted into a microservice.
 *
 * @see <a href="docs/API_ROUTING.md">API Routing Documentation</a>
 */
public final class NotificationRoutes {

  public static final String NOTIFICATIONS_PATH = "/notifications";
  private static final String API_VERSION = "/v1";
  private static final String BASE_PATH = "/jbh-api";
  public static final String NOTIFICATIONS_API_PATH_V1 =
      BASE_PATH + NOTIFICATIONS_PATH + API_VERSION;

  private NotificationRoutes() {}
}
