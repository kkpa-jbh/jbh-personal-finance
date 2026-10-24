package com.jbh.preferences.infra.adapters.in.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.jbh.commons.exception.InternalSystemException;
import com.jbh.gateway.client.JbhGatewayException;
import com.jbh.gateway.client.JbhHttpResponse;
import com.jbh.gateway.client.users.JbhUserGatewayClient;
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
class BaseRestAdapterTest {

  private static final String BEARER_TOKEN = "Bearer test-token";
  private static final String UNAUTHENTICATED_MESSAGE = "Unauthenticated user";
  private static final String UNAUTHENTICATED_CODE = "No user found";

  @Mock private JbhUserGatewayClient userClient;

  private BaseRestAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new BaseRestAdapter();
    adapter.gatewayClientFactory = () -> userClient;
  }

  @Test
  void shouldReturnUserIdWhenIamReturnsValidUuid() throws Exception {
    // Given
    final UUID expectedUserId = UUID.randomUUID();
    when(userClient.findUserId(Map.of(HttpHeaders.AUTHORIZATION, BEARER_TOKEN)))
        .thenReturn(JbhHttpResponse.builder().statusCode(200).typedBody(expectedUserId).build());

    // When
    final UUID userId = adapter.findUserId(BEARER_TOKEN);

    // Then
    assertEquals(expectedUserId, userId);
  }

  @Test
  void shouldThrowUnauthorizedWithIamMessageWhenIamReturns401WithBody() throws Exception {
    // Given
    final String body = "{\"message\":\"Token expired\",\"error_code\":\"TOKEN_EXPIRED\"}";
    when(userClient.findUserId(anyMap()))
        .thenReturn(JbhHttpResponse.builder().statusCode(401).body(body).build());

    // When
    final InternalSystemException exception =
        assertThrows(InternalSystemException.class, () -> adapter.findUserId(BEARER_TOKEN));

    // Then
    assertEquals("Token expired", exception.getMessage());
    assertEquals("TOKEN_EXPIRED", exception.getErrorCode());
  }

  @Test
  void shouldThrowUnauthenticatedWhenAuthorizationHeaderIsNull() {
    // When
    final InternalSystemException exception =
        assertThrows(InternalSystemException.class, () -> adapter.findUserId(null));

    // Then
    assertEquals(UNAUTHENTICATED_MESSAGE, exception.getMessage());
    assertEquals(UNAUTHENTICATED_CODE, exception.getErrorCode());
    verifyNoInteractions(userClient);
  }

  @Test
  void shouldThrowUnauthenticatedWhenIamReturns401WithoutBody() throws Exception {
    // Given
    when(userClient.findUserId(anyMap()))
        .thenReturn(JbhHttpResponse.builder().statusCode(401).build());

    // When
    final InternalSystemException exception =
        assertThrows(InternalSystemException.class, () -> adapter.findUserId(BEARER_TOKEN));

    // Then
    assertEquals(UNAUTHENTICATED_MESSAGE, exception.getMessage());
    assertEquals(UNAUTHENTICATED_CODE, exception.getErrorCode());
  }

  @Test
  void shouldThrowUnauthenticatedWhenIamReturns401WithInvalidJsonBody() throws Exception {
    // Given
    when(userClient.findUserId(anyMap()))
        .thenReturn(JbhHttpResponse.builder().statusCode(401).body("<html>401</html>").build());

    // When
    final InternalSystemException exception =
        assertThrows(InternalSystemException.class, () -> adapter.findUserId(BEARER_TOKEN));

    // Then
    assertEquals(UNAUTHENTICATED_MESSAGE, exception.getMessage());
    assertEquals(UNAUTHENTICATED_CODE, exception.getErrorCode());
  }

  @Test
  void shouldThrowGatewayExceptionWithStatusWhenIamReturns500() throws Exception {
    // Given
    when(userClient.findUserId(anyMap()))
        .thenReturn(JbhHttpResponse.builder().statusCode(500).body("boom").build());

    // When
    final JbhGatewayException exception =
        assertThrows(JbhGatewayException.class, () -> adapter.findUserId(BEARER_TOKEN));

    // Then
    assertTrue(exception.getMessage().contains("500"), "Message should have the status code");
    assertEquals(500, exception.getHttpStatusCode());
    assertEquals("USER_ID_LOOKUP_FAILED", exception.getErrorCode());
  }

  @Test
  void shouldThrowGatewayExceptionWhenSuccessfulResponseHasNoUserId() throws Exception {
    // Given
    when(userClient.findUserId(anyMap()))
        .thenReturn(JbhHttpResponse.builder().statusCode(200).build());

    // When
    final JbhGatewayException exception =
        assertThrows(JbhGatewayException.class, () -> adapter.findUserId(BEARER_TOKEN));

    // Then
    assertEquals(200, exception.getHttpStatusCode());
  }

  @Test
  void shouldWrapGatewayExceptionWithErrorCodeWhenNetworkFails() throws Exception {
    // Given
    final JbhGatewayException networkError =
        new JbhGatewayException(
            "Connection failed", "CONNECTION_FAILED", null, new ConnectException("refused"));
    when(userClient.findUserId(anyMap())).thenThrow(networkError);

    // When
    final JbhGatewayException exception =
        assertThrows(JbhGatewayException.class, () -> adapter.findUserId(BEARER_TOKEN));

    // Then
    assertTrue(
        exception.getMessage().contains("CONNECTION_FAILED"),
        "Message should have the gateway error code");
    assertEquals("CONNECTION_FAILED", exception.getErrorCode());
    assertSame(networkError, exception.getCause());
  }
}
