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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
@ApplicationScoped
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

  private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

  private static final String USER_ID_HEADER = "X-User-ID";
  private static final String START_TIME_PROPERTY = "request.startTime";

  @Override
  public void filter(final ContainerRequestContext requestContext) throws IOException {
    final String userId = requestContext.getHeaderString(USER_ID_HEADER);

    // Set up logging context
    LoggingContext.builder().userId(userId).module("productresponse-infra").apply();

    // Store for response processing
    requestContext.setProperty(START_TIME_PROPERTY, System.currentTimeMillis());

    logger.info(
        "Incoming request: {} {}",
        sanitize(requestContext.getMethod()),
        sanitize(requestContext.getUriInfo().getRequestUri().getPath()));
  }

  @Override
  public void filter(
      final ContainerRequestContext requestContext, final ContainerResponseContext responseContext)
      throws IOException {
    try {
      final Long startTime = (Long) requestContext.getProperty(START_TIME_PROPERTY);

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
