module jbh.products.domain {
  requires static lombok;
  requires org.slf4j;
  requires jbh.commons;

  exports com.jbh.products.domain.entity to
      jbh.products.application;
  exports com.jbh.products.domain.exceptions to
      jbh.products.application,
      jbh.products.infra;
  exports com.jbh.products.domain.vo to
      jbh.products.application,
      jbh.products.infra;
  exports com.jbh.products.domain.vo.metadata to
      jbh.products.application,
      jbh.products.infra;

  // Opens entity package to application module for test builders using reflection
  opens com.jbh.products.domain.entity to
      jbh.products.application;
}
