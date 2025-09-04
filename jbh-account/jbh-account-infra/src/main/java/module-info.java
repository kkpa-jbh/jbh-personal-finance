module jbh.account.infra {
  requires jakarta.persistence;
  requires jakarta.transaction;
  requires jakarta.ws.rs;
  requires quarkus.core;
  requires quarkus.hibernate.orm.panache;
  requires quarkus.panache.common;

  requires jbh.account.application;
  requires org.slf4j;
}