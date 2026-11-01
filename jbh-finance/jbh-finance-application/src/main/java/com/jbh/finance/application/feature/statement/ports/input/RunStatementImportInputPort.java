package com.jbh.finance.application.feature.statement.ports.input;

import com.jbh.finance.application.acid.UnitOfWork;
import com.jbh.finance.application.feature.movement.usecases.AddMovementsUploadedFileUseCase;
import com.jbh.finance.application.feature.statement.commands.RunStatementImportCommand;
import com.jbh.finance.application.feature.statement.dto.StatementClassificationDTO;
import com.jbh.finance.application.feature.statement.ports.output.StatementImportRepository;
import com.jbh.finance.application.feature.statement.usecases.ExtractStatementMovementsUseCase;
import com.jbh.finance.application.feature.statement.usecases.RunStatementImportUseCase;
import java.time.Clock;
import java.util.Optional;

public class RunStatementImportInputPort implements RunStatementImportUseCase {

  private final ExtractStatementMovementsUseCase extractUseCase;
  private final AddMovementsUploadedFileUseCase uploadUseCase;
  private final StatementImportRepository importRepository;
  private final UnitOfWork unitOfWork;
  private final Clock clock;

  public RunStatementImportInputPort(
      final ExtractStatementMovementsUseCase extractUseCase,
      final AddMovementsUploadedFileUseCase uploadUseCase,
      final StatementImportRepository importRepository,
      final UnitOfWork unitOfWork,
      final Clock clock) {
    this.extractUseCase = extractUseCase;
    this.uploadUseCase = uploadUseCase;
    this.importRepository = importRepository;
    this.unitOfWork = unitOfWork;
    this.clock = clock;
  }

  @Override
  public Optional<StatementClassificationDTO> run(final RunStatementImportCommand command) {
    throw new UnsupportedOperationException("not implemented");
  }
}
