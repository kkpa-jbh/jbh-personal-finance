package com.jbh.finance.application.feature.statement.ports.input;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.finance.application.feature.category.dto.CategoryDTO;
import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.application.feature.movement.ports.output.MovementQueryRepository;
import com.jbh.finance.application.feature.product.services.ProductLifecycleService;
import com.jbh.finance.application.feature.statement.commands.FindStatementImportCommand;
import com.jbh.finance.application.feature.statement.dto.StatementImportDTO;
import com.jbh.finance.application.feature.statement.dto.StatementImportMovementDTO;
import com.jbh.finance.application.feature.statement.dto.StatementImportResultDTO;
import com.jbh.finance.application.feature.statement.dto.StatementImportSettings;
import com.jbh.finance.application.feature.statement.dto.StatementImportStatus;
import com.jbh.finance.application.feature.statement.dto.StatementImportSummaryDTO;
import com.jbh.finance.application.feature.statement.dto.StatementMovementDirection;
import com.jbh.finance.application.feature.statement.ports.output.StatementImportRepository;
import com.jbh.finance.application.feature.statement.usecases.FindStatementImportUseCase;
import com.jbh.finance.application.shared.exceptions.BusinessApplicationExceptionType;
import com.jbh.finance.domain.movement.vo.MovementMetadataKey;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class FindStatementImportInputPort implements FindStatementImportUseCase {

  private static final String TIMED_OUT_CODE =
      BusinessApplicationExceptionType.STATEMENT_IMPORT_TIMED_OUT.name();
  private static final Comparator<MovementDTO> MOVEMENT_ORDER =
      Comparator.comparing(MovementDTO::movementDate)
          .thenComparing(MovementDTO::createdAt, Comparator.nullsFirst(Comparator.naturalOrder()));

  private final ProductLifecycleService productService;
  private final StatementImportRepository importRepository;
  private final MovementQueryRepository movementQueryRepository;
  private final StatementImportSettings settings;
  private final Clock clock;

  public FindStatementImportInputPort(
      final ProductLifecycleService productService,
      final StatementImportRepository importRepository,
      final MovementQueryRepository movementQueryRepository,
      final StatementImportSettings settings,
      final Clock clock) {
    this.productService = productService;
    this.importRepository = importRepository;
    this.movementQueryRepository = movementQueryRepository;
    this.settings = settings;
    this.clock = clock;
  }

  @Override
  public StatementImportResultDTO find(final FindStatementImportCommand command)
      throws BusinessException {
    if (command == null) {
      throw new GenericSpecificationException("Command cannot be null");
    }
    productService.findOrThrowByUserAndProductId(command.userId(), command.productId());

    final StatementImportDTO stored =
        importRepository
            .findByIdAndProduct(command.importId(), command.productId())
            .orElseThrow(
                () ->
                    new BusinessException(
                        BusinessApplicationExceptionType.STATEMENT_IMPORT_NOT_FOUND));

    if (isStale(stored)) {
      return resultOf(stored, StatementImportStatus.FAILED, TIMED_OUT_CODE);
    }
    return resultOf(stored, stored.status(), stored.errorCode());
  }

  /** Stale only when older than the limit; the stored row is never changed by a poll. */
  private boolean isStale(final StatementImportDTO stored) {
    final Instant limit = clock.instant().minus(settings.staleAfter());
    return stored.status() == StatementImportStatus.PROCESSING
        && stored.startedAt().isBefore(limit);
  }

  private StatementImportResultDTO resultOf(
      final StatementImportDTO stored, final StatementImportStatus status, final String errorCode) {
    final boolean done = status == StatementImportStatus.DONE;
    final List<StatementImportMovementDTO> movements = done ? savedMovements(stored) : List.of();
    final StatementImportSummaryDTO summary = done ? stored.summary() : null;
    final Instant finishedAt =
        status == stored.status() ? stored.finishedAt() : clock.instant();
    return new StatementImportResultDTO(
        stored.id(), status, errorCode, stored.startedAt(), finishedAt, summary, movements);
  }

  private List<StatementImportMovementDTO> savedMovements(final StatementImportDTO stored) {
    return movementQueryRepository.findByStatementImportId(stored.id()).stream()
        .sorted(MOVEMENT_ORDER)
        .map(FindStatementImportInputPort::toImportMovement)
        .toList();
  }

  private static StatementImportMovementDTO toImportMovement(final MovementDTO movement) {
    final CategoryDTO category = movement.category();
    final Map<String, String> displayName =
        category.getDisplayName() == null ? Map.of() : category.getDisplayName();
    final StatementMovementDirection direction =
        movement.movementType().isWithdrawal()
            ? StatementMovementDirection.DEBIT
            : StatementMovementDirection.CREDIT;
    return new StatementImportMovementDTO(
        movement.id().value(),
        movement.movementDate(),
        movement.description(),
        movement.movementAmount().abs(),
        direction,
        category.getAlias(),
        displayName,
        confidenceOf(movement),
        Boolean.TRUE.equals(metadataOf(movement, MovementMetadataKey.STATEMENT_NEEDS_REVIEW)));
  }

  /** JSON gives back a Double or an Integer, so the number is read through its text. */
  private static BigDecimal confidenceOf(final MovementDTO movement) {
    final Object value = metadataOf(movement, MovementMetadataKey.STATEMENT_CONFIDENCE);
    return value == null ? null : new BigDecimal(value.toString());
  }

  private static Object metadataOf(final MovementDTO movement, final MovementMetadataKey key) {
    return movement.metadata() == null ? null : movement.metadata().get(key);
  }
}
