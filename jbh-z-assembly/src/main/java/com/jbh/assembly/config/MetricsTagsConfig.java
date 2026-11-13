package com.jbh.assembly.config;

import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.config.MeterFilter;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Adds the common tags {@code service} and {@code env} to every meter. Quarkus has no property for
 * common tags, so a {@link MeterFilter} bean is the only way.
 */
@Singleton
public class MetricsTagsConfig {

  private static final String SERVICE_TAG = "service";
  private static final String ENV_TAG = "env";

  private final String service;
  private final String env;

  public MetricsTagsConfig(
      @ConfigProperty(name = "quarkus.application.name") final String service,
      @ConfigProperty(name = "jbh.metrics.env") final String env) {
    this.service = service;
    this.env = env;
  }

  /**
   * Builds the filter that tags each meter with the service name and the environment.
   *
   * @return the common-tags filter
   */
  @Produces
  @Singleton
  public MeterFilter commonTags() {
    return MeterFilter.commonTags(Tags.of(SERVICE_TAG, service, ENV_TAG, env));
  }
}
