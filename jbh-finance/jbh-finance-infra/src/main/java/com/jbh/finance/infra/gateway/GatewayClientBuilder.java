package com.jbh.finance.infra.gateway;

import com.jbh.finance.infra.config.JbhGatewayClientConfigProvider;
import com.jbh.gateway.client.JbhGatewayClientBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GatewayClientBuilder {

  private static final String FINANCE_SERVICE_CLIENT = "JBH_FINANCE_API";

  private final JbhGatewayClientConfigProvider configProvider;

  @Inject
  public GatewayClientBuilder(final JbhGatewayClientConfigProvider configProvider) {
    this.configProvider = configProvider;
  }

  public JbhGatewayClientBuilder buildGatewayClient() {
    return JbhGatewayClientBuilder.builder()
        .baseUrl(configProvider.baseUrl())
        .sourceService(FINANCE_SERVICE_CLIENT)
        .build();
  }
}
