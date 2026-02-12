package com.jbh.finance.infra.adapters.in.rest.common;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.InternalSystemException;
import com.jbh.commons.util.JbhJsonUtils;
import com.jbh.finance.infra.gateway.GatewayClientFactory;
import com.jbh.gateway.client.JbhGatewayException;
import com.jbh.gateway.client.JbhHttpResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response.Status;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class BaseRestAdapter {

  @Inject GatewayClientFactory gatewayClientFactory;

  protected UUID findUserId(final String authorizationHeader)
      throws JbhGatewayException, BusinessException, InternalSystemException {

    if (authorizationHeader == null) {
      throw new InternalSystemException("Unathenticated user", "No user found");
    }

    final UUID userId;
    final JbhHttpResponse gatewayResponse =
        gatewayClientFactory
            .getUserClient()
            .findUserId(Map.of(HttpHeaders.AUTHORIZATION, authorizationHeader));

    if (gatewayResponse.isSuccessful() && gatewayResponse.getBodyAs(UUID.class).isPresent()) {
      userId = gatewayResponse.getBodyAs(UUID.class).get();
    } else {
      if (gatewayResponse.getStatusCode() == Status.UNAUTHORIZED.getStatusCode()) {
        final Optional<String> body = gatewayResponse.getBody();
        if (body.isPresent()) {
          final Map<String, String> bodyMap = JbhJsonUtils.jsonToMap(body.get());
          throw new InternalSystemException(bodyMap.get("message"), bodyMap.get("error_code"));
        }
      }
      throw new IllegalArgumentException("Invalid user ID");
    }
    return userId;
  }
}
