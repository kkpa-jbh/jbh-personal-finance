package com.jbh.account.infra.gateway;

import com.jbh.account.infra.config.JbhGatewayClientConfigProvider;
import com.jbh.gateway.client.JbhGatewayClient;
import com.jbh.gateway.client.users.JbhUserApiGatewayClient;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class JbhGatewayClientsImpl implements JbhGatewayClients {

  private static final String ACCOUNT_SERVICE_CLIENT = "ACCOUNT_SERVICE";

  private final JbhGatewayClient jbhGatewayClient;

  @Inject
  public JbhGatewayClientsImpl(final JbhGatewayClientConfigProvider configProvider) {
    this.jbhGatewayClient = JbhGatewayClient.builder()
        .baseUrl(configProvider.baseUrl())
        .sourceService(ACCOUNT_SERVICE_CLIENT)
        .build();
  }

  @Override
  public JbhUserApiGatewayClient getUserClient() {
    System.out.println("JbhGatewayClientsImpl.getUserClient");
    return jbhGatewayClient.getUserClient();
  }
}
