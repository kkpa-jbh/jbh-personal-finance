package com.jbh.preferences.infra.adapters.in.rest;

import com.jbh.commons.exception.InternalSystemException;
import com.jbh.commons.util.JbhJsonUtils;
import com.jbh.gateway.client.JbhGatewayException;
import com.jbh.gateway.client.JbhHttpResponse;
import com.jbh.preferences.infra.gateway.GatewayClientFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response.Status;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class BaseRestAdapter {

  private static final int UNAUTHORIZED = Status.UNAUTHORIZED.getStatusCode();
  private static final String UNAUTHENTICATED_MESSAGE = "Unauthenticated user";
  private static final String UNAUTHENTICATED_CODE = "No user found";
  private static final String MESSAGE_KEY = "message";
  private static final String ERROR_CODE_KEY = "error_code";
  private static final String LOOKUP_FAILED_CODE = "USER_ID_LOOKUP_FAILED";
  private static final String LOOKUP_FAILED_MESSAGE = "User id lookup failed with HTTP status ";
  private static final String NO_ANSWER_MESSAGE = "User id lookup got no answer. Error code: ";

  @Inject GatewayClientFactory gatewayClientFactory;

  /**
   * Asks IAM, through the gateway, for the id of the user in the JWT.
   *
   * @throws InternalSystemException when the user is not authenticated (HTTP 401)
   * @throws JbhGatewayException when the lookup fails for any other reason (HTTP 500)
   */
  protected UUID findUserId(final String authorizationHeader)
      throws JbhGatewayException, InternalSystemException {
    if (authorizationHeader == null) {
      throw new InternalSystemException(UNAUTHENTICATED_MESSAGE, UNAUTHENTICATED_CODE);
    }

    final JbhHttpResponse gatewayResponse = requestUserId(authorizationHeader);
    final Optional<UUID> userId = gatewayResponse.getBodyAs(UUID.class);
    if (!gatewayResponse.isSuccessful() || userId.isEmpty()) {
      throwLookupError(gatewayResponse);
    }
    return userId.orElseThrow();
  }

  private JbhHttpResponse requestUserId(final String authorizationHeader)
      throws JbhGatewayException {
    try {
      return gatewayClientFactory
          .getUserClient()
          .findUserId(Map.of(HttpHeaders.AUTHORIZATION, authorizationHeader));
    } catch (final JbhGatewayException e) {
      throw new JbhGatewayException(
          NO_ANSWER_MESSAGE + e.getErrorCode(), e.getErrorCode(), e.getHttpStatusCode(), e);
    }
  }

  /** Maps a failed IAM answer to an exception. It always throws. */
  private static void throwLookupError(final JbhHttpResponse gatewayResponse)
      throws InternalSystemException, JbhGatewayException {
    final int statusCode = gatewayResponse.getStatusCode();
    final Optional<String> body = gatewayResponse.getBody();
    if (statusCode == UNAUTHORIZED && body.isPresent()) {
      final Map<String, String> bodyMap = JbhJsonUtils.jsonToMap(body.get());
      throw new InternalSystemException(bodyMap.get(MESSAGE_KEY), bodyMap.get(ERROR_CODE_KEY));
    }
    if (statusCode == UNAUTHORIZED) {
      throw new InternalSystemException(UNAUTHENTICATED_MESSAGE, UNAUTHENTICATED_CODE);
    }
    throw new JbhGatewayException(
        LOOKUP_FAILED_MESSAGE + statusCode, LOOKUP_FAILED_CODE, statusCode);
  }
}
