package com.jbh.preferences.infra.adapters.out.gateway;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.jbh.commons.exception.InternalSystemException;
import com.jbh.gateway.client.JbhGatewayException;
import com.jbh.gateway.client.JbhHttpResponse;
import com.jbh.gateway.client.users.JbhUserGatewayClient;
import com.jbh.preferences.application.core.vo.CallerToken;
import jakarta.ws.rs.core.HttpHeaders;
import java.net.ConnectException;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TeamMembershipGatewayAdapterTest {

  private static final String BEARER_TOKEN = "Bearer test-token";
  private static final CallerToken CALLER = new CallerToken(BEARER_TOKEN);
  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final Map<String, String> HEADERS =
      Map.of(HttpHeaders.AUTHORIZATION, BEARER_TOKEN);

  @Mock private JbhUserGatewayClient userClient;

  private TeamMembershipGatewayAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new TeamMembershipGatewayAdapter(() -> userClient);
  }

  @Test
  void shouldSayMemberWhenIamAnswers200() throws Exception {
    givenIamAnswers(200);

    assertTrue(adapter.isMember(TEAM_ID, CALLER));
  }

  @Test
  void shouldSayNotAMemberWhenIamAnswers403() throws Exception {
    givenIamAnswers(403);

    assertFalse(adapter.isMember(TEAM_ID, CALLER));
  }

  @Test
  void shouldSayNotAMemberWhenTheTeamDoesNotExist() throws Exception {
    givenIamAnswers(404);

    assertFalse(adapter.isMember(TEAM_ID, CALLER));
  }

  @Test
  void shouldThrowUnauthorizedWhenIamAnswers401() throws Exception {
    givenIamAnswers(401);

    assertThrows(InternalSystemException.class, () -> adapter.isMember(TEAM_ID, CALLER));
  }

  @Test
  void shouldThrowAServerErrorWhenIamAnswers500() throws Exception {
    givenIamAnswers(500);

    assertThrows(IllegalStateException.class, () -> adapter.isMember(TEAM_ID, CALLER));
  }

  @Test
  void shouldThrowAServerErrorWithTheCauseWhenIamGivesNoAnswer() throws Exception {
    final JbhGatewayException noAnswer =
        new JbhGatewayException(
            "Connection failed", "CONNECTION_FAILED", null, new ConnectException());
    when(userClient.findUserGroup(TEAM_ID, HEADERS)).thenThrow(noAnswer);

    final IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> adapter.isMember(TEAM_ID, CALLER));

    assertSame(noAnswer, exception.getCause());
  }

  private void givenIamAnswers(final int statusCode) throws JbhGatewayException {
    when(userClient.findUserGroup(TEAM_ID, HEADERS))
        .thenReturn(JbhHttpResponse.builder().statusCode(statusCode).build());
  }
}
