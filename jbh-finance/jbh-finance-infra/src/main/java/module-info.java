/** Keep organize it by package. */
module jbh.finance.infra {
  requires jakarta.persistence;
  requires jakarta.transaction;
  requires jakarta.ws.rs;
  requires quarkus.hibernate.orm.panache;
  requires quarkus.panache.common;
  requires jbh.finance.application;
  requires org.slf4j;
  requires org.eclipse.microprofile.openapi;
  requires quarkus.core;
  requires jbh.finance.domain;
  requires io.hypersistence.utils.hibernate.type;
  requires org.hibernate.orm.core;
  requires static lombok;
  requires org.apache.poi.ooxml;
  requires resteasy.reactive.common;
  requires jakarta.resource;
  requires smallrye.config.core;
  requires com.jbh.gateway;
  requires org.apache.commons.lang3;
  requires com.opencsv;
  requires org.apache.commons.collections4;
  requires jbh.commons;
  requires jakarta.cdi;
}
