module jbh.z.assembly {
  requires org.jboss.logging;
  requires jakarta.cdi;
  requires jakarta.inject;
  requires quarkus.core;
  requires microprofile.config.api;
  requires java.net.http;
  requires com.fasterxml.jackson.databind;
  requires resteasy.reactive.common;
  requires jakarta.ws.rs;

  opens com.jbh.assembly to
      quarkus.core;
  opens com.jbh.assembly.config to
      quarkus.core,
      com.fasterxml.jackson.databind;
}
