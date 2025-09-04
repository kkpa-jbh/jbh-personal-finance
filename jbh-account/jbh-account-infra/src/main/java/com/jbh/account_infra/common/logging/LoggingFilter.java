package com.jbh.account_infra.common.logging;

import com.jbh.account_app.common.logging.LoggingContext;
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
  public void filter(ContainerRequestContext requestContext) throws IOException {
    String trackingId = extractTrackingId(requestContext);
    String userId = requestContext.getHeaderString(USER_ID_HEADER);
    String requestId = extractRequestId(requestContext);

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

    logger.info("Incoming request: {} {} from IP: {}",
        requestContext.getMethod(),
        requestContext.getUriInfo().getRequestUri().getPath(),
        getClientIP(requestContext));
  }

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
    try {
      String trackingId = (String) requestContext.getProperty("trackingId");
      String requestId = (String) requestContext.getProperty("requestId");
      Long startTime = (Long) requestContext.getProperty(START_TIME_PROPERTY);

      // Add headers to response
      if (trackingId != null) {
        responseContext.getHeaders().add(TRACKING_ID_HEADER, trackingId);
      }
      if (requestId != null) {
        responseContext.getHeaders().add(REQUEST_ID_HEADER, requestId);
      }

      // Calculate execution time
      long executionTime = startTime != null ? System.currentTimeMillis() - startTime : 0;

      logger.info("Request completed: {} {} - Status: {} - Duration: {}ms",
          requestContext.getMethod(),
          requestContext.getUriInfo().getRequestUri().getPath(),
          responseContext.getStatus(),
          executionTime);

    } finally {
      // Clean up logging context
      LoggingContext.clear();
    }
  }

  private String extractTrackingId(ContainerRequestContext request) {
    String trackingId = request.getHeaderString(TRACKING_ID_HEADER);
    return trackingId != null ? trackingId : UUID.randomUUID().toString();
  }

  private String extractRequestId(ContainerRequestContext request) {
    String requestId = request.getHeaderString(REQUEST_ID_HEADER);
    return requestId != null ? requestId : UUID.randomUUID().toString();
  }

  private String getClientIP(ContainerRequestContext request) {
    String xForwardedFor = request.getHeaderString("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      return xForwardedFor.split(",")[0].trim();
    }

    String xRealIP = request.getHeaderString("X-Real-IP");
    if (xRealIP != null && !xRealIP.isEmpty()) {
      return xRealIP;
    }

    // In Quarkus, we can't easily get the remote address from ContainerRequestContext
    // This would need to be done differently or use Vert.x context
    return "unknown";
  }
}