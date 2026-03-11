package com.jbh.finance.infra.adapters.exceptions;

import com.jbh.commons.api.ApiResponse;
import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.InternalSystemException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.List;
import org.jboss.resteasy.reactive.RestResponse.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Global exception handler for the Finance REST API. Maps exceptions to standardized ApiResponse
 * objects with appropriate HTTP status codes.
 *
 * <p><b>NOTE - Code Duplication by Design:</b> This handler is intentionally duplicated across
 * modules (finance, notification, preferences, etc.). Each module owns its own copy because:
 * <ul>
 *   <li>In a modular monolith deployed as a single Quarkus application, all {@code @Provider}
 *       classes are scanned and registered globally. JAX-RS only allows one
 *       {@code ExceptionMapper<Exception>} to be active at a time, leading to a provider
 *       collision where an arbitrary handler wins — regardless of which module's API is being
 *       called.</li>
 *   <li>Keeping a copy per module ensures correctness if/when modules are extracted into
 *       independent microservices in the future.</li>
 * </ul>
 * Until a shared solution (e.g., a commons-level handler or specific per-type mappers) is
 * adopted, do NOT remove individual module handlers to avoid the collision problem.
 */
@Provider
public class FinanceGlobalExceptionHandler implements ExceptionMapper<Exception> {

  private static final Logger LOG = LoggerFactory.getLogger(FinanceGlobalExceptionHandler.class);

  @Override
  public Response toResponse(final Exception exception) {
    LOG.error("Exception caught by GlobalExceptionHandler", exception);

    if (exception instanceof final BusinessException accountBusinessException) {
      return handleAccountBusinessException(accountBusinessException);
    }

    if (exception instanceof final InternalSystemException internalSystemException) {
      return handleInternalSystemException(internalSystemException);
    }

    if (exception instanceof final IllegalArgumentException illegalArgumentException) {
      return handleIllegalArgumentException(illegalArgumentException);
    }

    if (exception instanceof final NotSupportedException notSupportedException) {
      return handleNotSupportedException(notSupportedException);
    }

    // Default handler for unexpected exceptions
    return handleGenericException(exception);
  }

  private Response handleAccountBusinessException(final BusinessException exception) {
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

  private Response handleInternalSystemException(final InternalSystemException exception) {
    final String errorMessage = exception.getMessage();

    final ApiResponse<Void> errorResponse =
        ApiResponse.withErrorCode(errorMessage, exception.getErrorCode());

    LOG.warn("Internal exception: {}", errorMessage);

    return Response.status(Status.UNAUTHORIZED).entity(errorResponse).build();
  }

  private Response handleIllegalArgumentException(final IllegalArgumentException exception) {
    final String errorMessage = exception.getMessage();
    final ApiResponse<Void> errorResponse = ApiResponse.error(errorMessage);

    LOG.warn("Illegal argument: {}", errorMessage);

    return Response.status(Response.Status.BAD_REQUEST) // HTTP 400
        .entity(errorResponse)
        .build();
  }

  private Response handleNotSupportedException(final NotSupportedException exception) {
    final String errorMessage =
        "Invalid Content-Type header. For file uploads, ensure Content-Type is set to"
            + " 'multipart/form-data'. Error: "
            + exception.getMessage();
    final ApiResponse<Void> errorResponse = ApiResponse.error(errorMessage);

    LOG.warn("Content-Type mismatch: {}", errorMessage);

    return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE) // HTTP 415
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
