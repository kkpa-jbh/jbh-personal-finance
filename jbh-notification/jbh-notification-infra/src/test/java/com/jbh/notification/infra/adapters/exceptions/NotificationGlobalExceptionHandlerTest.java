package com.jbh.notification.infra.adapters.exceptions;

import static org.junit.jupiter.api.Assertions.*;

import com.jbh.commons.api.ApiResponse;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotificationGlobalExceptionHandlerTest {

    private static final String GENERIC_ERROR_MESSAGE = "An unexpected error occurred. Please try again later.";
    private NotificationGlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new NotificationGlobalExceptionHandler();
    }

    @Test
    void shouldReturnHttp500WhenRuntimeExceptionIsThrown() {
        final RuntimeException exception = new RuntimeException("Unexpected runtime error");

        final Response response = handler.toResponse(exception);

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertNotNull(response.getEntity());
        assertInstanceOf(ApiResponse.class, response.getEntity());

        final ApiResponse<?> apiResponse = (ApiResponse<?>) response.getEntity();
        assertFalse(apiResponse.success());
        assertEquals(GENERIC_ERROR_MESSAGE, apiResponse.message());
    }

    @Test
    void shouldReturnHttp500WhenNullPointerExceptionIsThrown() {
        final NullPointerException exception = new NullPointerException("Null reference");

        final Response response = handler.toResponse(exception);

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        final ApiResponse<?> apiResponse = (ApiResponse<?>) response.getEntity();
        assertFalse(apiResponse.success());
        assertEquals(GENERIC_ERROR_MESSAGE, apiResponse.message());
    }

    @Test
    void shouldReturnHttp500WhenIllegalStateExceptionIsThrown() {
        final IllegalStateException exception = new IllegalStateException("Invalid state");

        final Response response = handler.toResponse(exception);

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        final ApiResponse<?> apiResponse = (ApiResponse<?>) response.getEntity();
        assertFalse(apiResponse.success());
        assertEquals(GENERIC_ERROR_MESSAGE, apiResponse.message());
    }

    @Test
    void shouldNotExposeInternalErrorMessageToClient() {
        final String internalMessage = "Database connection failed: password incorrect";
        final Exception exception = new Exception(internalMessage);

        final Response response = handler.toResponse(exception);

        final ApiResponse<?> apiResponse = (ApiResponse<?>) response.getEntity();
        assertNotEquals(internalMessage, apiResponse.message());
        assertEquals(GENERIC_ERROR_MESSAGE, apiResponse.message());
    }

    @Test
    void shouldReturnApiResponseWithNullDataOnError() {
        final Exception exception = new Exception("Test error");

        final Response response = handler.toResponse(exception);

        final ApiResponse<?> apiResponse = (ApiResponse<?>) response.getEntity();
        assertNull(apiResponse.data());
        assertNull(apiResponse.errorCode());
    }
}
