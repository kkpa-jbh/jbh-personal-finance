package com.jbh.preferences.infra.gateway;

import com.jbh.gateway.client.JbhGatewayClientBuilder;
import com.jbh.preferences.infra.config.JbhGatewayClientConfigProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GatewayClientBuilder {

  private static final String PREFERENCES_SERVICE_CLIENT = "JBH_PREFERENCES_API";

  private final JbhGatewayClientConfigProvider configProvider;

  @Inject
  public GatewayClientBuilder(final JbhGatewayClientConfigProvider configProvider) {
    this.configProvider = configProvider;
  }

  public JbhGatewayClientBuilder buildGatewayClient() {
    return JbhGatewayClientBuilder.builder()
        .baseUrl(configProvider.baseUrl())
        .sourceService(PREFERENCES_SERVICE_CLIENT)
        .build();
  }
}
