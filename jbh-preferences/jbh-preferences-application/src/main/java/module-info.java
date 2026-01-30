module jbh.preferences.application {
  requires jbh.commons;
  requires jbh.preferences.domain;
  requires org.slf4j;
  requires static lombok;

  exports com.jbh.preferences.application.core.ports.input to
      jbh.preferences.infra,
      jbh.z.assembly;
  exports com.jbh.preferences.application.core.ports.output to
      jbh.preferences.infra,
      jbh.z.assembly;
  exports com.jbh.preferences.application.core.dto to
      jbh.preferences.infra;
  exports com.jbh.preferences.application.core.vo.commands to
      jbh.preferences.infra;
  exports com.jbh.preferences.application.core.services;
}
