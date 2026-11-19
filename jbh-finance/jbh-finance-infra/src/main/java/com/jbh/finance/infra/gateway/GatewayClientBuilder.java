package com.jbh.finance.infra.gateway;

import com.jbh.finance.infra.config.JbhGatewayClientConfigProvider;
import com.jbh.gateway.client.JbhGatewayClientBuilder;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GatewayClientBuilder {

  private static final String FINANCE_SERVICE_CLIENT = "JBH_FINANCE_API";

  private final JbhGatewayClientConfigProvider configProvider;
  // Receives the circuit breaker metrics of the gateway client (/q/metrics).
  private final MeterRegistry meterRegistry;
  // Without it the client makes no span and sends no traceparent, so the trace stops here.
  private final OpenTelemetry openTelemetry;

  @Inject
  public GatewayClientBuilder(
      final JbhGatewayClientConfigProvider configProvider,
      final MeterRegistry meterRegistry,
      final OpenTelemetry openTelemetry) {
    this.configProvider = configProvider;
    this.meterRegistry = meterRegistry;
    this.openTelemetry = openTelemetry;
  }

  public JbhGatewayClientBuilder buildGatewayClient() {
    return JbhGatewayClientBuilder.builder()
        .baseUrl(configProvider.baseUrl())
        .sourceService(FINANCE_SERVICE_CLIENT)
        .meterRegistry(meterRegistry)
        .openTelemetry(openTelemetry)
        .build();
  }
}
