package com.jbh.preferences.infra.adapters.out.gateway;

import com.jbh.commons.exception.InternalSystemException;
import com.jbh.gateway.client.JbhGatewayException;
import com.jbh.gateway.client.JbhHttpResponse;
import com.jbh.preferences.application.core.ports.output.TeamMembershipQueryRepository;
import com.jbh.preferences.application.core.vo.CallerToken;
import com.jbh.preferences.infra.gateway.GatewayClientFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response.Status;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Asks IAM, through the gateway, for the team as the caller. IAM answers 200 to an accepted member,
 * 403 to any other user and 404 when the team does not exist.
 */
@ApplicationScoped
public class TeamMembershipGatewayAdapter implements TeamMembershipQueryRepository {

  private static final int UNAUTHORIZED = Status.UNAUTHORIZED.getStatusCode();
  private static final Set<Integer> NOT_A_MEMBER =
      Set.of(Status.FORBIDDEN.getStatusCode(), Status.NOT_FOUND.getStatusCode());
  private static final String UNAUTHENTICATED_MESSAGE = "Unauthenticated user";
  private static final String UNAUTHENTICATED_CODE = "No user found";
  private static final String LOOKUP_FAILED_MESSAGE =
      "Team membership lookup failed with HTTP status ";
  private static final String NO_ANSWER_MESSAGE =
      "Team membership lookup got no answer. Error code: ";

  private final GatewayClientFactory gatewayClientFactory;

  @Inject
  public TeamMembershipGatewayAdapter(final GatewayClientFactory gatewayClientFactory) {
    this.gatewayClientFactory = gatewayClientFactory;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Any other IAM answer, or no answer, is a server error (HTTP 500).
   */
  @Override
  public boolean isMember(final UUID teamId, final CallerToken caller)
      throws InternalSystemException {
    final JbhHttpResponse response = requestTeam(teamId, caller);
    if (response.isSuccessful()) {
      return true;
    }
    final int statusCode = response.getStatusCode();
    if (NOT_A_MEMBER.contains(statusCode)) {
      return false;
    }
    if (statusCode == UNAUTHORIZED) {
      throw new InternalSystemException(UNAUTHENTICATED_MESSAGE, UNAUTHENTICATED_CODE);
    }
    throw new IllegalStateException(LOOKUP_FAILED_MESSAGE + statusCode);
  }

  private JbhHttpResponse requestTeam(final UUID teamId, final CallerToken caller) {
    try {
      return gatewayClientFactory
          .getUserClient()
          .findUserGroup(teamId, Map.of(HttpHeaders.AUTHORIZATION, caller.value()));
    } catch (final JbhGatewayException e) {
      throw new IllegalStateException(NO_ANSWER_MESSAGE + e.getErrorCode(), e);
    }
  }
}
