package com.jbh.finance.test.testfixtures.fakes.gateway;

import io.opentelemetry.api.OpenTelemetry;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * Gives every {@code @QuarkusTest} of this module an {@link OpenTelemetry} bean. The real bean
 * comes from the quarkus-opentelemetry extension, which only the assembly module has. Without this,
 * {@code GatewayClientBuilder} cannot be injected and the test app does not start.
 */
@Singleton
public class NoopOpenTelemetryProducer {

  /**
   * Builds an OpenTelemetry that records and sends nothing.
   *
   * @return the no-op OpenTelemetry
   */
  @Produces
  @Singleton
  public OpenTelemetry openTelemetry() {
    return OpenTelemetry.noop();
  }
}
