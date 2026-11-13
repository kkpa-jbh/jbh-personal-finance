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
  void shouldDisableOtelSdkByDefaultWhenNoEnvVar() {
    assertEquals("${OTEL_SDK_DISABLED:true}", PROPS.getProperty("quarkus.otel.sdk.disabled"));
  }

  @Test
  void shouldSampleAllByDefaultWhenNoSamplerEnvVar() {
    assertEquals(
        "${OTEL_TRACES_SAMPLER_ARG:1.0}", PROPS.getProperty("quarkus.otel.traces.sampler.arg"));
  }

  @Test
  void shouldReadEndpointFromEnvWhenConfigured() {
    final String endpoint = PROPS.getProperty("%prod.quarkus.otel.exporter.otlp.endpoint");

    assertNotNull(endpoint);
    assertTrue(endpoint.startsWith("${OTEL_EXPORTER_OTLP_ENDPOINT:"));
  }

  @Test
  void shouldLeaveBaseEndpointUnset() {
    assertNull(PROPS.getProperty("quarkus.otel.exporter.otlp.endpoint"));
  }

  @Test
  void shouldNotConfigureLgtmDevService() {
    assertNull(PROPS.getProperty("quarkus.observability.enabled"));
  }

  @Test
  void shouldExportByDefaultInDevToLocalStack() {
    assertEquals("${OTEL_SDK_DISABLED:false}", PROPS.getProperty("%dev.quarkus.otel.sdk.disabled"));
    assertEquals(
        "http://localhost:4317", PROPS.getProperty("%dev.quarkus.otel.exporter.otlp.endpoint"));
  }

  @Test
  void shouldEnableDatasourceMetrics() {
    assertEquals("true", PROPS.getProperty("quarkus.datasource.metrics.enabled"));
  }

  @Test
  void shouldSetEnvPerProfileWhenTagging() {
    assertEquals("prod", PROPS.getProperty(ENV_KEY));
    assertEquals("dev", PROPS.getProperty("%dev." + ENV_KEY));
    assertEquals("test", PROPS.getProperty("%test." + ENV_KEY));
  }

  @Test
  void shouldPrintTraceAndSpanIdsInDevConsole() {
    final String format = PROPS.getProperty("%dev.quarkus.log.console.format");

    assertNotNull(format);
    assertTrue(format.contains("traceId=%X{traceId}"));
    assertTrue(format.contains("spanId=%X{spanId}"));
  }

  @Test
  void shouldExportLogsToOtelOnlyInDev() {
    assertEquals("true", PROPS.getProperty("%dev.quarkus.otel.logs.enabled"));
    assertNull(PROPS.getProperty("quarkus.otel.logs.enabled"));
    assertNull(PROPS.getProperty("%prod.quarkus.otel.logs.enabled"));
  }
}
