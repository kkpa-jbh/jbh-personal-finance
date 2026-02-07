module jbh.products.domain {
  requires static lombok;
  requires org.slf4j;
  requires jbh.commons;

  // Product Aggregate exports
  exports com.jbh.products.domain.product to
      jbh.products.application,
      jbh.products.infra;
  exports com.jbh.products.domain.product.vo to
      jbh.products.application,
      jbh.products.infra;
  exports com.jbh.products.domain.product.vo.metadata to
      jbh.products.application,
      jbh.products.infra;
  exports com.jbh.products.domain.product.validation to
      jbh.products.application,
      jbh.products.infra;
  exports com.jbh.products.domain.product.service.metrics to
      jbh.products.application,
      jbh.products.infra;
  exports com.jbh.products.domain.product.service.calculators to
      jbh.products.application,
      jbh.products.infra;

  // Movement Aggregate exports
  exports com.jbh.products.domain.movement to
      jbh.products.application,
      jbh.products.infra;
  exports com.jbh.products.domain.movement.vo to
      jbh.products.application,
      jbh.products.infra;

  // MonthlyBalance Aggregate exports
  exports com.jbh.products.domain.monthlybalance to
      jbh.products.application,
      jbh.products.infra;
  exports com.jbh.products.domain.monthlybalance.service to
      jbh.products.application,
      jbh.products.infra;

  // Shared exports
  exports com.jbh.products.domain.shared.vo to
      jbh.products.application,
      jbh.products.infra;
  exports com.jbh.products.domain.shared.exceptions to
      jbh.products.application,
      jbh.products.infra;

  // Opens Product aggregate to application module for test builders using reflection
  opens com.jbh.products.domain.product to
      jbh.products.application;
  opens com.jbh.products.domain.movement to
      jbh.products.application;
  opens com.jbh.products.domain.monthlybalance to
      jbh.products.application;
}
