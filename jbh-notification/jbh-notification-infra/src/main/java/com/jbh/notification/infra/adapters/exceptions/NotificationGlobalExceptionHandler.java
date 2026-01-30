package com.jbh.notification.infra.adapters.exceptions;

import com.jbh.commons.api.ApiResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Global exception handler for the Notification REST API.
 * Maps exceptions to standardized ApiResponse objects with appropriate HTTP status codes.
 */
@Provider
public class NotificationGlobalExceptionHandler implements ExceptionMapper<Exception> {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationGlobalExceptionHandler.class);
    private static final String GENERIC_ERROR_MESSAGE = "An unexpected error occurred. Please try again later.";

    @Override
    public Response toResponse(final Exception exception) {
        LOG.error("Exception caught: {} - {}", exception.getClass().getSimpleName(), exception.getMessage());
        LOG.debug("Full stack trace:", exception);

        return handleGenericException(exception);
    }

    private Response handleGenericException(final Exception exception) {
        final ApiResponse<Void> errorResponse = ApiResponse.error(GENERIC_ERROR_MESSAGE);

        LOG.error("Unexpected exception occurred: {}", exception.getMessage(), exception);

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorResponse)
                .build();
    }
}
