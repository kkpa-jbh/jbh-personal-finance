package com.jbh.assembly.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class MetricsTagsConfigTest {

  private static final String SERVICE = "jbh-personal-finance";
  private static final String METER_NAME = "jbh.test";
  private static final String SERVICE_TAG = "service";
  private static final String ENV_TAG = "env";

  private static MeterRegistry registryWithFilter(final String env) {
    final MeterRegistry registry = new SimpleMeterRegistry();
    registry.config().meterFilter(new MetricsTagsConfig(SERVICE, env).commonTags());
    return registry;
  }

  @Test
  void shouldAddServiceAndEnvTagsWhenMeterIsCreated() {
    final MeterRegistry registry = registryWithFilter("prod");

    final Counter counter = registry.counter(METER_NAME);

    assertEquals(SERVICE, counter.getId().getTag(SERVICE_TAG));
    assertEquals("prod", counter.getId().getTag(ENV_TAG));
  }

  @Test
  void shouldUseGivenEnvWhenEnvIsDev() {
    final MeterRegistry registry = registryWithFilter("dev");

    final Counter counter = registry.counter(METER_NAME);

    assertEquals("dev", counter.getId().getTag(ENV_TAG));
  }

  @Test
  void shouldAddOnlyTwoTagsWhenMeterHasNoOwnTags() {
    final MeterRegistry registry = registryWithFilter("prod");

    final Counter counter = registry.counter(METER_NAME);

    assertEquals(2, counter.getId().getTags().size());
  }

  @Test
  void shouldKeepOwnTagsWhenMeterHasThem() {
    final MeterRegistry registry = registryWithFilter("prod");

    final Counter counter = registry.counter(METER_NAME, "result", "ok");

    assertEquals(3, counter.getId().getTags().size());
    assertEquals("ok", counter.getId().getTag("result"));
    assertEquals(SERVICE, counter.getId().getTag(SERVICE_TAG));
    assertEquals("prod", counter.getId().getTag(ENV_TAG));
  }
}
