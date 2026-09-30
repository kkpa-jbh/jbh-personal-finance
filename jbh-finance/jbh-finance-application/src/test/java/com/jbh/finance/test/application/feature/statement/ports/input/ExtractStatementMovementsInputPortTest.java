package com.jbh.finance.test.application.feature.statement.ports.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.finance.application.feature.category.dto.CategoryDTO;
import com.jbh.finance.application.feature.category.ports.output.CategoryQueryRepo;
import com.jbh.finance.application.feature.statement.commands.ExtractStatementMovementsCommand;
import com.jbh.finance.application.feature.statement.dto.CategorizedMovementDTO;
import com.jbh.finance.application.feature.statement.dto.ClassificationAnswerDTO;
import com.jbh.finance.application.feature.statement.dto.ClassificationQuestionDTO;
import com.jbh.finance.application.feature.statement.dto.ClassificationSettings;
import com.jbh.finance.application.feature.statement.dto.ExtractedMovementDTO;
import com.jbh.finance.application.feature.statement.dto.ExtractedStatementDTO;
import com.jbh.finance.application.feature.statement.dto.StatementClassificationDTO;
import com.jbh.finance.application.feature.statement.dto.StatementMovementDirection;
import com.jbh.finance.application.feature.statement.dto.TotalsCheckStatus;
import com.jbh.finance.application.feature.statement.ports.input.ExtractStatementMovementsInputPort;
import com.jbh.finance.application.feature.statement.ports.output.MovementClassifier;
import com.jbh.finance.application.feature.statement.ports.output.StatementReader;
import com.jbh.finance.application.shared.exceptions.BusinessApplicationExceptionType;
import com.jbh.finance.domain.category.vo.CategorySourceVO;
import com.jbh.finance.domain.category.vo.CategoryTypeVO;
import com.jbh.finance.test.testfixtures.CategoryServiceMock;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@Tag("unit")
class ExtractStatementMovementsInputPortTest {

  private static final String UNKNOWN = "UNK";
  private static final String GROCERY = "EXP_GROCERY";
  private static final String SALARY = "INC_SALARY";
  private static final byte[] PDF_BYTES = {37, 80, 68, 70};
  private static final LocalDate DATE = LocalDate.of(2026, 9, 1);

  @Mock private StatementReader statementReader;

  @Mock private MovementClassifier movementClassifier;

  @Mock private CategoryQueryRepo categoryQueryRepo;

