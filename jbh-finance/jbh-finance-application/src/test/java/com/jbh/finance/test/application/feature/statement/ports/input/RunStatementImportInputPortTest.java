package com.jbh.finance.test.application.feature.statement.ports.input;

import static com.jbh.finance.test.testfixtures.builders.StatementFixtures.COP;
import static com.jbh.finance.test.testfixtures.builders.StatementFixtures.USD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.BusinessExceptionType;
import com.jbh.finance.application.feature.category.dto.CategoryDTO;
import com.jbh.finance.application.feature.movement.commands.AddMovementUploadedFileCommand;
import com.jbh.finance.application.feature.movement.commands.UploadMovementsBatchCommand;
import com.jbh.finance.application.feature.movement.dto.AddMultipleBasicMovementDTO;
import com.jbh.finance.application.feature.movement.usecases.AddMovementsUploadedFileUseCase;
import com.jbh.finance.application.feature.statement.commands.ExtractStatementMovementsCommand;
import com.jbh.finance.application.feature.statement.commands.RunStatementImportCommand;
import com.jbh.finance.application.feature.statement.dto.CategorizedMovementDTO;
import com.jbh.finance.application.feature.statement.dto.NewStatementImportDTO;
import com.jbh.finance.application.feature.statement.dto.SkippedStatementMovementDTO;
import com.jbh.finance.application.feature.statement.dto.SkippedStatementMovementDTO.SkipReason;
import com.jbh.finance.application.feature.statement.dto.StatementBalanceDTO;
import com.jbh.finance.application.feature.statement.dto.StatementClassificationDTO;
import com.jbh.finance.application.feature.statement.dto.StatementImportDTO;
import com.jbh.finance.application.feature.statement.dto.StatementImportStatus;
import com.jbh.finance.application.feature.statement.dto.StatementImportSummaryDTO;
import com.jbh.finance.application.feature.statement.dto.StatementMovementDirection;
import com.jbh.finance.application.feature.statement.dto.TotalsCheckDTO;
import com.jbh.finance.application.feature.statement.dto.TotalsCheckStatus;
import com.jbh.finance.application.feature.statement.ports.input.RunStatementImportInputPort;
import com.jbh.finance.application.feature.statement.usecases.ExtractStatementMovementsUseCase;
import com.jbh.finance.application.shared.exceptions.BusinessApplicationExceptionType;
import com.jbh.finance.domain.category.vo.CategorySourceVO;
import com.jbh.finance.domain.movement.vo.MovementReviewVO;
import com.jbh.finance.domain.movement.vo.MovementType;
import com.jbh.finance.domain.movement.vo.ProcessMovementOptionsVO;
import com.jbh.finance.domain.product.vo.ProductId;
import com.jbh.finance.domain.shared.exceptions.BusinessDomainExceptionType;
import com.jbh.finance.domain.shared.vo.EntityOperationVO;
import com.jbh.finance.test.testfixtures.builders.StatementFixtures;
import com.jbh.finance.test.testfixtures.fakes.acid.RecordingUnitOfWork;
import com.jbh.finance.test.testfixtures.fakes.statement.InMemoryStatementImportRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@Tag("unit")
class RunStatementImportInputPortTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-11-01T10:00:00Z"), ZoneOffset.UTC);
  private static final String UPLOAD_EVENT = "upload";
  private static final String READER_FAILED = "STATEMENT_READER_FAILED";
  private static final byte[] PDF_BYTES = {37, 80, 68, 70};

  private static final CategoryDTO GROCERY =
      StatementFixtures.category(CategorySourceVO.EXPENSE, "EXP_GROCERY", 20L, "Market");
  private static final CategoryDTO SALARY =
      StatementFixtures.category(CategorySourceVO.INCOME, "INC_SALARY", 10L, "Salary");
  private static final CategoryDTO UNKNOWN =
      StatementFixtures.category(CategorySourceVO.EXPENSE, "UNK", 30L, "Unknown");

  private static final CategorizedMovementDTO ROW_A =
      row("TIENDA", 3, StatementMovementDirection.DEBIT, "50.00", COP, GROCERY, "0.9", false);
  private static final CategorizedMovementDTO ROW_B =
      row("NOMINA", 1, StatementMovementDirection.CREDIT, "1000.00", COP, SALARY, "0.95", false);
  private static final CategorizedMovementDTO ROW_C =
      row("XYZ", 2, StatementMovementDirection.DEBIT, "20.00", COP, UNKNOWN, "0.4", true);
  private static final CategorizedMovementDTO ROW_D =
      row("AMAZON", 2, StatementMovementDirection.DEBIT, "5.00", USD, GROCERY, "0.9", false);
  private static final TotalsCheckDTO COP_TOTALS =
      new TotalsCheckDTO(
          COP, TotalsCheckStatus.MATCH, new BigDecimal("930.00"), new BigDecimal("930.00"));

  private final UUID userId = UUID.randomUUID();
  private final ProductId productId = ProductId.generate();
  private final UUID importId = UUID.randomUUID();

  private ExtractStatementMovementsUseCase extractUseCase;
  private AddMovementsUploadedFileUseCase uploadUseCase;
  private RecordingUnitOfWork unitOfWork;
  private InMemoryStatementImportRepository repository;
  private RunStatementImportInputPort inputPort;

  @BeforeEach
  void setUp() throws BusinessException {
    extractUseCase = mock(ExtractStatementMovementsUseCase.class);
    uploadUseCase = mock(AddMovementsUploadedFileUseCase.class);
    unitOfWork = new RecordingUnitOfWork();
    repository = new InMemoryStatementImportRepository(CLOCK, unitOfWork::isInTransaction);
    repository.insertProcessing(
        new NewStatementImportDTO(importId, userId, productId, StatementFixtures.SHA));
    when(uploadUseCase.uploadMovementsFromFile(
            eq(userId), eq(productId), any(UploadMovementsBatchCommand.class)))
        .thenAnswer(
            invocation -> {
              repository.events().add(UPLOAD_EVENT);
              return new AddMultipleBasicMovementDTO(null, List.of());
            });
    inputPort =
        new RunStatementImportInputPort(
            extractUseCase, uploadUseCase, repository, unitOfWork, CLOCK);
  }

  @Test
  void shouldUploadSavedRowsInDateOrderWhenStatementHasMixedRows() throws BusinessException {
    // Arrange
    stubExtract(classification(ROW_A, ROW_B, ROW_C, ROW_D));

    // Act
    inputPort.run(command());

    // Assert
    final List<AddMovementUploadedFileCommand> commands = uploadedBatch().movements();
    assertEquals(3, commands.size());
    assertEquals(List.of("NOMINA", "XYZ", "TIENDA"), descriptions(commands));
  }

  @Test
  void shouldMapRowsToCommandsWhenStatementHasMixedRows() throws BusinessException {
    // Arrange
    stubExtract(classification(ROW_A, ROW_B, ROW_C, ROW_D));

    // Act
    inputPort.run(command());

    // Assert: order is B, C, A
    final List<AddMovementUploadedFileCommand> commands = uploadedBatch().movements();
    final AddMovementUploadedFileCommand deposit = commands.get(0);
    assertEquals(MovementType.DEPOSIT, deposit.movementType());
    assertEquals(0, new BigDecimal("1000.00").compareTo(deposit.totalAmount()));
    assertEquals(LocalDate.of(2026, 9, 1), deposit.entryDate());
    assertSame(SALARY, deposit.category());
    assertEquals(new MovementReviewVO(new BigDecimal("0.95"), false), deposit.review());

    final AddMovementUploadedFileCommand unknownDebit = commands.get(1);
    assertEquals(MovementType.WITHDRAWAL, unknownDebit.movementType());
    assertEquals(0, new BigDecimal("-20.00").compareTo(unknownDebit.totalAmount()));
    assertSame(UNKNOWN, unknownDebit.category());
    assertEquals(new MovementReviewVO(new BigDecimal("0.4"), true), unknownDebit.review());

    final AddMovementUploadedFileCommand groceryDebit = commands.get(2);
    assertEquals(MovementType.WITHDRAWAL, groceryDebit.movementType());
    assertEquals(0, new BigDecimal("-50.00").compareTo(groceryDebit.totalAmount()));
    assertSame(GROCERY, groceryDebit.category());
    assertEquals(new MovementReviewVO(new BigDecimal("0.9"), false), groceryDebit.review());

    for (final AddMovementUploadedFileCommand command : commands) {
      assertNull(command.balanceSnapshot());
      assertEquals(importId, command.statementImportId());
    }
  }

  @Test
  void shouldUseStatementOptionsWhenUploading() throws BusinessException {
    // Arrange
    stubExtract(classification(ROW_A, ROW_B, ROW_C, ROW_D));

    // Act
    inputPort.run(command());

    // Assert
    final ProcessMovementOptionsVO options = uploadedBatch().options();
    assertTrue(options.skipFundsCheck());
    assertEquals(EntityOperationVO.ADD, options.operation());
    assertFalse(options.isMonthOfficiallyReported());
  }

  @Test
  void shouldMarkDoneWithSummaryWhenStatementHasMixedRows() throws BusinessException {
    // Arrange
    final StatementClassificationDTO classification = classification(ROW_A, ROW_B, ROW_C, ROW_D);
    stubExtract(classification);

    // Act
    inputPort.run(command());

    // Assert
    final StatementImportDTO stored = repository.get(importId);
    assertEquals(StatementImportStatus.DONE, stored.status());
    final StatementImportSummaryDTO summary = stored.summary();
    assertEquals(COP, summary.mainCurrency());
    assertEquals(3, summary.savedCount());
    assertEquals(1, summary.skippedCount());
    assertEquals(1, summary.needsReviewCount());
    assertEquals(classification.totalsChecks(), summary.totalsChecks());
    assertEquals(List.of(skipped(ROW_D, SkipReason.OTHER_CURRENCY)), summary.notSavedMovements());
  }

  @Test
  void shouldMarkDoneInsideTransactionAfterUploadWhenStatementIsSaved() throws BusinessException {
    // Arrange
    stubExtract(classification(ROW_A, ROW_B, ROW_C, ROW_D));

    // Act
    inputPort.run(command());

    // Assert
    assertEquals(
        List.of(UPLOAD_EVENT, InMemoryStatementImportRepository.MARK_DONE_EVENT),
        repository.events());
    assertTrue(repository.markDoneRanInTransaction());
    assertEquals(1, unitOfWork.transactionCount());
  }

  @Test
  void shouldReturnClassificationWhenImportSucceeds() throws BusinessException {
    // Arrange
    final StatementClassificationDTO classification = classification(ROW_A, ROW_B, ROW_C, ROW_D);
    stubExtract(classification);

    // Act
    final Optional<StatementClassificationDTO> result = inputPort.run(command());

    // Assert
    assertEquals(Optional.of(classification), result);
  }

  @Test
  void shouldSkipNothingWhenAllRowsAreInOneCurrency() throws BusinessException {
    // Arrange
    stubExtract(classification(ROW_A, ROW_B, ROW_C));

    // Act
    inputPort.run(command());

    // Assert
    final StatementImportSummaryDTO summary = repository.get(importId).summary();
    assertEquals(0, summary.skippedCount());
    assertTrue(summary.notSavedMovements().isEmpty());
  }

  @Test
  void shouldFailWithReaderCodeWhenExtractThrowsReaderFailure() throws BusinessException {
    // Arrange
    when(extractUseCase.extract(any())).thenThrow(new BusinessException(ReaderFailure.STATEMENT_READER_FAILED));

    // Act
    final Optional<StatementClassificationDTO> result = inputPort.run(command());

    // Assert
    assertTrue(result.isEmpty());
    assertFailed(READER_FAILED);
    verify(uploadUseCase, never())
        .uploadMovementsFromFile(any(), any(), any(UploadMovementsBatchCommand.class));
  }

  @Test
  void shouldFailWithNoMovementsCodeWhenExtractFindsNoMovements() throws BusinessException {
    // Arrange
    when(extractUseCase.extract(any()))
        .thenThrow(
            new BusinessException(BusinessApplicationExceptionType.STATEMENT_NO_MOVEMENTS_FOUND));

    // Act
    inputPort.run(command());

    // Assert
    assertFailed("STATEMENT_NO_MOVEMENTS_FOUND");
  }

  @Test
  void shouldFailWithDomainCodeWhenUploadThrowsBusinessException() throws BusinessException {
    // Arrange
    stubExtract(classification(ROW_A, ROW_B, ROW_C, ROW_D));
    doThrow(new BusinessException(BusinessDomainExceptionType.INSUFFICIENT_FUNDS))
        .when(uploadUseCase)
        .uploadMovementsFromFile(eq(userId), eq(productId), any(UploadMovementsBatchCommand.class));

    // Act
    final Optional<StatementClassificationDTO> result = inputPort.run(command());

    // Assert
    assertTrue(result.isEmpty());
    assertFailed("INSUFFICIENT_FUNDS");
    assertFalse(repository.events().contains(InMemoryStatementImportRepository.MARK_DONE_EVENT));
  }

  @Test
  void shouldFailWithGenericCodeWhenExtractThrowsUnexpectedException() throws BusinessException {
    // Arrange
    when(extractUseCase.extract(any())).thenThrow(new IllegalStateException("boom"));

    // Act
    final Optional<StatementClassificationDTO> result = inputPort.run(command());

    // Assert
    assertTrue(result.isEmpty());
    assertFailed("STATEMENT_IMPORT_FAILED");
  }

  @Test
  void shouldRollBackAndKeepFailedRowWhenMarkDoneFindsRowAlreadyFailed() throws BusinessException {
    // Arrange
    repository.put(failedRow());
    stubExtract(classification(ROW_A, ROW_B, ROW_C, ROW_D));

    // Act
    final Optional<StatementClassificationDTO> result = inputPort.run(command());

    // Assert
    assertTrue(result.isEmpty());
    assertEquals(1, unitOfWork.rollbackCount());
    assertEquals(StatementImportStatus.FAILED, repository.get(importId).status());
    assertEquals(
        InMemoryStatementImportRepository.TIMED_OUT_CODE, repository.get(importId).errorCode());
  }

  @Test
  void shouldSkipFutureRowAndKeepTodayWhenRowsAreDatedAfterToday() throws BusinessException {
    // Arrange
    final CategorizedMovementDTO past = rowOn(LocalDate.of(2026, 9, 1), "PAST", "1000.00", COP);
    final CategorizedMovementDTO future = rowOn(LocalDate.of(2026, 11, 2), "FUTURE", "10.00", COP);
    final CategorizedMovementDTO today = rowOn(LocalDate.of(2026, 11, 1), "TODAY", "7.00", COP);
    stubExtract(classification(past, future, today));

    // Act
    final Optional<StatementClassificationDTO> result = inputPort.run(command());

    // Assert
    assertEquals(List.of("PAST", "TODAY"), descriptions(uploadedBatch().movements()));
    final StatementImportDTO stored = repository.get(importId);
    assertEquals(StatementImportStatus.DONE, stored.status());
    assertEquals(2, stored.summary().savedCount());
    assertEquals(1, stored.summary().skippedCount());
    assertEquals(
        List.of(skipped(future, SkipReason.FUTURE_DATE)), stored.summary().notSavedMovements());
    assertTrue(result.isPresent());
  }

  @Test
  void shouldGiveOtherCurrencyReasonWhenRowIsInOtherCurrencyAndInTheFuture()
      throws BusinessException {
    // Arrange
    final CategorizedMovementDTO main = rowOn(LocalDate.of(2026, 9, 1), "MAIN", "100.00", COP);
    final CategorizedMovementDTO mainTwo = rowOn(LocalDate.of(2026, 9, 2), "MAIN2", "100.00", COP);
    final CategorizedMovementDTO foreignFuture =
        rowOn(LocalDate.of(2026, 11, 2), "FOREIGN", "9.00", USD);
    stubExtract(classification(main, mainTwo, foreignFuture));

    // Act
    inputPort.run(command());

    // Assert
    assertEquals(
        List.of(skipped(foreignFuture, SkipReason.OTHER_CURRENCY)),
        repository.get(importId).summary().notSavedMovements());
  }

  @Test
  void shouldNotUploadAndFinishDoneWhenEveryMainCurrencyRowIsInTheFuture()
      throws BusinessException {
    // Arrange
    final CategorizedMovementDTO futureOne = rowOn(LocalDate.of(2026, 11, 2), "F1", "10.00", COP);
    final CategorizedMovementDTO futureTwo = rowOn(LocalDate.of(2026, 12, 1), "F2", "20.00", COP);
    stubExtract(classification(futureOne, futureTwo));

    // Act
    final Optional<StatementClassificationDTO> result = inputPort.run(command());

    // Assert
    verify(uploadUseCase, never())
        .uploadMovementsFromFile(any(), any(), any(UploadMovementsBatchCommand.class));
    final StatementImportDTO stored = repository.get(importId);
    assertEquals(StatementImportStatus.DONE, stored.status());
    assertEquals(0, stored.summary().savedCount());
    assertEquals(2, stored.summary().skippedCount());
    assertEquals(
        List.of(
            skipped(futureOne, SkipReason.FUTURE_DATE),
            skipped(futureTwo, SkipReason.FUTURE_DATE)),
        stored.summary().notSavedMovements());
    assertTrue(result.isPresent());
  }

  private void assertFailed(final String errorCode) {
    final StatementImportDTO stored = repository.get(importId);
    assertEquals(StatementImportStatus.FAILED, stored.status());
    assertEquals(errorCode, stored.errorCode());
  }

  private UploadMovementsBatchCommand uploadedBatch() throws BusinessException {
    final ArgumentCaptor<UploadMovementsBatchCommand> captor =
        ArgumentCaptor.forClass(UploadMovementsBatchCommand.class);
    verify(uploadUseCase).uploadMovementsFromFile(eq(userId), eq(productId), captor.capture());
    return captor.getValue();
  }

  private void stubExtract(final StatementClassificationDTO classification)
      throws BusinessException {
    when(extractUseCase.extract(any())).thenReturn(classification);
  }

  private RunStatementImportCommand command() {
    return new RunStatementImportCommand(
        importId, userId, productId, new ExtractStatementMovementsCommand(PDF_BYTES.clone()));
  }

  private StatementImportDTO failedRow() {
    final StatementImportDTO current = repository.get(importId);
    return new StatementImportDTO(
        current.id(),
        current.userId(),
        current.productId(),
        current.pdfSha256(),
        StatementImportStatus.FAILED,
        InMemoryStatementImportRepository.TIMED_OUT_CODE,
        null,
        current.startedAt(),
        CLOCK.instant());
  }

  private static StatementClassificationDTO classification(final CategorizedMovementDTO... rows) {
    return new StatementClassificationDTO(
        List.of(rows), List.of(COP_TOTALS), List.of(new StatementBalanceDTO(COP, null, null)));
  }

  private static List<String> descriptions(final List<AddMovementUploadedFileCommand> commands) {
    return commands.stream().map(AddMovementUploadedFileCommand::description).toList();
  }

  private static SkippedStatementMovementDTO skipped(
      final CategorizedMovementDTO row, final SkipReason reason) {
    return new SkippedStatementMovementDTO(
        row.date(),
        row.description(),
        row.amount(),
        row.currency(),
        row.direction(),
        reason,
        row.categoryAlias(),
        row.categoryDisplayName(),
        row.confidence(),
        row.lowConfidence());
  }

  private static CategorizedMovementDTO rowOn(
      final LocalDate date, final String description, final String amount, final String currency) {
    return StatementFixtures.rowWithConfidence(
        date,
        description,
        StatementMovementDirection.DEBIT,
        amount,
        currency,
        GROCERY,
        new BigDecimal("0.9"),
        false);
  }

  private static CategorizedMovementDTO row(
      final String description,
      final int septemberDay,
      final StatementMovementDirection direction,
      final String amount,
      final String currency,
      final CategoryDTO category,
      final String confidence,
      final boolean needsReview) {
    return StatementFixtures.rowWithConfidence(
        LocalDate.of(2026, 9, septemberDay),
        description,
        direction,
        amount,
        currency,
        category,
        new BigDecimal(confidence),
        needsReview);
  }

  /** A reader failure; the real type lives in the infra module, so the test has its own. */
  private enum ReaderFailure implements BusinessExceptionType {
    STATEMENT_READER_FAILED;

    @Override
    public String getMessage() {
      return name();
    }
  }
}
