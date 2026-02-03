module jbh.products.application {
  requires jbh.commons;
  requires jbh.products.domain;
  requires org.slf4j;
  requires java.logging;
  requires static lombok;

  exports com.jbh.products.application.acid;
  exports com.jbh.products.application.common.logging;

  // Accounts Module Exports
  exports com.jbh.products.application.core.usecases to
      jbh.products.infra;
  exports com.jbh.products.application.core.ports.input to
      jbh.products.infra,
      jbh.z.assembly;
  exports com.jbh.products.application.core.ports.output to
      jbh.products.infra,
      jbh.z.assembly;
  exports com.jbh.products.application.core.dto to
      jbh.products.infra;
  exports com.jbh.products.application.core.dto.balancehistory to
      jbh.products.infra;
  exports com.jbh.products.application.core.vo.commands to
      jbh.products.infra;
  exports com.jbh.products.application.core.services;

  // Movements Module Exports
  exports com.jbh.products.application.movements.ports.output to
      jbh.products.infra,
      jbh.z.assembly;
  exports com.jbh.products.application.async;
  exports com.jbh.products.application.core.ports.output.monthlybalance to
      jbh.products.infra,
      jbh.z.assembly;
  exports com.jbh.products.application.core.services.monthlybalance;
  exports com.jbh.products.application.core.services.account;
  exports com.jbh.products.application.core.services.movements;
  exports com.jbh.products.application.core.services.metadata;
}
