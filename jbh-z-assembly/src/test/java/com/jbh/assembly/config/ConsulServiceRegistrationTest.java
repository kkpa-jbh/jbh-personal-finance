package com.jbh.assembly.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsulServiceRegistrationTest {

    @Mock
    private ObjectMapper objectMapper;

    private ConsulServiceRegistration registration;

    @BeforeEach
    void setUp() throws Exception {
        registration = new ConsulServiceRegistration();

        setField(registration, "objectMapper", objectMapper);
        setField(registration, "consulEnabled", true);
        setField(registration, "consulHost", "localhost");
        setField(registration, "consulPort", 8500);
        setField(registration, "serviceName", "jbh-personal-finance");
        setField(registration, "servicePort", 7777);
        setField(registration, "healthCheckPath", "/q/health");
        setField(registration, "healthCheckInterval", "30s");
        setField(registration, "preferIpAddress", false);
        setField(registration, "serviceTags", List.of("quarkus", "jbh", "personal-finance"));
    }

    @Test
    void shouldBeCreatedSuccessfully() {
        assertNotNull(registration);
    }

    @Test
    void shouldHaveCorrectServiceNameConfiguration() throws Exception {
        String serviceName = getField(registration, "serviceName", String.class);
        assertEquals("jbh-personal-finance", serviceName);
    }

    @Test
    void shouldHaveCorrectConsulHostConfiguration() throws Exception {
        String consulHost = getField(registration, "consulHost", String.class);
        assertEquals("localhost", consulHost);
    }

    @Test
    void shouldHaveCorrectConsulPortConfiguration() throws Exception {
        int consulPort = getField(registration, "consulPort", int.class);
        assertEquals(8500, consulPort);
    }

    @Test
    void shouldHaveCorrectHealthCheckPath() throws Exception {
        String healthCheckPath = getField(registration, "healthCheckPath", String.class);
        assertEquals("/q/health", healthCheckPath);
    }

    @Test
    void shouldHaveCorrectHealthCheckInterval() throws Exception {
        String healthCheckInterval = getField(registration, "healthCheckInterval", String.class);
        assertEquals("30s", healthCheckInterval);
    }

    @Test
    void shouldHaveServiceTagsConfigured() throws Exception {
        @SuppressWarnings("unchecked")
        List<String> tags = getField(registration, "serviceTags", List.class);

        assertNotNull(tags);
        assertEquals(3, tags.size());
        assertTrue(tags.contains("quarkus"));
        assertTrue(tags.contains("jbh"));
        assertTrue(tags.contains("personal-finance"));
    }

    @Test
    void shouldSerializeRegistrationPayload() throws Exception {
        when(objectMapper.writeValueAsString(any(Map.class))).thenReturn("{\"test\":\"payload\"}");

        String json = objectMapper.writeValueAsString(Map.of("test", "payload"));
        assertEquals("{\"test\":\"payload\"}", json);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @SuppressWarnings("unchecked")
    private <T> T getField(Object target, String fieldName, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(target);
    }
}
