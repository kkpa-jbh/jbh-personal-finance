package com.jbh.finance.test.application.feature.statement.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.jbh.finance.application.feature.category.dto.CategoryDTO;
import com.jbh.finance.application.feature.category.ports.output.CategoryQueryRepo;
import com.jbh.finance.application.feature.statement.dto.CategoryOptionDTO;
import com.jbh.finance.application.feature.statement.dto.ClassificationQuestionDTO;
import com.jbh.finance.application.feature.statement.dto.ExtractedMovementDTO;
import com.jbh.finance.application.feature.statement.dto.StatementMovementDirection;
import com.jbh.finance.application.feature.statement.services.StatementCategoryCatalog;
import com.jbh.finance.domain.category.vo.CategorySourceVO;
import com.jbh.finance.domain.category.vo.CategoryTypeVO;
import com.jbh.finance.test.testfixtures.CategoryServiceMock;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@Tag("unit")
class StatementCategoryCatalogTest {

  private static final String UNKNOWN = "UNK";

  @Mock private CategoryQueryRepo categoryQueryRepo;

  private StatementCategoryCatalog catalog;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    when(categoryQueryRepo.findActiveBySource(CategorySourceVO.INCOME))
        .thenReturn(
            List.of(category(CategorySourceVO.INCOME, "INC_SALARY", "Salary", "Monthly pay")));
    when(categoryQueryRepo.findActiveBySource(CategorySourceVO.EXPENSE))
        .thenReturn(
            List.of(
                category(CategorySourceVO.EXPENSE, "EXP_GROCERY", "Market", "Food shopping"),
                category(CategorySourceVO.EXPENSE, "EXP_TAXI", "Taxi", null)));
    // CategoryServiceMock returns the "UNK" system category.
    catalog = StatementCategoryCatalog.load(categoryQueryRepo, new CategoryServiceMock());
  }

  @Test
  void shouldListIncomeOptionsThenUnknownWhenCredit() {
    // Act
    final List<CategoryOptionDTO> options = catalog.optionsFor(StatementMovementDirection.CREDIT);

    // Assert
    assertEquals(List.of("INC_SALARY", UNKNOWN), aliases(options));
  }

  @Test
  void shouldListExpenseOptionsThenUnknownWhenDebit() {
    // Act
    final List<CategoryOptionDTO> options = catalog.optionsFor(StatementMovementDirection.DEBIT);

    // Assert
    assertEquals(List.of("EXP_GROCERY", "EXP_TAXI", UNKNOWN), aliases(options));
  }

  @Test
  void shouldJoinNameAndDescriptionWhenBothExist() {
    // Act
    final CategoryOptionDTO option = catalog.optionsFor(StatementMovementDirection.DEBIT).get(0);

    // Assert
    assertEquals("Market: Food shopping", option.rule());
  }

  @Test
  void shouldUseNameOnlyWhenDescriptionIsMissing() {
    // Act
    final CategoryOptionDTO option = catalog.optionsFor(StatementMovementDirection.DEBIT).get(1);

    // Assert
    assertEquals("Taxi", option.rule());
  }

  @Test
  void shouldKeyQuestionByIndexWhenBuildingQuestion() {
    // Arrange
    final ExtractedMovementDTO movement =
        new ExtractedMovementDTO(
            LocalDate.of(2026, 9, 1),
            "COMPRA EXITO",
            new BigDecimal("120.50"),
            "COP",
            StatementMovementDirection.DEBIT);

    // Act
    final ClassificationQuestionDTO question = catalog.questionFor(3, movement);

    // Assert
    assertEquals("m_3", question.key());
    assertEquals("COMPRA EXITO", question.description());
    assertEquals(StatementMovementDirection.DEBIT, question.direction());
    assertEquals(List.of("EXP_GROCERY", "EXP_TAXI", UNKNOWN), aliases(question.options()));
  }

  @Test
  void shouldRejectAliasWhenItBelongsToOtherSource() {
    // Act & Assert
    assertFalse(catalog.isAllowed(StatementMovementDirection.DEBIT, "INC_SALARY"));
    assertTrue(catalog.isAllowed(StatementMovementDirection.DEBIT, "EXP_GROCERY"));
    assertTrue(catalog.isAllowed(StatementMovementDirection.CREDIT, UNKNOWN));
  }

  @Test
  void shouldReturnEmptyDisplayNameWhenAliasIsUnknown() {
    // Act & Assert
    assertTrue(catalog.displayNameOf("NOPE").isEmpty());
    assertEquals("Market", catalog.displayNameOf("EXP_GROCERY").get("en"));
    assertEquals(UNKNOWN, catalog.unknownAlias());
  }

  private static List<String> aliases(final List<CategoryOptionDTO> options) {
    return options.stream().map(CategoryOptionDTO::alias).toList();
  }

  private static CategoryDTO category(
      final CategorySourceVO source,
      final String alias,
      final String name,
      final String description) {
    return CategoryDTO.withEntity(
        new CategoryTypeVO(source, alias),
        1L,
        Map.of("en", name, "es", name),
        true,
        description == null ? null : Map.of("en", description, "es", description));
  }
}
