package com.jbh.account.infra.adapters.in.rest;

import com.jbh.account.infra.gateway.GatewayClientFactory;
import com.jbh.gateway.client.JbhGatewayException;
import com.jbh.gateway.client.JbhHttpResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class BaseRestAdapter {

  @Inject GatewayClientFactory gatewayClientFactory;

  protected UUID findUserId(final String authorizationHeader) throws JbhGatewayException {
    final UUID userId;
    final JbhHttpResponse gatewayResponse =
        gatewayClientFactory
            .getUserClient()
            .findUserId(Map.of(HttpHeaders.AUTHORIZATION, authorizationHeader));

    if (gatewayResponse.isSuccessful() && gatewayResponse.getBodyAs(UUID.class).isPresent()) {
      userId = gatewayResponse.getBodyAs(UUID.class).get();
    } else {
      throw new IllegalArgumentException("Invalid user ID");
    }
    return userId;
  }
}
