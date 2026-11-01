package com.jbh.finance.application.feature.statement.ports.input;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.BusinessExceptionType;
import com.jbh.finance.application.acid.UnitOfWork;
import com.jbh.finance.application.common.logging.LoggerFactory;
import com.jbh.finance.application.feature.movement.commands.UploadMovementsBatchCommand;
import com.jbh.finance.application.feature.movement.usecases.AddMovementsUploadedFileUseCase;
import com.jbh.finance.application.feature.statement.commands.RunStatementImportCommand;
import com.jbh.finance.application.feature.statement.dto.StatementClassificationDTO;
import com.jbh.finance.application.feature.statement.dto.StatementImportSummaryDTO;
import com.jbh.finance.application.feature.statement.ports.output.StatementImportRepository;
import com.jbh.finance.application.feature.statement.usecases.ExtractStatementMovementsUseCase;
import com.jbh.finance.application.feature.statement.usecases.RunStatementImportUseCase;
import com.jbh.finance.application.shared.exceptions.BusinessApplicationExceptionType;
import java.time.Clock;
import java.util.Optional;
import org.slf4j.Logger;

/**
 * Runs one admitted statement import in the background. The long AI work happens before the
 * transaction, so no database connection is held during it. It never throws: every failure ends in
 * a {@code FAILED} row.
 */
public class RunStatementImportInputPort implements RunStatementImportUseCase {
  private static final Logger LOG = LoggerFactory.getLogger(RunStatementImportInputPort.class);

  private static final String LOG_PREFIX = "[StatementImport]";
  private static final String GENERIC_ERROR_CODE = "STATEMENT_IMPORT_FAILED";

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
  @SuppressWarnings("PMD.AvoidCatchingGenericException")
  public Optional<StatementClassificationDTO> run(final RunStatementImportCommand command) {
    try {
      final StatementClassificationDTO classification =
          extractUseCase.extract(command.extractCommand());
      final StatementImportPlan plan = StatementImportPlan.of(classification, clock);
      final StatementImportSummaryDTO summary = plan.summary();

      unitOfWork.executeInTransaction(() -> saveAndFinish(command, plan, summary));
      return Optional.of(classification);
    } catch (final BusinessException e) {
      return fail(command, errorCodeOf(e.getBusinessExceptionType()));
    } catch (final RuntimeException e) {
      // The background job must end in a FAILED row, whatever went wrong.
      LOG.error(LOG_PREFIX + " Unexpected failure", e);
      return fail(command, GENERIC_ERROR_CODE);
    }
  }

  /**
   * One transaction: the save takes the product lock first, then the import is marked {@code DONE}.
   * If the row is no longer {@code PROCESSING} (freed as stale), everything rolls back.
   */
  private StatementImportSummaryDTO saveAndFinish(
      final RunStatementImportCommand command,
      final StatementImportPlan plan,
      final StatementImportSummaryDTO summary)
      throws BusinessException {
    if (!plan.toSave().isEmpty()) {
      uploadUseCase.uploadMovementsFromFile(
          command.userId(),
          command.productId(),
          UploadMovementsBatchCommand.forStatement(plan.commands(command.importId())));
    }
    if (!importRepository.markDone(command.importId(), summary)) {
      throw new BusinessException(BusinessApplicationExceptionType.STATEMENT_IMPORT_TIMED_OUT);
    }
    return summary;
  }

  private Optional<StatementClassificationDTO> fail(
      final RunStatementImportCommand command, final String errorCode) {
    LOG.warn(LOG_PREFIX + " Import failed with {}", errorCode);
    importRepository.markFailed(command.importId(), errorCode);
    return Optional.empty();
  }

  private static String errorCodeOf(final BusinessExceptionType type) {
    return type instanceof Enum<?> named ? named.name() : GENERIC_ERROR_CODE;
  }
}
