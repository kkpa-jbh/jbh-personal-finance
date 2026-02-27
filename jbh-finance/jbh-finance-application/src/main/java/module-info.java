module jbh.finance.application {
  requires jbh.commons;
  requires jbh.finance.domain;
  requires org.slf4j;
  requires java.logging;
  requires static lombok;

  // Test framework / Mockito access
  opens com.jbh.finance.application.feature.movement.ports.output;
  opens com.jbh.finance.application.feature.monthlybalance.ports.output;
  opens com.jbh.finance.application.feature.product.ports.output;

  // Cross-cutting infrastructure exports
  exports com.jbh.finance.application.acid;
  exports com.jbh.finance.application.async;
  exports com.jbh.finance.application.common.logging;

  // Shared business exports
  exports com.jbh.finance.application.shared.exceptions;
  exports com.jbh.finance.application.shared.validation;

  // Category feature expoerts
  exports com.jbh.finance.application.feature.category.services to
      jbh.finance.infra;

  // Product feature exports
  exports com.jbh.finance.application.feature.product.dto to
      jbh.finance.infra;
  exports com.jbh.finance.application.feature.product.ports.input to
      jbh.finance.infra,
      jbh.z.assembly;
  exports com.jbh.finance.application.feature.product.ports.output to
      jbh.finance.infra,
      jbh.z.assembly;
  exports com.jbh.finance.application.feature.product.usecases to
      jbh.finance.infra;
  exports com.jbh.finance.application.feature.product.commands to
      jbh.finance.infra;
  exports com.jbh.finance.application.feature.product.services;
  exports com.jbh.finance.application.feature.product.services.metadata;

  // Movement feature exports
  exports com.jbh.finance.application.feature.movement.dto to
      jbh.finance.infra;
  exports com.jbh.finance.application.feature.movement.ports.input to
      jbh.finance.infra,
      jbh.z.assembly;
  exports com.jbh.finance.application.feature.movement.ports.output to
      jbh.finance.infra,
      jbh.z.assembly;
  exports com.jbh.finance.application.feature.movement.usecases to
      jbh.finance.infra;
  exports com.jbh.finance.application.feature.movement.commands to
      jbh.finance.infra;
  exports com.jbh.finance.application.feature.movement.services;

  // MonthlyBalance feature exports
  exports com.jbh.finance.application.feature.monthlybalance.dto to
      jbh.finance.infra;
  exports com.jbh.finance.application.feature.monthlybalance.dto.balancehistory to
      jbh.finance.infra;
  exports com.jbh.finance.application.feature.monthlybalance.ports.input to
      jbh.finance.infra,
      jbh.z.assembly;
  exports com.jbh.finance.application.feature.monthlybalance.ports.output to
      jbh.finance.infra,
      jbh.z.assembly;
  exports com.jbh.finance.application.feature.monthlybalance.usecases to
      jbh.finance.infra;
  exports com.jbh.finance.application.feature.monthlybalance.commands to
      jbh.finance.infra;
  exports com.jbh.finance.application.feature.monthlybalance.services;
  exports com.jbh.finance.application.feature.category.dto;
  exports com.jbh.finance.application.feature.category.ports.output;
}
