package com.jbh.commons.api;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class ApiResponseTest {

    @Test
    void shouldCreateSuccessResponseWithDataAndMessage() {
        final String data = "test data";
        final String message = "Custom message";

        final ApiResponse<String> response = ApiResponse.success(data, message);

        assertTrue(response.success());
        assertEquals(data, response.data());
        assertEquals(message, response.message());
        assertNull(response.errors());
        assertNull(response.errorCode());
    }

    @Test
    void shouldCreateSuccessResponseWithDefaultMessage() {
        final String data = "test data";

        final ApiResponse<String> response = ApiResponse.success(data);

        assertTrue(response.success());
        assertEquals(data, response.data());
        assertEquals("Operation completed successfully", response.message());
        assertNull(response.errors());
        assertNull(response.errorCode());
    }

    @Test
    void shouldCreateErrorResponseWithMessage() {
        final String errorMessage = "Something went wrong";

        final ApiResponse<Void> response = ApiResponse.error(errorMessage);

        assertFalse(response.success());
        assertNull(response.data());
        assertEquals(errorMessage, response.message());
        assertNull(response.errors());
        assertNull(response.errorCode());
    }

    @Test
    void shouldCreateErrorResponseWithMessageAndErrors() {
        final String errorMessage = "Validation failed";
        final List<String> errors = List.of("Field X is required", "Field Y must be positive");

        final ApiResponse<Void> response = ApiResponse.error(errorMessage, errors);

        assertFalse(response.success());
        assertNull(response.data());
        assertEquals(errorMessage, response.message());
        assertEquals(errors, response.errors());
        assertNull(response.errorCode());
    }

    @Test
    void shouldCreateErrorResponseWithErrorCode() {
        final String errorMessage = "Unauthorized access";
        final String errorCode = "AUTH_001";

        final ApiResponse<Void> response = ApiResponse.withErrorCode(errorMessage, errorCode);

        assertFalse(response.success());
        assertNull(response.data());
        assertEquals(errorMessage, response.message());
        assertNull(response.errors());
        assertEquals(errorCode, response.errorCode());
    }
}
