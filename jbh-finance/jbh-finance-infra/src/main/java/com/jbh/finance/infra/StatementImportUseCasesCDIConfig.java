package com.jbh.finance.infra;

import com.jbh.finance.application.acid.UnitOfWork;
import com.jbh.finance.application.feature.movement.ports.output.MovementQueryRepository;
import com.jbh.finance.application.feature.movement.usecases.AddMovementsUploadedFileUseCase;
import com.jbh.finance.application.feature.product.services.ProductLifecycleService;
import com.jbh.finance.application.feature.statement.dto.StatementImportSettings;
import com.jbh.finance.application.feature.statement.ports.input.FindStatementImportInputPort;
import com.jbh.finance.application.feature.statement.ports.input.RunStatementImportInputPort;
import com.jbh.finance.application.feature.statement.ports.input.StartStatementImportInputPort;
import com.jbh.finance.application.feature.statement.ports.output.StatementImportRepository;
import com.jbh.finance.application.feature.statement.usecases.ExtractStatementMovementsUseCase;
import com.jbh.finance.application.feature.statement.usecases.FindStatementImportUseCase;
import com.jbh.finance.application.feature.statement.usecases.RunStatementImportUseCase;
import com.jbh.finance.application.feature.statement.usecases.StartStatementImportUseCase;
import com.jbh.finance.infra.config.StatementImportConfig;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import java.time.Clock;

/** CDI wiring of the three statement import use cases (start, run, find). */
@ApplicationScoped
@RegisterForReflection(
    targets = {
      StartStatementImportInputPort.class,
      RunStatementImportInputPort.class,
      FindStatementImportInputPort.class
    })
public class StatementImportUseCasesCDIConfig {

  @Inject ProductLifecycleService productService;

  @Inject StatementImportRepository importRepository;

  @Inject MovementQueryRepository movementQueryRepository;

  @Inject UnitOfWork unitOfWork;

  @Inject AddMovementsUploadedFileUseCase uploadUseCase;

  @Inject ExtractStatementMovementsUseCase extractUseCase;

  @Inject StatementImportConfig config;

  @Produces
  @ApplicationScoped
  public StartStatementImportUseCase startStatementImportUseCase() {
    return new StartStatementImportInputPort(productService, importRepository, settings());
  }

  @Produces
  @ApplicationScoped
  public RunStatementImportUseCase runStatementImportUseCase() {
    return new RunStatementImportInputPort(
        extractUseCase, uploadUseCase, importRepository, unitOfWork, clock());
  }

  @Produces
  @ApplicationScoped
  public FindStatementImportUseCase findStatementImportUseCase() {
    return new FindStatementImportInputPort(
        productService, importRepository, movementQueryRepository, settings(), clock());
  }

  private StatementImportSettings settings() {
    return new StatementImportSettings(config.staleAfter());
  }

  private static Clock clock() {
    return Clock.systemDefaultZone();
  }
}
