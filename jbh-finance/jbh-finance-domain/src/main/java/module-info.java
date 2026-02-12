module jbh.finance.domain {
  requires static lombok;
  requires org.slf4j;
  requires jbh.commons;

  // Product Aggregate exports
  exports com.jbh.finance.domain.product to
      jbh.finance.application,
      jbh.finance.infra;
  exports com.jbh.finance.domain.product.vo to
      jbh.finance.application,
      jbh.finance.infra;
  exports com.jbh.finance.domain.product.vo.metadata to
      jbh.finance.application,
      jbh.finance.infra;
  exports com.jbh.finance.domain.product.validation to
      jbh.finance.application,
      jbh.finance.infra;
  exports com.jbh.finance.domain.product.service.metrics to
      jbh.finance.application,
      jbh.finance.infra;
  exports com.jbh.finance.domain.product.service.calculators to
      jbh.finance.application,
      jbh.finance.infra;

  // Movement Aggregate exports
  exports com.jbh.finance.domain.movement to
      jbh.finance.application,
      jbh.finance.infra;
  exports com.jbh.finance.domain.movement.vo to
      jbh.finance.application,
      jbh.finance.infra;

  // MonthlyBalance Aggregate exports
  exports com.jbh.finance.domain.monthlybalance to
      jbh.finance.application,
      jbh.finance.infra;
  exports com.jbh.finance.domain.monthlybalance.service to
      jbh.finance.application,
      jbh.finance.infra;

  // Shared exports
  exports com.jbh.finance.domain.shared.vo to
      jbh.finance.application,
      jbh.finance.infra;
  exports com.jbh.finance.domain.shared.exceptions to
      jbh.finance.application,
      jbh.finance.infra;

  // Opens Product aggregate to application module for test builders using reflection
  opens com.jbh.finance.domain.product to
      jbh.finance.application;
  opens com.jbh.finance.domain.movement to
      jbh.finance.application;
  opens com.jbh.finance.domain.monthlybalance to
      jbh.finance.application;
}
