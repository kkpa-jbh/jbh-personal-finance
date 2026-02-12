package com.jbh.preferences.infra.adapters.exceptions;

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

@Provider
public class PreferencesGlobalExceptionHandler implements ExceptionMapper<Exception> {

  private static final Logger LOG =
      LoggerFactory.getLogger(PreferencesGlobalExceptionHandler.class);

  @Override
  public Response toResponse(final Exception exception) {
    LOG.error("Exception caught by PreferencesGlobalExceptionHandler", exception);

    if (exception instanceof final BusinessException businessException) {
      return handleBusinessException(businessException);
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

    return handleGenericException(exception);
  }

  private Response handleBusinessException(final BusinessException exception) {
    final String errorMessage = exception.getMessage();

    final ApiResponse<Void> errorResponse =
        ApiResponse.error(
            errorMessage,
            exception.getBusinessExceptionType() != null
                ? List.of(exception.getBusinessExceptionType().getMessage())
                : null);

    LOG.warn("Business exception: {}", errorMessage);

    return Response.status(422).entity(errorResponse).build();
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

    return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
  }

  private Response handleNotSupportedException(final NotSupportedException exception) {
    final String errorMessage =
        "Invalid Content-Type header. For file uploads, ensure Content-Type is set to"
            + " 'multipart/form-data'. Error: "
            + exception.getMessage();
    final ApiResponse<Void> errorResponse = ApiResponse.error(errorMessage);

    LOG.warn("Content-Type mismatch: {}", errorMessage);

    return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).entity(errorResponse).build();
  }

  private Response handleGenericException(final Exception exception) {
    final String errorMessage = "An unexpected error occurred. Please try again later.";
    final ApiResponse<Void> errorResponse = ApiResponse.error(errorMessage);

    LOG.error("Unexpected exception occurred", exception);

    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorResponse).build();
  }
}
