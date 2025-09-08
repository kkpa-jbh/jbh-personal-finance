module jbh.z.assembly {
  requires jakarta.cdi;
  requires jakarta.inject;
  requires org.jboss.logging;
  requires quarkus.core;

  requires jbh.account.application;
  requires jbh.account.domain;
}