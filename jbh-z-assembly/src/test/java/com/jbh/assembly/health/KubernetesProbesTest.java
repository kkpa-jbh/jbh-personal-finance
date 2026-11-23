package com.jbh.assembly.health;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Guard test for the Kubernetes probes (JBH-86).
 *
 * <p>The kubelet calls these paths with no token: startupProbe /q/health/started, livenessProbe
 * /q/health/live, readinessProbe /q/health/ready. If one stops answering UP with Consul off, the pod
 * never becomes Ready in the cluster.
 *
 * <p>Tagged "database": it starts PostgreSQL in Docker, so it runs only with
 * -Djbh.test.excludedGroups=none (ci.yml passes it).
 */
@Tag("database")
@QuarkusTest
@TestProfile(KubernetesProbesTestProfile.class)
class KubernetesProbesTest {

  private static final String UP = "UP";
  private static final int OK = 200;

  @Test
  void startedProbeAnswersUp() {
    given().when().get("/q/health/started").then().statusCode(OK).body("status", equalTo(UP));
  }

  @Test
  void livenessProbeAnswersUp() {
    given().when().get("/q/health/live").then().statusCode(OK).body("status", equalTo(UP));
  }

  @Test
  void readinessProbeAnswersUp() {
    given().when().get("/q/health/ready").then().statusCode(OK).body("status", equalTo(UP));
  }
}
