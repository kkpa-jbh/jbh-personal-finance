package com.jbh.finance.test.application.feature.statement.ports.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.jbh.commons.exception.BusinessException;
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
import com.jbh.finance.application.feature.statement.ports.input.FindStatementImportInputPort;
import com.jbh.finance.application.shared.exceptions.BusinessApplicationExceptionType;
import com.jbh.finance.domain.category.vo.CategorySourceVO;
import com.jbh.finance.domain.movement.vo.MovementId;
import com.jbh.finance.domain.movement.vo.MovementMetadata;
import com.jbh.finance.domain.movement.vo.MovementMetadataKey;
import com.jbh.finance.domain.movement.vo.MovementType;
import com.jbh.finance.domain.product.vo.ProductId;
import com.jbh.finance.test.testfixtures.builders.StatementFixtures;
import com.jbh.finance.test.testfixtures.fakes.statement.InMemoryStatementImportRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class FindStatementImportInputPortTest {

  private static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
  private static final Duration STALE_AFTER = Duration.ofMinutes(10);
  private static final String TIMED_OUT = "STATEMENT_IMPORT_TIMED_OUT";
  private static final String READER_FAILED = "STATEMENT_READER_FAILED";
  private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 11, 1, 10, 0);

  private static final CategoryDTO SALARY =
      StatementFixtures.category(CategorySourceVO.INCOME, "INC_SALARY", 10L, "Salary");
  private static final CategoryDTO GROCERY =
      StatementFixtures.category(CategorySourceVO.EXPENSE, "EXP_GROCERY", 20L, "Market");

  private final UUID userId = UUID.randomUUID();
  private final ProductId productId = ProductId.generate();
  private final UUID importId = UUID.randomUUID();

  private ProductLifecycleService productService;
  private MovementQueryRepository movementQueryRepository;
  private InMemoryStatementImportRepository repository;
  private FindStatementImportInputPort inputPort;

  @BeforeEach
  void setUp() {
    productService = mock(ProductLifecycleService.class);
    movementQueryRepository = mock(MovementQueryRepository.class);
    repository = new InMemoryStatementImportRepository(CLOCK);
    inputPort =
        new FindStatementImportInputPort(
            productService,
            repository,
            movementQueryRepository,
            new StatementImportSettings(STALE_AFTER),
            CLOCK);
  }

  @Test
  void shouldRethrowAndNotReadImportsWhenProductIsNotFoundForUser() throws BusinessException {
    // Arrange
    final BusinessException notFound =
        new BusinessException(BusinessApplicationExceptionType.PRODUCT_NOT_FOUND);
    when(productService.findOrThrowByUserAndProductId(userId, productId)).thenThrow(notFound);

    // Act
    final BusinessException exception =
        assertThrows(BusinessException.class, () -> inputPort.find(command()));

    // Assert
    assertSame(notFound, exception);
    assertEquals(0, repository.interactionCount());
  }

  @Test
  void shouldThrowNotFoundWhenImportIdDoesNotExist() {
    // Act
    final BusinessException exception =
        assertThrows(BusinessException.class, () -> inputPort.find(command()));

    // Assert
    assertEquals(
        BusinessApplicationExceptionType.STATEMENT_IMPORT_NOT_FOUND,
        exception.getBusinessExceptionType());
  }

  @Test
  void shouldThrowNotFoundWhenImportBelongsToAnotherProduct() {
    // Arrange
    repository.put(
        row(
            ProductId.generate(),
            StatementImportStatus.PROCESSING,
            null,
            null,
            NOW.minus(Duration.ofMinutes(1))));

    // Act
    final BusinessException exception =
        assertThrows(BusinessException.class, () -> inputPort.find(command()));

    // Assert
    assertEquals(
        BusinessApplicationExceptionType.STATEMENT_IMPORT_NOT_FOUND,
        exception.getBusinessExceptionType());
  }

  @Test
  void shouldReportProcessingWithoutMovementsWhenImportStartedTwoMinutesAgo()
      throws BusinessException {
    // Arrange
    repository.put(processingStartedMinutesAgo(2));

    // Act
    final StatementImportResultDTO result = inputPort.find(command());

    // Assert
    assertEquals(StatementImportStatus.PROCESSING, result.status());
    assertTrue(result.movements().isEmpty());
    assertNull(result.summary());
    verifyNoInteractions(movementQueryRepository);
  }

  @Test
  void shouldReportFailedTimedOutWhenImportStartedElevenMinutesAgo() throws BusinessException {
    // Arrange
    repository.put(processingStartedMinutesAgo(11));

    // Act
    final StatementImportResultDTO result = inputPort.find(command());

    // Assert
    assertEquals(StatementImportStatus.FAILED, result.status());
    assertEquals(TIMED_OUT, result.errorCode());
    assertTrue(result.movements().isEmpty());
    assertEquals(StatementImportStatus.PROCESSING, repository.get(importId).status());
  }

  @Test
  void shouldStillReportProcessingWhenImportStartedExactlyTenMinutesAgo()
      throws BusinessException {
    // Arrange
    repository.put(processingStartedMinutesAgo(10));

    // Act
    final StatementImportResultDTO result = inputPort.find(command());

    // Assert
    assertEquals(StatementImportStatus.PROCESSING, result.status());
  }

  @Test
  void shouldReportStoredErrorCodeWhenImportFailed() throws BusinessException {
    // Arrange
    repository.put(
        row(productId, StatementImportStatus.FAILED, READER_FAILED, null, NOW.minusSeconds(30)));

    // Act
    final StatementImportResultDTO result = inputPort.find(command());

    // Assert
    assertEquals(StatementImportStatus.FAILED, result.status());
    assertEquals(READER_FAILED, result.errorCode());
    assertTrue(result.movements().isEmpty());
  }

  @Test
  void shouldSortMovementsByDateThenCreatedAtWhenImportIsDone() throws BusinessException {
    // Arrange
    repository.put(doneRow());
    final MovementDTO sep3 = movement("SEP3", 3, 0);
    final MovementDTO sep1 = movement("SEP1", 1, 0);
    final MovementDTO sep2Late = movement("SEP2-LATE", 2, 5);
    final MovementDTO sep2Early = movement("SEP2-EARLY", 2, 1);
    when(movementQueryRepository.findByStatementImportId(importId))
        .thenReturn(List.of(sep3, sep1, sep2Late, sep2Early));

    // Act
    final StatementImportResultDTO result = inputPort.find(command());

    // Assert
    assertEquals(
        List.of("SEP1", "SEP2-EARLY", "SEP2-LATE", "SEP3"),
        result.movements().stream().map(StatementImportMovementDTO::description).toList());
  }

  @Test
  void shouldMapDirectionAmountCategoryAndReviewWhenImportIsDone() throws BusinessException {
    // Arrange
    repository.put(doneRow());
    final MovementDTO deposit =
        movementBuilder("NOMINA", 1, MovementType.DEPOSIT, "1000.00", SALARY, null);
    final MovementDTO withdrawal =
        movementBuilder(
            "XYZ",
            2,
            MovementType.WITHDRAWAL,
            "-20.00",
            GROCERY,
            Map.of(
                MovementMetadataKey.STATEMENT_CONFIDENCE, new BigDecimal("0.4"),
                MovementMetadataKey.STATEMENT_NEEDS_REVIEW, true));
    when(movementQueryRepository.findByStatementImportId(importId))
        .thenReturn(List.of(deposit, withdrawal));

    // Act
    final List<StatementImportMovementDTO> movements = inputPort.find(command()).movements();

    // Assert
    final StatementImportMovementDTO credit = movements.get(0);
    assertEquals(StatementMovementDirection.CREDIT, credit.direction());
    assertEquals(0, new BigDecimal("1000.00").compareTo(credit.amount()));
    assertEquals("INC_SALARY", credit.categoryAlias());
    assertEquals("Salary", credit.categoryDisplayName().get("en"));
    assertFalse(credit.needsReview());
    assertNull(credit.confidence());

    final StatementImportMovementDTO debit = movements.get(1);
    assertEquals(StatementMovementDirection.DEBIT, debit.direction());
    assertEquals(0, new BigDecimal("20.00").compareTo(debit.amount()));
    assertEquals("EXP_GROCERY", debit.categoryAlias());
    assertEquals(0, new BigDecimal("0.4").compareTo(debit.confidence()));
    assertTrue(debit.needsReview());
    assertEquals(deposit.id().value(), credit.movementId());
  }

  @Test
  void shouldReadReviewDataWhenMetadataCameBackFromJsonAsDoubleAndInteger()
      throws BusinessException {
    // Arrange: JSON gives a Double for 0.4 and the Boolean true; a whole number would be Integer.
    repository.put(doneRow());
    final MovementDTO fromJson =
        movementBuilder(
            "JSON",
            1,
            MovementType.WITHDRAWAL,
            "-20.00",
            GROCERY,
            Map.of(
                MovementMetadataKey.STATEMENT_CONFIDENCE, Double.valueOf(0.4),
                MovementMetadataKey.STATEMENT_NEEDS_REVIEW, Boolean.TRUE));
    final MovementDTO wholeNumber =
        movementBuilder(
            "WHOLE",
            2,
            MovementType.WITHDRAWAL,
            "-20.00",
            GROCERY,
            Map.of(
                MovementMetadataKey.STATEMENT_CONFIDENCE, Integer.valueOf(1),
                MovementMetadataKey.STATEMENT_NEEDS_REVIEW, Boolean.FALSE));
    when(movementQueryRepository.findByStatementImportId(importId))
        .thenReturn(List.of(fromJson, wholeNumber));

    // Act
    final List<StatementImportMovementDTO> movements = inputPort.find(command()).movements();

    // Assert
    assertEquals(0, new BigDecimal("0.4").compareTo(movements.get(0).confidence()));
    assertTrue(movements.get(0).needsReview());
    assertEquals(0, BigDecimal.ONE.compareTo(movements.get(1).confidence()));
    assertFalse(movements.get(1).needsReview());
  }

  @Test
  void shouldReturnStoredSummaryWhenImportIsDone() throws BusinessException {
    // Arrange
    final StatementImportDTO done = doneRow();
    repository.put(done);
    when(movementQueryRepository.findByStatementImportId(importId)).thenReturn(List.of());

    // Act
    final StatementImportResultDTO result = inputPort.find(command());

    // Assert
    assertEquals(StatementImportStatus.DONE, result.status());
    assertEquals(done.summary(), result.summary());
    assertEquals(importId, result.importId());
    assertNull(result.errorCode());
  }

  private FindStatementImportCommand command() {
    return new FindStatementImportCommand(userId, productId, importId);
  }

  private StatementImportDTO processingStartedMinutesAgo(final long minutes) {
    return row(
        productId, StatementImportStatus.PROCESSING, null, null, NOW.minus(Duration.ofMinutes(minutes)));
  }

  private StatementImportDTO doneRow() {
    final StatementImportSummaryDTO summary =
        new StatementImportSummaryDTO("COP", 2, 0, 1, List.of(), List.of());
    return row(productId, StatementImportStatus.DONE, null, summary, NOW.minusSeconds(80));
  }

  private StatementImportDTO row(
      final ProductId rowProductId,
      final StatementImportStatus status,
      final String errorCode,
      final StatementImportSummaryDTO summary,
      final Instant startedAt) {
    final Instant finishedAt = status == StatementImportStatus.PROCESSING ? null : NOW;
    return new StatementImportDTO(
        importId,
        userId,
        rowProductId,
        StatementFixtures.SHA,
        status,
        errorCode,
        summary,
        startedAt,
        finishedAt);
  }

  private MovementDTO movement(final String description, final int day, final int createdMinute) {
    return MovementDTO.builder()
        .id(MovementId.generate())
        .productId(productId)
        .movementType(MovementType.WITHDRAWAL)
        .category(GROCERY)
        .movementAmount(new BigDecimal("-10.00"))
        .movementDate(LocalDate.of(2026, 9, day))
        .metadata(MovementMetadata.createEmpty())
        .createdAt(CREATED_AT.plusMinutes(createdMinute))
        .description(description)
        .statementImportId(importId)
        .build();
  }

  private MovementDTO movementBuilder(
      final String description,
      final int day,
      final MovementType type,
      final String amount,
      final CategoryDTO category,
      final Map<MovementMetadataKey, Object> metadata) {
    return MovementDTO.builder()
        .id(MovementId.generate())
        .productId(productId)
        .movementType(type)
        .category(category)
        .movementAmount(new BigDecimal(amount))
        .movementDate(LocalDate.of(2026, 9, day))
        .metadata(MovementMetadata.of(metadata))
        .createdAt(CREATED_AT)
        .description(description)
        .statementImportId(importId)
        .build();
  }
}
