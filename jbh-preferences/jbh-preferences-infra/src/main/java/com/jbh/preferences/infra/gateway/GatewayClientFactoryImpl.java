package com.jbh.preferences.infra.gateway;

import com.jbh.gateway.client.JbhGatewayClientBuilder;
import com.jbh.gateway.client.JbhGatewayException;
import com.jbh.gateway.client.users.JbhUserGatewayClient;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class GatewayClientFactoryImpl implements GatewayClientFactory {

  private static final Logger LOG = LoggerFactory.getLogger(GatewayClientFactoryImpl.class);

  private final JbhGatewayClientBuilder jbhGatewayClient;

  private JbhUserGatewayClient userGatewayClient;

  @Inject
  public GatewayClientFactoryImpl(final GatewayClientBuilder gatewayClientBuilder) {
    this.jbhGatewayClient = gatewayClientBuilder.buildGatewayClient();
  }

  @Override
  public JbhUserGatewayClient getUserClient() {
    if (userGatewayClient == null) {
      try {
        userGatewayClient = jbhGatewayClient.getUserClient();
      } catch (final JbhGatewayException e) {
        LOG.error("Error while creating user API gateway client", e);
        throw new IllegalArgumentException("Error while creating user API gateway client", e);
      }
    }
    return userGatewayClient;
  }
}
