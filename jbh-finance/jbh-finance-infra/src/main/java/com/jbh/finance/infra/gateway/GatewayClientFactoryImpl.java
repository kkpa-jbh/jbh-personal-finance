package com.jbh.finance.infra.gateway;

import com.jbh.gateway.client.JbhGatewayClientBuilder;
import com.jbh.gateway.client.JbhGatewayException;
import com.jbh.gateway.client.users.JbhUserGatewayClient;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class GatewayClientFactoryImpl implements GatewayClientFactory {

  // Built once in the constructor. The final field makes it safe to share between threads.
  private final JbhUserGatewayClient userGatewayClient;

  @Inject
  public GatewayClientFactoryImpl(final GatewayClientBuilder gatewayClientBuilder) {
    this.userGatewayClient = createUserClient(gatewayClientBuilder.buildGatewayClient());
  }

  @Override
  public JbhUserGatewayClient getUserClient() {
    return userGatewayClient;
  }

  private static JbhUserGatewayClient createUserClient(
      final JbhGatewayClientBuilder gatewayClient) {
    try {
      return gatewayClient.getUserClient();
    } catch (final JbhGatewayException e) {
      throw new IllegalStateException("Error while creating user API gateway client", e);
    }
  }
}
