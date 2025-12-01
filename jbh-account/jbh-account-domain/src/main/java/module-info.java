module jbh.account.domain {
  requires static lombok;
  requires org.slf4j;

  exports com.jbh.account.domain.utils to
      jbh.account.application,
      jbh.account.infra;
  exports com.jbh.account.domain.entity to
      jbh.account.application;
  exports com.jbh.account.domain.exceptions to
      jbh.account.application,
      jbh.account.infra;
  exports com.jbh.account.domain.vo to
      jbh.account.application,
      jbh.account.infra;
  exports com.jbh.account.domain.vo.metadata to
      jbh.account.application,
      jbh.account.infra;

  // Opens entity package to application module for test builders using reflection
  opens com.jbh.account.domain.entity to
      jbh.account.application;
}