  private ExtractStatementMovementsInputPort inputPort;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    // CategoryServiceMock returns the "UNK" system category.
    inputPort = inputPortWithBatchSize(20);
  }

  @Test
  void shouldThrowWhenCommandIsNull() {
    // Act & Assert
    assertThrows(GenericSpecificationException.class, () -> inputPort.extract(null));
    verifyNoInteractions(statementReader, movementClassifier, categoryQueryRepo);
  }

  @Test
  void shouldThrowWhenStatementHasNoMovements() throws BusinessException {
    // Arrange
    when(statementReader.read(any())).thenReturn(new ExtractedStatementDTO(List.of(), null, null));

    // Act
    final BusinessException exception =
        assertThrows(BusinessException.class, () -> inputPort.extract(command()));

    // Assert
    assertEquals(
        BusinessApplicationExceptionType.STATEMENT_NO_MOVEMENTS_FOUND,
        exception.getBusinessExceptionType());
    verify(movementClassifier, never()).classify(anyList());
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldSplitQuestionsInBatchesWhenBatchSizeIsSmall() throws BusinessException {
    // Arrange
    inputPort = inputPortWithBatchSize(2);
    stubCategories();
    stubStatement(debits(5), null, null);
    when(movementClassifier.classify(anyList())).thenReturn(List.of());
    final ArgumentCaptor<List<ClassificationQuestionDTO>> captor =
        ArgumentCaptor.forClass(List.class);

    // Act
    inputPort.extract(command());

    // Assert
    verify(movementClassifier, times(3)).classify(captor.capture());
    assertEquals(List.of(2, 2, 1), captor.getAllValues().stream().map(List::size).toList());
  }

  @Test
  void shouldNotMarkLowConfidenceWhenConfidenceIsAboveMinimum() throws BusinessException {
    // Arrange
    stubCategories();
    stubStatement(debits(1), null, null);
    stubAnswers(new ClassificationAnswerDTO("m_0", GROCERY, new BigDecimal("0.9")));

    // Act
    final CategorizedMovementDTO movement = inputPort.extract(command()).movements().get(0);

    // Assert
    assertEquals(GROCERY, movement.categoryAlias());
    assertEquals("Market", movement.categoryDisplayName().get("en"));
    assertFalse(movement.lowConfidence());
  }

  @Test
  void shouldMarkLowConfidenceWhenConfidenceIsBelowMinimum() throws BusinessException {
    // Arrange
    stubCategories();
    stubStatement(debits(1), null, null);
    stubAnswers(new ClassificationAnswerDTO("m_0", GROCERY, new BigDecimal("0.5")));

    // Act
    final CategorizedMovementDTO movement = inputPort.extract(command()).movements().get(0);

    // Assert
    assertEquals(GROCERY, movement.categoryAlias());
    assertTrue(movement.lowConfidence());
  }

  @Test
  void shouldMarkLowConfidenceWhenAnswerIsUnknown() throws BusinessException {
    // Arrange
    stubCategories();
    stubStatement(debits(1), null, null);
    stubAnswers(new ClassificationAnswerDTO("m_0", UNKNOWN, new BigDecimal("0.95")));

    // Act
    final CategorizedMovementDTO movement = inputPort.extract(command()).movements().get(0);

    // Assert
    assertEquals(UNKNOWN, movement.categoryAlias());
    assertTrue(movement.lowConfidence());
  }

  @Test
  void shouldFallBackToUnknownWhenAnswerIsMissingOrFromWrongSource() throws BusinessException {
    // Arrange: m_0 has no answer; m_1 (a debit) got an income category.
    stubCategories();
    stubStatement(debits(2), null, null);
    stubAnswers(new ClassificationAnswerDTO("m_1", SALARY, new BigDecimal("0.99")));

    // Act
    final List<CategorizedMovementDTO> movements = inputPort.extract(command()).movements();

    // Assert
    for (final CategorizedMovementDTO movement : movements) {
      assertEquals(UNKNOWN, movement.categoryAlias());
      assertEquals(0, BigDecimal.ZERO.compareTo(movement.confidence()));
      assertTrue(movement.lowConfidence());
    }
  }

  @Test
  void shouldKeepPdfOrderAndTotalsWhenClassified() throws BusinessException {
    // Arrange
    stubCategories();
    final List<ExtractedMovementDTO> movements =
        List.of(
            movement("NOMINA EMPRESA", "500", StatementMovementDirection.CREDIT),
            movement("COMPRA EXITO", "200", StatementMovementDirection.DEBIT));
    stubStatement(movements, new BigDecimal("1000"), new BigDecimal("1300"));
    stubAnswers(
        new ClassificationAnswerDTO("m_1", GROCERY, new BigDecimal("0.8")),
        new ClassificationAnswerDTO("m_0", SALARY, new BigDecimal("0.9")));

    // Act
    final StatementClassificationDTO result = inputPort.extract(command());

    // Assert
    assertEquals("NOMINA EMPRESA", result.movements().get(0).description());
    assertEquals(SALARY, result.movements().get(0).categoryAlias());
    assertEquals("COMPRA EXITO", result.movements().get(1).description());
    assertEquals(GROCERY, result.movements().get(1).categoryAlias());
    assertEquals(TotalsCheckStatus.MATCH, result.totalsCheck().status());
  }

  @Test
  void shouldPropagateExceptionWhenClassifierFails() throws BusinessException {
    // Arrange
    stubCategories();
    stubStatement(debits(1), null, null);
    final BusinessException failure =
        new BusinessException(BusinessApplicationExceptionType.STATEMENT_NO_MOVEMENTS_FOUND);
    when(movementClassifier.classify(anyList())).thenThrow(failure);

    // Act
    final BusinessException exception =
        assertThrows(BusinessException.class, () -> inputPort.extract(command()));

    // Assert
    assertSame(failure, exception);
  }

  private ExtractStatementMovementsInputPort inputPortWithBatchSize(final int batchSize) {
    return new ExtractStatementMovementsInputPort(
        statementReader,
        movementClassifier,
        categoryQueryRepo,
        new CategoryServiceMock(),
        new ClassificationSettings(batchSize, new BigDecimal("0.7")));
  }

  private void stubCategories() {
    when(categoryQueryRepo.findActiveBySource(CategorySourceVO.INCOME))
        .thenReturn(List.of(category(CategorySourceVO.INCOME, SALARY, "Salary")));
    when(categoryQueryRepo.findActiveBySource(CategorySourceVO.EXPENSE))
        .thenReturn(List.of(category(CategorySourceVO.EXPENSE, GROCERY, "Market")));
  }

  private void stubStatement(
      final List<ExtractedMovementDTO> movements,
      final BigDecimal opening,
      final BigDecimal closing)
      throws BusinessException {
    when(statementReader.read(any()))
        .thenReturn(new ExtractedStatementDTO(movements, opening, closing));
  }

  private void stubAnswers(final ClassificationAnswerDTO... answers) throws BusinessException {
    when(movementClassifier.classify(anyList())).thenReturn(List.of(answers));
  }

  private static ExtractStatementMovementsCommand command() {
    return new ExtractStatementMovementsCommand(PDF_BYTES.clone());
  }

  private static List<ExtractedMovementDTO> debits(final int count) {
    final List<ExtractedMovementDTO> movements = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      movements.add(movement("COMPRA " + i, "10", StatementMovementDirection.DEBIT));
    }
    return movements;
  }

  private static ExtractedMovementDTO movement(
      final String description, final String amount, final StatementMovementDirection direction) {
    return new ExtractedMovementDTO(DATE, description, new BigDecimal(amount), direction);
  }

  private static CategoryDTO category(
      final CategorySourceVO source, final String alias, final String name) {
    return CategoryDTO.withEntity(
        new CategoryTypeVO(source, alias),
        1L,
        Map.of("en", name, "es", name),
        true,
        Map.of("en", name + " description", "es", name));
  }
}
