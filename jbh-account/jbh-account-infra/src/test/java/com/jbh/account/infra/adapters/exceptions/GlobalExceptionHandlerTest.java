package com.jbh.account.infra.adapters.exceptions;

import static org.junit.jupiter.api.Assertions.*;

import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.exceptions.JbhExceptionMessage;
import com.jbh.account.infra.adapters.in.rest.vo.ApiResponse;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler handler;

  @BeforeEach
  void setUp() {
    handler = new GlobalExceptionHandler();
  }

  @Test
  void shouldReturnHttp422WhenAccountBusinessExceptionIsThrown() {
    // Given
    final String errorMessage = "Business validation failed";
    final AccountBusinessException exception =
        new AccountBusinessException(
            errorMessage, new JbhExceptionMessage("VALIDATION_ERROR", "Custom validation error"));

    // When
    final Response response = handler.toResponse(exception);

    // Then
    assertEquals(422, response.getStatus(), "Should return HTTP 422 for AccountBusinessException");
    assertNotNull(response.getEntity(), "Response should contain an entity");
    assertTrue(
        response.getEntity() instanceof ApiResponse, "Entity should be an ApiResponse instance");

    final ApiResponse<?> apiResponse = (ApiResponse<?>) response.getEntity();
    assertFalse(apiResponse.success(), "ApiResponse success should be false");
    assertEquals(errorMessage, apiResponse.message(), "Error message should match");
    assertNotNull(apiResponse.errors(), "Errors list should not be null");
  }

  @Test
  void shouldReturnHttp400WhenIllegalArgumentExceptionIsThrown() {
    // Given
    final String errorMessage = "Invalid argument provided";
    final IllegalArgumentException exception = new IllegalArgumentException(errorMessage);

    // When
    final Response response = handler.toResponse(exception);

    // Then
    assertEquals(
        400, response.getStatus(), "Should return HTTP 400 for IllegalArgumentException");
    assertNotNull(response.getEntity(), "Response should contain an entity");
    assertTrue(
        response.getEntity() instanceof ApiResponse, "Entity should be an ApiResponse instance");

    final ApiResponse<?> apiResponse = (ApiResponse<?>) response.getEntity();
    assertFalse(apiResponse.success(), "ApiResponse success should be false");
    assertEquals(errorMessage, apiResponse.message(), "Error message should match");
  }

  @Test
  void shouldReturnHttp500WhenGenericExceptionIsThrown() {
    // Given
    final Exception exception = new RuntimeException("Unexpected error");

    // When
    final Response response = handler.toResponse(exception);

    // Then
    assertEquals(500, response.getStatus(), "Should return HTTP 500 for generic exceptions");
    assertNotNull(response.getEntity(), "Response should contain an entity");
    assertTrue(
        response.getEntity() instanceof ApiResponse, "Entity should be an ApiResponse instance");

    final ApiResponse<?> apiResponse = (ApiResponse<?>) response.getEntity();
    assertFalse(apiResponse.success(), "ApiResponse success should be false");
    assertEquals(
        "An unexpected error occurred. Please try again later.",
        apiResponse.message(),
        "Should return generic error message");
  }

  @Test
  void shouldHandleAccountBusinessExceptionWithoutCustomMessage() {
    // Given
    final String errorMessage = "Business error without custom message";
    final AccountBusinessException exception = new AccountBusinessException(errorMessage, null);

    // When
    final Response response = handler.toResponse(exception);

    // Then
    assertEquals(422, response.getStatus(), "Should return HTTP 422");
    final ApiResponse<?> apiResponse = (ApiResponse<?>) response.getEntity();
    assertFalse(apiResponse.success(), "ApiResponse success should be false");
    assertEquals(errorMessage, apiResponse.message(), "Error message should match");
    assertNull(apiResponse.errors(), "Errors list should be null when no custom message");
  }
}