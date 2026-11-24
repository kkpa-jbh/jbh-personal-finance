package com.jbh.assembly.health;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.List;
import java.util.Map;

/**
 * Runs the app like the Kubernetes cluster does (JBH-86): Consul registration off
 * (the cluster sets CONSUL_ENABLED=false) and a throwaway PostgreSQL.
 */
public class KubernetesProbesTestProfile implements QuarkusTestProfile {

  @Override
  public Map<String, String> getConfigOverrides() {
    return Map.of("consul.enabled", "false");
  }

  @Override
  public List<TestResourceEntry> testResources() {
    return List.of(new TestResourceEntry(AssemblyPostgresResource.class));
  }
}
