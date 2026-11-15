package com.jbh.assembly.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Guards the observability keys of this module's application.properties. The file is read by
 * relative path because other modules also ship an application.properties.
 */
class ObservabilityConfigPropertiesTest {

  private static final Path PROPERTIES_FILE = Path.of("src/main/resources/application.properties");
  private static final String LOG_LEVEL_KEY = "quarkus.log.category.\"com.jbh.**\".level";
  private static final String JSON_ENABLED_KEY = "quarkus.log.console.json.enabled";
  private static final String ENV_KEY = "jbh.metrics.env";

  private static final Properties PROPS = new Properties();

  @BeforeAll
  static void loadProperties() throws IOException {
    try (Reader reader = Files.newBufferedReader(PROPERTIES_FILE, StandardCharsets.UTF_8)) {
      PROPS.load(reader);
    }
  }

  @Test
  void shouldLogInfoInProdAndDebugInDevWhenCategoryIsComJbh() {
    assertEquals("INFO", PROPS.getProperty("%prod." + LOG_LEVEL_KEY));
    assertEquals("DEBUG", PROPS.getProperty("%dev." + LOG_LEVEL_KEY));
  }

  @Test
  void shouldHaveNoProfilelessLevelWhenCategoryIsComJbh() {
    assertFalse(PROPS.containsKey(LOG_LEVEL_KEY));
  }

  @Test
  void shouldEnableJsonOnlyInProdWhenLoggingConsole() {
    assertEquals("false", PROPS.getProperty(JSON_ENABLED_KEY));
    assertEquals("true", PROPS.getProperty("%prod." + JSON_ENABLED_KEY));
  }

  @Test
  void shouldWriteMdcAsTopLevelFieldsWhenJsonIsOn() {
    assertEquals("true", PROPS.getProperty("quarkus.log.console.json.mdc.flat-fields"));
  }

  @Test
  void shouldKeepSdkOnAndExportOffByDefaultWhenNoEnvVar() {
    assertEquals("false", PROPS.getProperty("quarkus.otel.sdk.disabled"));
    assertEquals("${OTEL_SDK_DISABLED:true}", PROPS.getProperty("jbh.otel.export-off"));
  }

  @Test
  void shouldSampleAllByDefaultWhenNoSamplerEnvVar() {
    assertEquals(
        "${OTEL_TRACES_SAMPLER_ARG:1.0}", PROPS.getProperty("quarkus.otel.traces.sampler.arg"));
    assertEquals(
        "${jbh.otel.sampler.${jbh.otel.export-off}:always_off}",
        PROPS.getProperty("quarkus.otel.traces.sampler"));
    assertEquals("always_off", PROPS.getProperty("jbh.otel.sampler.true"));
    assertEquals(
        "${OTEL_TRACES_SAMPLER:parentbased_traceidratio}",
        PROPS.getProperty("jbh.otel.sampler.false"));
  }

  @Test
  void shouldReadEndpointFromEnvWhenConfigured() {
    assertEquals(
        "${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:14317}",
        PROPS.getProperty("quarkus.otel.exporter.otlp.endpoint"));
  }

  @Test
  void shouldHaveOneEndpointLineForAllProfiles() {
    assertNull(PROPS.getProperty("%prod.quarkus.otel.exporter.otlp.endpoint"));
    assertNull(PROPS.getProperty("%dev.quarkus.otel.exporter.otlp.endpoint"));
  }

  @Test
  void shouldNotConfigureLgtmDevService() {
    assertNull(PROPS.getProperty("quarkus.observability.enabled"));
  }

  @Test
  void shouldExportByDefaultInDevToLocalStack() {
    assertNull(PROPS.getProperty("%dev.quarkus.otel.sdk.disabled"));
    assertEquals("${OTEL_SDK_DISABLED:false}", PROPS.getProperty("%dev.jbh.otel.export-off"));
  }

  @Test
  void shouldEnableDatasourceMetrics() {
    assertEquals("true", PROPS.getProperty("quarkus.datasource.metrics.enabled"));
  }

  @Test
  void shouldSetEnvPerProfileWhenTagging() {
    assertEquals("${JBH_ENV:prod}", PROPS.getProperty(ENV_KEY));
    assertEquals("dev", PROPS.getProperty("%dev." + ENV_KEY));
    assertEquals("test", PROPS.getProperty("%test." + ENV_KEY));
    assertEquals(
        "service.namespace=jbh,deployment.environment.name=${jbh.metrics.env},"
            + "service.version=${JBH_SERVICE_VERSION:unknown}",
        PROPS.getProperty("quarkus.otel.resource.attributes"));
  }

  @Test
  void shouldPrintTraceAndSpanIdsInDevConsole() {
    final String format = PROPS.getProperty("%dev.quarkus.log.console.format");

    assertNotNull(format);
    assertTrue(format.contains("traceId=%X{traceId}"));
    assertTrue(format.contains("spanId=%X{spanId}"));
  }

  @Test
  void shouldBuildOtelLogsInEveryProfileAndSwitchByExportFlag() {
    assertEquals("true", PROPS.getProperty("quarkus.otel.logs.enabled"));
    assertNull(PROPS.getProperty("%dev.quarkus.otel.logs.enabled"));
    assertEquals("false", PROPS.getProperty("jbh.otel.logs-handler.true"));
    assertEquals("true", PROPS.getProperty("jbh.otel.logs-handler.false"));
    assertEquals(
        "${jbh.otel.logs-handler.${jbh.otel.export-off}:false}",
        PROPS.getProperty("quarkus.otel.logs.handler.enabled"));
  }
}
