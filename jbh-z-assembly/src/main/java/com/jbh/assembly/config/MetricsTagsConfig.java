package com.jbh.assembly.config;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import java.time.Duration;
import java.util.stream.LongStream;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Micrometer meter filters for this app: the common tags {@code service} and {@code env} on every
 * meter, and the latency buckets of the HTTP server timer. Quarkus has no property for either, so
 * {@link MeterFilter} beans are the only way.
 */
@Singleton
public class MetricsTagsConfig {

  private static final String SERVICE_TAG = "service";
  private static final String ENV_TAG = "env";
  private static final String HTTP_SERVER_TIMER = "http.server.requests";
  private static final long[] LATENCY_BUCKET_MILLIS = {
    5, 10, 25, 50, 100, 250, 500, 1000, 2500, 5000, 10_000
  };

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

  /**
   * Builds the filter that gives {@code http.server.requests} fixed latency buckets (5 ms to 10 s).
   * Other meters keep their configuration.
   *
   * @return the HTTP latency buckets filter
   */
  @Produces
  @Singleton
  public MeterFilter httpServerLatency() {
    final double[] bucketNanos =
        LongStream.of(LATENCY_BUCKET_MILLIS)
            .mapToDouble(millis -> Duration.ofMillis(millis).toNanos())
            .toArray();
    return new MeterFilter() {
      @Override
      public DistributionStatisticConfig configure(
          final Meter.Id id, final DistributionStatisticConfig config) {
        if (!HTTP_SERVER_TIMER.equals(id.getName())) {
          return config;
        }
        return DistributionStatisticConfig.builder()
            .serviceLevelObjectives(bucketNanos)
            .build()
            .merge(config);
      }
    };
  }
}
