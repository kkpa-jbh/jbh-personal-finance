package com.jbh.finance.infra.common.logging;

import static com.jbh.finance.infra.LogSanitizer.sanitize;

import com.jbh.finance.application.common.logging.LoggingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
@ApplicationScoped
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

  private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

  private static final String TRACKING_ID_HEADER = "X-Tracking-ID";
  private static final String USER_ID_HEADER = "X-User-ID";
  private static final String REQUEST_ID_HEADER = "X-Request-ID";
  private static final String START_TIME_PROPERTY = "request.startTime";

  @Override
  public void filter(final ContainerRequestContext requestContext) throws IOException {
    final String trackingId = extractTrackingId(requestContext);
    final String userId = requestContext.getHeaderString(USER_ID_HEADER);
    final String requestId = extractRequestId(requestContext);

    // Set up logging context
    LoggingContext.builder()
        .trackingId(trackingId)
        .userId(userId)
        .requestId(requestId)
        .module("account-infra")
        .apply();

    // Store for response processing
    requestContext.setProperty("trackingId", trackingId);
    requestContext.setProperty("requestId", requestId);
    requestContext.setProperty(START_TIME_PROPERTY, System.currentTimeMillis());

    logger.info(
        "Incoming request: {} {} from IP: {}",
        sanitize(requestContext.getMethod()),
        sanitize(requestContext.getUriInfo().getRequestUri().getPath()),
        sanitize(getClientIP(requestContext)));
  }

  private String extractTrackingId(final ContainerRequestContext request) {
    final String trackingId = request.getHeaderString(TRACKING_ID_HEADER);
    return trackingId != null ? trackingId : UUID.randomUUID().toString();
  }

  private String extractRequestId(final ContainerRequestContext request) {
    final String requestId = request.getHeaderString(REQUEST_ID_HEADER);
    return requestId != null ? requestId : UUID.randomUUID().toString();
  }

  private String getClientIP(final ContainerRequestContext request) {
    final String xForwardedFor = request.getHeaderString("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      return xForwardedFor.split(",")[0].trim();
    }

    final String xRealIP = request.getHeaderString("X-Real-IP");
    if (xRealIP != null && !xRealIP.isEmpty()) {
      return xRealIP;
    }

    // In Quarkus, we can't easily get the remote address from ContainerRequestContext
    // This would need to be done differently or use Vert.x context
    return "unknown";
  }

  @Override
  public void filter(
      final ContainerRequestContext requestContext, final ContainerResponseContext responseContext)
      throws IOException {
    try {
      final String trackingId = (String) requestContext.getProperty("trackingId");
      final String requestId = (String) requestContext.getProperty("requestId");
      final Long startTime = (Long) requestContext.getProperty(START_TIME_PROPERTY);

      // Add headers to response
      if (trackingId != null) {
        responseContext.getHeaders().add(TRACKING_ID_HEADER, trackingId);
      }
      if (requestId != null) {
        responseContext.getHeaders().add(REQUEST_ID_HEADER, requestId);
      }

      // Calculate execution time
      final long executionTime = startTime != null ? System.currentTimeMillis() - startTime : 0;

      logger.info(
          "Request completed: {} {} - Status: {} - Duration: {}ms",
          sanitize(requestContext.getMethod()),
          sanitize(requestContext.getUriInfo().getRequestUri().getPath()),
          sanitize(responseContext.getStatus()),
          sanitize(executionTime));

    } finally {
      // Clean up logging context
      LoggingContext.clear();
    }
  }
}
