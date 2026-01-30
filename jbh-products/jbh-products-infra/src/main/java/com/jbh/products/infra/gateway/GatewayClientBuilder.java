package com.jbh.products.infra.gateway;

import com.jbh.products.infra.config.JbhGatewayClientConfigProvider;
import com.jbh.gateway.client.JbhGatewayClientBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GatewayClientBuilder {

  private static final String ACCOUNT_SERVICE_CLIENT = "JBH_PRODUCTS_API";

  private final JbhGatewayClientConfigProvider configProvider;

  @Inject
  public GatewayClientBuilder(final JbhGatewayClientConfigProvider configProvider) {
    this.configProvider = configProvider;
  }

  public JbhGatewayClientBuilder buildGatewayClient() {
    return JbhGatewayClientBuilder.builder()
        .baseUrl(configProvider.baseUrl())
        .sourceService(ACCOUNT_SERVICE_CLIENT)
        .build();
  }
}
