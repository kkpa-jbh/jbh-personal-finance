package com.jbh.assembly.config;

import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import io.quarkus.opentelemetry.runtime.AutoConfiguredOpenTelemetrySdkBuilderCustomizer;
import jakarta.inject.Singleton;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Applies the export switch ({@code jbh.otel.export-off}) after the OpenTelemetry SDK has read its
 * env vars. Quarkus passes {@code application.properties} to the SDK only as defaults, so {@code
 * OTEL_SDK_DISABLED} and {@code OTEL_TRACES_SAMPLER} from the env would win over them. A properties
 * customizer runs last, so its values win.
 *
 * <p>The SDK stays on in every case, so each request keeps a valid {@code traceId}. When the export
 * is off, the sampler is {@code always_off}, so no span is sent. When the export is on, the sampler
 * from the config or the env is kept.
 */
@Singleton
public class OtelExportGateCustomizer implements AutoConfiguredOpenTelemetrySdkBuilderCustomizer {

  static final String SDK_DISABLED_KEY = "otel.sdk.disabled";
  static final String SAMPLER_KEY = "otel.traces.sampler";
  static final String SDK_ON = "false";
  static final String SAMPLER_OFF = "always_off";

  /** The only value of {@code jbh.otel.export-off} that turns the export on. */
  private static final String EXPORT_ON_VALUE = "false";

  private final String exportOff;

  /**
   * Creates the customizer.
   *
   * @param exportOff the raw value of {@code jbh.otel.export-off}; only {@code false} turns the
   *     export on
   */
  public OtelExportGateCustomizer(
      @ConfigProperty(name = "jbh.otel.export-off") final String exportOff) {
    this.exportOff = exportOff;
  }

  @Override
  public void customize(final AutoConfiguredOpenTelemetrySdkBuilder builder) {
    final Map<String, String> overrides = overrides(exportOff);
    builder.addPropertiesCustomizer(config -> overrides);
  }

  /**
   * Builds the SDK properties that must win over the env vars.
   *
   * @param exportOff the raw value of {@code jbh.otel.export-off}; any value other than {@code
   *     false} (case-sensitive) means export off, the same rule as {@code application.properties}
   * @return the SDK property overrides
   */
  static Map<String, String> overrides(final String exportOff) {
    if (EXPORT_ON_VALUE.equals(exportOff)) {
      return Map.of(SDK_DISABLED_KEY, SDK_ON);
    }
    return Map.of(SDK_DISABLED_KEY, SDK_ON, SAMPLER_KEY, SAMPLER_OFF);
  }
}
