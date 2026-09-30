package com.jbh.finance.infra;

import com.jbh.finance.application.feature.category.ports.output.CategoryQueryRepo;
import com.jbh.finance.application.feature.category.services.CategoryService;
import com.jbh.finance.application.feature.statement.dto.ClassificationSettings;
import com.jbh.finance.application.feature.statement.ports.input.ExtractStatementMovementsInputPort;
import com.jbh.finance.application.feature.statement.ports.output.MovementClassifier;
import com.jbh.finance.application.feature.statement.ports.output.StatementReader;
import com.jbh.finance.application.feature.statement.usecases.ExtractStatementMovementsUseCase;
import com.jbh.finance.infra.config.StatementImportConfig;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

/** CDI wiring of the bank statement import. Kept apart so ProductUseCasesCDIConfig does not grow. */
@ApplicationScoped
@RegisterForReflection(targets = {ExtractStatementMovementsInputPort.class})
public class StatementUseCasesCDIConfig {

  @Inject CategoryQueryRepo categoryQueryRepo;

  @Inject CategoryService categoryService;

  @Inject StatementReader statementReader;

  @Inject MovementClassifier movementClassifier;

  @Inject StatementImportConfig config;

  @Produces
  @ApplicationScoped
  public ExtractStatementMovementsUseCase extractStatementMovementsUseCase() {
    return new ExtractStatementMovementsInputPort(
        statementReader,
        movementClassifier,
        categoryQueryRepo,
        categoryService,
        new ClassificationSettings(config.jev().batchSize(), config.jev().minConfidence()));
  }
}
