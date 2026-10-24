package com.jbh.finance.test.testfixtures.fakes.gateway;

import com.jbh.finance.infra.gateway.GatewayClientFactory;
import com.jbh.gateway.client.JbhHttpResponse;
import com.jbh.gateway.client.users.JbhUserGatewayClient;
import io.quarkus.test.Mock;
import jakarta.ws.rs.core.HttpHeaders;
import java.util.Map;
import java.util.UUID;

/**
 * Replaces the gateway call to IAM in every {@code @QuarkusTest}. Each known token maps to a fixed
 * user id, so a test can act as two different users. An unknown token gets HTTP 401, like IAM.
 */
@Mock
public class FakeGatewayClientFactory implements GatewayClientFactory {

  public static final String USER_A_TOKEN = "Bearer user-a";
  public static final String USER_B_TOKEN = "Bearer user-b";
  public static final UUID USER_A_ID = UUID.fromString("00000000-0000-0000-0000-00000000000a");
  public static final UUID USER_B_ID = UUID.fromString("00000000-0000-0000-0000-00000000000b");

  private static final int OK = 200;
  private static final int UNAUTHORIZED = 401;
  private static final Map<String, UUID> USER_IDS_BY_TOKEN =
      Map.of(USER_A_TOKEN, USER_A_ID, USER_B_TOKEN, USER_B_ID);

  @Override
  public JbhUserGatewayClient getUserClient() {
    return headers -> {
      final UUID userId = USER_IDS_BY_TOKEN.get(headers.get(HttpHeaders.AUTHORIZATION));
      return userId == null
          ? JbhHttpResponse.builder().statusCode(UNAUTHORIZED).build()
          : JbhHttpResponse.builder().statusCode(OK).typedBody(userId).build();
    };
  }
}
