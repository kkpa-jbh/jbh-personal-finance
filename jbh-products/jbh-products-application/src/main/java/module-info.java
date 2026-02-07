module jbh.products.application {
  requires jbh.commons;
  requires jbh.products.domain;
  requires org.slf4j;
  requires java.logging;
  requires static lombok;

  // Cross-cutting infrastructure exports
  exports com.jbh.products.application.acid;
  exports com.jbh.products.application.async;
  exports com.jbh.products.application.common.logging;

  // Shared business exports
  exports com.jbh.products.application.shared.exceptions;
  exports com.jbh.products.application.shared.validation;

  // Product feature exports
  exports com.jbh.products.application.feature.product.dto to
      jbh.products.infra;
  exports com.jbh.products.application.feature.product.ports.input to
      jbh.products.infra,
      jbh.z.assembly;
  exports com.jbh.products.application.feature.product.ports.output to
      jbh.products.infra,
      jbh.z.assembly;
  exports com.jbh.products.application.feature.product.usecases to
      jbh.products.infra;
  exports com.jbh.products.application.feature.product.commands to
      jbh.products.infra;
  exports com.jbh.products.application.feature.product.services;
  exports com.jbh.products.application.feature.product.services.metadata;

  // Movement feature exports
  exports com.jbh.products.application.feature.movement.dto to
      jbh.products.infra;
  exports com.jbh.products.application.feature.movement.ports.input to
      jbh.products.infra,
      jbh.z.assembly;
  exports com.jbh.products.application.feature.movement.ports.output to
      jbh.products.infra,
      jbh.z.assembly;
  exports com.jbh.products.application.feature.movement.usecases to
      jbh.products.infra;
  exports com.jbh.products.application.feature.movement.commands to
      jbh.products.infra;
  exports com.jbh.products.application.feature.movement.services;

  // MonthlyBalance feature exports
  exports com.jbh.products.application.feature.monthlybalance.dto to
      jbh.products.infra;
  exports com.jbh.products.application.feature.monthlybalance.dto.balancehistory to
      jbh.products.infra;
  exports com.jbh.products.application.feature.monthlybalance.ports.input to
      jbh.products.infra,
      jbh.z.assembly;
  exports com.jbh.products.application.feature.monthlybalance.ports.output to
      jbh.products.infra,
      jbh.z.assembly;
  exports com.jbh.products.application.feature.monthlybalance.usecases to
      jbh.products.infra;
  exports com.jbh.products.application.feature.monthlybalance.commands to
      jbh.products.infra;
  exports com.jbh.products.application.feature.monthlybalance.services;
}
