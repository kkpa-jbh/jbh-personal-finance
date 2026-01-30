module jbh.preferences.infra {
  requires jakarta.persistence;
  requires jakarta.transaction;
  requires jakarta.ws.rs;
  requires quarkus.hibernate.orm.panache;
  requires quarkus.panache.common;
  requires jbh.preferences.application;
  requires org.slf4j;
  requires org.eclipse.microprofile.openapi;
  requires quarkus.core;
  requires jbh.preferences.domain;
  requires io.hypersistence.utils.hibernate.type;
  requires org.hibernate.orm.core;
  requires static lombok;
  requires resteasy.reactive.common;
  requires smallrye.config.core;
  requires com.jbh.gateway;
  requires jbh.commons;

  uses com.jbh.preferences.application.core.ports.input.GetUserPreferencesInputPort;
}
