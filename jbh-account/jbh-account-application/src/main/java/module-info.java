module jbh.account.application {
  requires jbh.account.domain;
  requires org.slf4j;
  requires java.logging;

  exports com.jbh.account.application.acid;
  exports com.jbh.account.application.common.logging;

  // Accounts Module Exports
  exports com.jbh.account.application.accounts.usecases to
      jbh.account.infra;
  exports com.jbh.account.application.accounts.ports.input to
      jbh.account.infra,
      jbh.z.assembly;
  exports com.jbh.account.application.accounts.ports.output to
      jbh.account.infra,
      jbh.z.assembly;
  exports com.jbh.account.application.accounts.dto to
      jbh.account.infra;
  exports com.jbh.account.application.accounts.vo to
      jbh.account.infra;
  exports com.jbh.account.application.accounts.vo.commands to
      jbh.account.infra;
  exports com.jbh.account.application.accounts.services;

  // Movements Module Exports
  exports com.jbh.account.application.movements.ports.output to
      jbh.account.infra,
      jbh.z.assembly;
  exports com.jbh.account.application.async;
}
