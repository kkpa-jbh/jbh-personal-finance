package com.jbh.finance.infra.adapters.in.rest.common;

/**
 * API path constants for the Account/Finance module.
 *
 * <p>Each infrastructure module defines its own BASE_API_PATH because Quarkus's {@code
 * quarkus.rest.path} property applies globally to ALL JAX-RS endpoints in the application. In a
 * modular monolith where multiple modules expose REST APIs, using a global prefix would force all
 * modules to share the same base path, which breaks domain separation.
 *
 * <p>By defining the base path per module, each bounded context maintains full control over its API
 * structure and can be independently evolved or extracted into a microservice.
 *
 * @see <a href="docs/API_ROUTING.md">API Routing Documentation</a>
 */
public final class ApiConstants {

  public static final String BASE_API_PATH = "/jbh-api/finance";

  private ApiConstants() {}
}
