package com.jbh.account.infra.adapters.exceptions;

import static org.junit.jupiter.api.Assertions.*;

import com.jbh.account.domain.exceptions.BusinessDomainExceptionType;
import com.jbh.account.infra.adapters.in.rest.vo.ApiResponse;
import com.jbh.commons.exception.BusinessException;
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
    final BusinessException exception =
        new BusinessException(BusinessDomainExceptionType.EMPTY_MOVEMENTS);

    // When
    final Response response = handler.toResponse(exception);

    // Then
    assertEquals(422, response.getStatus(), "Should return HTTP 422 for AccountBusinessException");
    assertNotNull(response.getEntity(), "Response should contain an entity");
    assertTrue(
        response.getEntity() instanceof ApiResponse, "Entity should be an ApiResponse instance");

    final ApiResponse<?> apiResponse = (ApiResponse<?>) response.getEntity();
    assertFalse(apiResponse.success(), "ApiResponse success should be false");
    assertEquals(
        BusinessDomainExceptionType.EMPTY_MOVEMENTS.getMessage(),
        apiResponse.message(),
        "Error message should match");
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
    assertEquals(400, response.getStatus(), "Should return HTTP 400 for IllegalArgumentException");
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
  void shouldHandleAccountBusinessExceptionWithFormattedMessage() {
    // Given
    final String metadataKey = "REAL_ESTATE_PURCHASE_DATE";
    final BusinessException exception =
        new BusinessException(BusinessDomainExceptionType.MISSING_METADATA, metadataKey);

    // When
    final Response response = handler.toResponse(exception);

    // Then
    assertEquals(422, response.getStatus(), "Should return HTTP 422");
    final ApiResponse<?> apiResponse = (ApiResponse<?>) response.getEntity();
    assertFalse(apiResponse.success(), "ApiResponse success should be false");
    assertTrue(
        apiResponse.message().contains(metadataKey),
        "Error message should contain the metadata key");
    assertNotNull(apiResponse.errors(), "Errors list should contain the exception type message");
  }
}
