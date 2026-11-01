package com.jbh.finance.test.testfixtures.builders;

import com.jbh.finance.application.feature.category.dto.CategoryDTO;
import com.jbh.finance.application.feature.statement.dto.CategorizedMovementDTO;
import com.jbh.finance.application.feature.statement.dto.StatementMovementDirection;
import com.jbh.finance.application.feature.statement.vo.PdfSha256;
import com.jbh.finance.domain.category.vo.CategorySourceVO;
import com.jbh.finance.domain.category.vo.CategoryTypeVO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/** Builds the categories and the categorized statement rows used by the statement import tests. */
public final class StatementFixtures {

  public static final String COP = "COP";
  public static final String USD = "USD";
  private static final int SHA256_HEX_LENGTH = 64;

  /** A valid digest value built without the digest code, so tests do not depend on it. */
  public static final PdfSha256 SHA = new PdfSha256("a".repeat(SHA256_HEX_LENGTH));

  private StatementFixtures() {
    throw new AssertionError("Utility class - do not instantiate");
  }

  /** A category with English and Spanish names, like the ones read from the database. */
  public static CategoryDTO category(
      final CategorySourceVO source, final String alias, final Long id, final String name) {
    return CategoryDTO.withEntity(
        new CategoryTypeVO(source, alias),
        id,
        Map.of("en", name, "es", name + " es"),
        true,
        Map.of("en", name + " description", "es", name));
  }

  /** A categorized statement row; the category is built from the alias. */
  public static CategorizedMovementDTO row(
      final LocalDate date,
      final StatementMovementDirection direction,
      final String amount,
      final CategoryDTO category) {
    return row(date, "ROW " + date + " " + amount, direction, amount, COP, category);
  }

  /** A categorized statement row with a currency; confidence 0.9 and no review. */
  public static CategorizedMovementDTO row(
      final LocalDate date,
      final String description,
      final StatementMovementDirection direction,
      final String amount,
      final String currency,
      final CategoryDTO category) {
    return rowWithConfidence(
        date, description, direction, amount, currency, category, new BigDecimal("0.9"), false);
  }

  /** A categorized statement row with an explicit confidence and review flag. */
  public static CategorizedMovementDTO rowWithConfidence(
      final LocalDate date,
      final String description,
      final StatementMovementDirection direction,
      final String amount,
      final String currency,
      final CategoryDTO category,
      final BigDecimal confidence,
      final boolean needsReview) {
    return new CategorizedMovementDTO(
        date,
        description,
        new BigDecimal(amount),
        currency,
        direction,
        category.getAlias(),
        category.getDisplayName(),
        confidence,
        needsReview,
        category);
  }
}
