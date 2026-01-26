module jbh.account.application {
  requires jbh.commons;
  requires jbh.account.domain;
  requires org.slf4j;
  requires java.logging;
  requires static lombok;

  exports com.jbh.account.application.acid;
  exports com.jbh.account.application.common.logging;

  // Accounts Module Exports
  exports com.jbh.account.application.core.usecases to
      jbh.account.infra;
  exports com.jbh.account.application.core.ports.input to
      jbh.account.infra,
      jbh.z.assembly;
  exports com.jbh.account.application.core.ports.output to
      jbh.account.infra,
      jbh.z.assembly;
  exports com.jbh.account.application.core.dto to
      jbh.account.infra;
  exports com.jbh.account.application.core.vo.commands to
      jbh.account.infra;
  exports com.jbh.account.application.core.services;

  // Movements Module Exports
  exports com.jbh.account.application.movements.ports.output to
      jbh.account.infra,
      jbh.z.assembly;
  exports com.jbh.account.application.async;
  exports com.jbh.account.application.core.ports.output.monthlybalance to
      jbh.account.infra,
      jbh.z.assembly;
  exports com.jbh.account.application.core.services.monthlybalance;
  exports com.jbh.account.application.core.services.account;
  exports com.jbh.account.application.core.services.movements;
  exports com.jbh.account.application.core.services.metadata;
}
