package com.jbh.assembly.config;

import io.micrometer.core.instrument.config.MeterFilter;

/** Adds the common tags {@code service} and {@code env} to every meter. */
public class MetricsTagsConfig {

  public MetricsTagsConfig(final String service, final String env) {
    // Skeleton: the values are stored when commonTags() is implemented.
  }

  public MeterFilter commonTags() {
    throw new UnsupportedOperationException("not implemented");
  }
}
