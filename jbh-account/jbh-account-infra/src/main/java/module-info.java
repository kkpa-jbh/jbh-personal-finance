module jbh.account.infra {
  requires jakarta.persistence;
  requires jakarta.transaction;
  requires jakarta.ws.rs;
  requires quarkus.hibernate.orm.panache;
  requires quarkus.panache.common;

  requires jbh.account.application;
  requires org.slf4j;
  requires org.eclipse.microprofile.openapi;
  requires quarkus.core;
  requires jbh.account.domain;

  uses com.jbh.account.application.accounts.ports.input.RegisterSimpleMovementInputPort;
}