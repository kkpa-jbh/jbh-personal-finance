package com.jbh.assembly.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Checks that the export switch wins over the OpenTelemetry env vars. System properties stand in
 * for env vars: the SDK ranks both above the Quarkus defaults, so they show the same bug.
 */
class OtelExportGateCustomizerTest {

  private static final String EXPORT_OFF = "true";
  private static final String EXPORT_ON = "false";
  private static final String INVALID_SWITCH = "1";
  private static final String SAMPLER_ON = "always_on";
  private static final String SPAN_NAME = "request";

  /** What Quarkus passes as SDK defaults, without any exporter so the test sends nothing. */
  private static final Map<String, String> QUARKUS_DEFAULTS =
      Map.of(
          OtelExportGateCustomizer.SDK_DISABLED_KEY, OtelExportGateCustomizer.SDK_ON,
          OtelExportGateCustomizer.SAMPLER_KEY, OtelExportGateCustomizer.SAMPLER_OFF,
          "otel.traces.exporter", "none",
          "otel.metrics.exporter", "none",
          "otel.logs.exporter", "none");

  @AfterEach
  void clearEnvStandIns() {
    System.clearProperty(OtelExportGateCustomizer.SDK_DISABLED_KEY);
    System.clearProperty(OtelExportGateCustomizer.SAMPLER_KEY);
  }

  @Test
  void shouldKeepSdkOnAndSamplerOffWhenExportIsOff() {
    assertEquals(
        Map.of(
            OtelExportGateCustomizer.SDK_DISABLED_KEY, OtelExportGateCustomizer.SDK_ON,
            OtelExportGateCustomizer.SAMPLER_KEY, OtelExportGateCustomizer.SAMPLER_OFF),
        OtelExportGateCustomizer.overrides(EXPORT_OFF));
  }

  @Test
  void shouldKeepOnlySdkOnWhenExportIsOn() {
    assertEquals(
        Map.of(OtelExportGateCustomizer.SDK_DISABLED_KEY, OtelExportGateCustomizer.SDK_ON),
        OtelExportGateCustomizer.overrides(EXPORT_ON));
  }

  @Test
  void shouldTreatExportAsOffWhenSwitchIsInvalid() {
    assertEquals(
        OtelExportGateCustomizer.SAMPLER_OFF,
        OtelExportGateCustomizer.overrides(INVALID_SWITCH).get(OtelExportGateCustomizer.SAMPLER_KEY));
  }

  @Test
  void shouldTreatExportAsOffWhenSwitchIsMissing() {
    assertEquals(
        OtelExportGateCustomizer.SAMPLER_OFF,
        OtelExportGateCustomizer.overrides(null).get(OtelExportGateCustomizer.SAMPLER_KEY));
  }

  @Test
  void shouldKeepValidTraceIdWhenSdkDisabledEnvIsTrue() {
    System.setProperty(OtelExportGateCustomizer.SDK_DISABLED_KEY, EXPORT_OFF);

    final SpanContext span = startSpan(EXPORT_OFF);

    assertTrue(span.isValid());
    assertFalse(span.isSampled());
  }

  @Test
  void shouldNotSampleWhenExportIsOffAndSamplerEnvIsAlwaysOn() {
    System.setProperty(OtelExportGateCustomizer.SAMPLER_KEY, SAMPLER_ON);

    final SpanContext span = startSpan(EXPORT_OFF);

    assertTrue(span.isValid());
    assertFalse(span.isSampled());
  }

  @Test
  void shouldKeepSamplerEnvWhenExportIsOn() {
    System.setProperty(OtelExportGateCustomizer.SDK_DISABLED_KEY, EXPORT_ON);
    System.setProperty(OtelExportGateCustomizer.SAMPLER_KEY, SAMPLER_ON);

    final SpanContext span = startSpan(EXPORT_ON);

    assertTrue(span.isSampled());
  }

  private static SpanContext startSpan(final String exportOff) {
    final AutoConfiguredOpenTelemetrySdkBuilder builder =
        AutoConfiguredOpenTelemetrySdk.builder()
            .disableShutdownHook()
            .addPropertiesSupplier(() -> QUARKUS_DEFAULTS);
    new OtelExportGateCustomizer(exportOff).customize(builder);
    try (OpenTelemetrySdk sdk = builder.build().getOpenTelemetrySdk()) {
      return sdk.getTracer(SPAN_NAME).spanBuilder(SPAN_NAME).startSpan().getSpanContext();
    }
  }
}
