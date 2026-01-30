package com.jbh.commons.api;

import java.util.List;

/**
 * Standard API response wrapper for consistent response format across all modules.
 *
 * @param <T> The type of data being returned
 */
public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        List<String> errors,
        String errorCode) {

    public static <T> ApiResponse<T> success(final T data, final String message) {
        return new ApiResponse<>(true, data, message, null, null);
    }

    public static <T> ApiResponse<T> success(final T data) {
        return new ApiResponse<>(true, data, "Operation completed successfully", null, null);
    }

    public static <T> ApiResponse<T> error(final String message) {
        return new ApiResponse<>(false, null, message, null, null);
    }

    public static <T> ApiResponse<T> error(final String message, final List<String> errors) {
        return new ApiResponse<>(false, null, message, errors, null);
    }

    public static <T> ApiResponse<T> withErrorCode(final String message, final String errorCode) {
        return new ApiResponse<>(false, null, message, null, errorCode);
    }
}
