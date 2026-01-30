module jbh.preferences.domain {
  requires static lombok;
  requires org.slf4j;
  requires jbh.commons;

  exports com.jbh.preferences.domain.entity to
      jbh.preferences.application;
  exports com.jbh.preferences.domain.exceptions to
      jbh.preferences.application,
      jbh.preferences.infra;
  exports com.jbh.preferences.domain.vo to
      jbh.preferences.application,
      jbh.preferences.infra;

  opens com.jbh.preferences.domain.entity to
      jbh.preferences.application;
}
