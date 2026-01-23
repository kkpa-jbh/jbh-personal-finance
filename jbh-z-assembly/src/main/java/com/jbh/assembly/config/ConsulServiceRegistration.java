package com.jbh.assembly.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class ConsulServiceRegistration {

    private static final Logger LOG = Logger.getLogger(ConsulServiceRegistration.class);

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "consul.enabled", defaultValue = "true")
    boolean consulEnabled;

    @ConfigProperty(name = "consul.host", defaultValue = "localhost")
    String consulHost;

    @ConfigProperty(name = "consul.port", defaultValue = "8500")
    int consulPort;

    @ConfigProperty(name = "consul.service.name", defaultValue = "jbh-personal-finance")
    String serviceName;

    @ConfigProperty(name = "quarkus.http.port", defaultValue = "7777")
    int servicePort;

    @ConfigProperty(name = "consul.health-check.path", defaultValue = "/q/health")
    String healthCheckPath;

    @ConfigProperty(name = "consul.health-check.interval", defaultValue = "30s")
    String healthCheckInterval;

    @ConfigProperty(name = "consul.prefer-ip-address", defaultValue = "false")
    boolean preferIpAddress;

    @ConfigProperty(name = "consul.service.tags", defaultValue = "quarkus,jbh,personal-finance")
    List<String> serviceTags;

    private String serviceId;
    private HttpClient httpClient;

    void onStart(@Observes StartupEvent ev) {
        LOG.info("Consul registration initialization started");
        if (!consulEnabled) {
            LOG.warn("Consul registration is disabled via configuration (consul.enabled=false)");
            return;
        }

        LOG.debugf("Consul configuration: host=%s, port=%d, serviceName=%s", consulHost, consulPort, serviceName);

        httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        LOG.debug("HTTP client created for Consul communication");

        try {
            registerService();
        } catch (Exception e) {
            LOG.errorf(e, "Failed to register service with Consul at %s:%d - Service discovery will not be available",
                consulHost, consulPort);
        }
    }

    void onStop(@Observes ShutdownEvent ev) {
        LOG.info("Application shutdown detected - initiating Consul deregistration");
        if (!consulEnabled) {
            LOG.debug("Consul was disabled, skipping deregistration");
            return;
        }
        if (httpClient == null || serviceId == null) {
            LOG.warn("Consul deregistration skipped: service was not registered (httpClient or serviceId is null)");
            return;
        }

        try {
            deregisterService();
        } catch (Exception e) {
            LOG.errorf(e, "Failed to deregister service id=%s from Consul - Service may remain registered until TTL expires", serviceId);
        }
    }

    private void registerService() throws Exception {
        LOG.info("Starting service registration with Consul");

        String instanceId = UUID.randomUUID().toString().substring(0, 8);
        serviceId = serviceName + "-" + instanceId;
        LOG.debugf("Generated service ID: %s", serviceId);

        String serviceAddress = resolveServiceAddress();
        LOG.infof("Resolved service address: %s (preferIpAddress=%s)", serviceAddress, preferIpAddress);

        String healthCheckUrl = String.format("http://%s:%d%s", serviceAddress, servicePort, healthCheckPath);
        LOG.infof("Health check URL configured: %s (interval=%s)", healthCheckUrl, healthCheckInterval);

        Map<String, Object> check = Map.of(
            "HTTP", healthCheckUrl,
            "Interval", healthCheckInterval,
            "DeregisterCriticalServiceAfter", "1m"
        );

        Map<String, Object> registration = Map.of(
            "ID", serviceId,
            "Name", serviceName,
            "Address", serviceAddress,
            "Port", servicePort,
            "Tags", serviceTags,
            "Check", check
        );

        String json = objectMapper.writeValueAsString(registration);
        LOG.debugf("Registration payload: %s", json);

        String consulUrl = String.format("http://%s:%d/v1/agent/service/register", consulHost, consulPort);
        LOG.debugf("Sending registration request to Consul: %s", consulUrl);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(consulUrl))
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(json))
            .timeout(Duration.ofSeconds(10))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            LOG.infof("Service successfully registered with Consul: id=%s, name=%s, address=%s:%d, tags=%s",
                serviceId, serviceName, serviceAddress, servicePort, serviceTags);
        } else {
            LOG.errorf("Failed to register with Consul. Status: %d, Body: %s, URL: %s",
                response.statusCode(), response.body(), consulUrl);
        }
    }

    private void deregisterService() throws Exception {
        LOG.infof("Initiating service deregistration from Consul: id=%s, name=%s", serviceId, serviceName);

        String consulUrl = String.format("http://%s:%d/v1/agent/service/deregister/%s",
            consulHost, consulPort, serviceId);
        LOG.debugf("Sending deregistration request to: %s", consulUrl);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(consulUrl))
            .PUT(HttpRequest.BodyPublishers.noBody())
            .timeout(Duration.ofSeconds(10))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            LOG.infof("Service successfully deregistered from Consul: id=%s", serviceId);
        } else {
            LOG.errorf("Failed to deregister from Consul. Status: %d, Body: %s, ServiceId: %s",
                response.statusCode(), response.body(), serviceId);
        }
    }

    private String resolveServiceAddress() throws UnknownHostException {
        LOG.debug("Resolving service address for Consul registration");
        try {
            if (preferIpAddress) {
                String ipAddress = InetAddress.getLocalHost().getHostAddress();
                LOG.debugf("Resolved IP address: %s", ipAddress);
                return ipAddress;
            }
            String hostName = InetAddress.getLocalHost().getHostName();
            LOG.debugf("Resolved hostname: %s", hostName);
            return hostName;
        } catch (UnknownHostException e) {
            LOG.errorf(e, "Failed to resolve local host address - check network configuration");
            throw e;
        }
    }
}
