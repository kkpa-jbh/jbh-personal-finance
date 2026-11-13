package com.jbh.preferences.infra.gateway;

import com.jbh.gateway.client.JbhGatewayClientBuilder;
import com.jbh.preferences.infra.config.JbhGatewayClientConfigProvider;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GatewayClientBuilder {

  private static final String PREFERENCES_SERVICE_CLIENT = "JBH_PREFERENCES_API";

  private final JbhGatewayClientConfigProvider configProvider;
  // Receives the circuit breaker metrics of the gateway client (/q/metrics).
  private final MeterRegistry meterRegistry;

  @Inject
  public GatewayClientBuilder(
      final JbhGatewayClientConfigProvider configProvider, final MeterRegistry meterRegistry) {
    this.configProvider = configProvider;
    this.meterRegistry = meterRegistry;
  }

  public JbhGatewayClientBuilder buildGatewayClient() {
    return JbhGatewayClientBuilder.builder()
        .baseUrl(configProvider.baseUrl())
        .sourceService(PREFERENCES_SERVICE_CLIENT)
        .meterRegistry(meterRegistry)
        .build();
  }
}
