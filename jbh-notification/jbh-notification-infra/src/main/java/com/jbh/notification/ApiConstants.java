package com.jbh.notification;

/**
 * API path constants for the Notification module.
 *
 * <p>Each infrastructure module defines its own BASE_PATH because Quarkus's
 * {@code quarkus.rest.path} property applies globally to ALL JAX-RS endpoints in the application.
 * In a modular monolith where multiple modules expose REST APIs, using a global prefix would
 * force all modules to share the same base path, which breaks domain separation.
 *
 * <p>By defining the base path per module, each bounded context maintains full control over
 * its API structure and can be independently evolved or extracted into a microservice.
 *
 * @see <a href="docs/API_ROUTING.md">API Routing Documentation</a>
 */
public final class ApiConstants {
    public static final String BASE_PATH = "/jbh-api/notifications";

    private ApiConstants() {}
}