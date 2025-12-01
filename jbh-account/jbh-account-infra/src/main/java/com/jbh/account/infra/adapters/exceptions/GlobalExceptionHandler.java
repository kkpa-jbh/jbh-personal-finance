package com.jbh.account.infra.adapters.exceptions;

import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.infra.adapters.in.rest.vo.ApiResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Global exception handler for the Account REST API. Maps exceptions to standardized ApiResponse
 * objects with appropriate HTTP status codes.
 */
@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Exception> {

  private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @Override
  public Response toResponse(final Exception exception) {
    LOG.error("Exception caught by GlobalExceptionHandler", exception);

    if (exception instanceof final AccountBusinessException accountBusinessException) {
      return handleAccountBusinessException(accountBusinessException);
    }

    if (exception instanceof final IllegalArgumentException illegalArgumentException) {
      return handleIllegalArgumentException(illegalArgumentException);
    }

    // Default handler for unexpected exceptions
    return handleGenericException(exception);
  }

  private Response handleAccountBusinessException(final AccountBusinessException exception) {
    final String errorMessage = exception.getMessage();

    final ApiResponse<Void> errorResponse =
        ApiResponse.error(
            errorMessage,
            exception.getBusinessExceptionType() != null
                ? List.of(exception.getBusinessExceptionType().getMessage())
                : null);

    LOG.warn("Business exception: {}", errorMessage);

    return Response.status(422) // HTTP 422 - Unprocessable Entity
        .entity(errorResponse)
        .build();
  }

  private Response handleIllegalArgumentException(final IllegalArgumentException exception) {
    final String errorMessage = exception.getMessage();
    final ApiResponse<Void> errorResponse = ApiResponse.error(errorMessage);

    LOG.warn("Illegal argument: {}", errorMessage);

    return Response.status(Response.Status.BAD_REQUEST) // HTTP 400
        .entity(errorResponse)
        .build();
  }

  private Response handleGenericException(final Exception exception) {
    final String errorMessage = "An unexpected error occurred. Please try again later.";
    final ApiResponse<Void> errorResponse = ApiResponse.error(errorMessage);

    LOG.error("Unexpected exception occurred", exception);

    return Response.status(Response.Status.INTERNAL_SERVER_ERROR) // HTTP 500
        .entity(errorResponse)
        .build();
  }
}
