package com.jbh.finance.application.feature.statement.services;

import com.jbh.finance.application.feature.category.dto.CategoryDTO;
import com.jbh.finance.application.feature.category.ports.output.CategoryQueryRepo;
import com.jbh.finance.application.feature.category.services.CategoryService;
import com.jbh.finance.application.feature.statement.dto.CategoryOptionDTO;
import com.jbh.finance.application.feature.statement.dto.ClassificationQuestionDTO;
import com.jbh.finance.application.feature.statement.dto.ExtractedMovementDTO;
import com.jbh.finance.application.feature.statement.dto.StatementMovementDirection;
import com.jbh.finance.domain.category.vo.CategorySourceVO;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * The categories a statement movement may get, built once per request.
 *
 * <p>Money in may only get income categories, money out only expense categories. The last option of
 * each direction is its fallback: Other income for money in, Unknown expense for money out.
 */
public final class StatementCategoryCatalog {

  public static final String QUESTION_KEY_PREFIX = "m_";
  private static final String ENGLISH = "en";
  private static final String FALLBACK_RULE =
      "None of the other categories clearly fits this movement";
  private static final String RULE_SEPARATOR = ": ";

  private final Map<CategorySourceVO, List<CategoryOptionDTO>> optionsBySource;
  private final Map<String, CategoryDTO> categoriesByAlias;
  private final Map<CategorySourceVO, CategoryDTO> fallbackBySource;
  private final String unknownCategoryAlias;

  private StatementCategoryCatalog(
      final Map<CategorySourceVO, List<CategoryOptionDTO>> optionsBySource,
      final Map<String, CategoryDTO> categoriesByAlias,
      final Map<CategorySourceVO, CategoryDTO> fallbackBySource) {
    this.optionsBySource = optionsBySource;
    this.categoriesByAlias = categoriesByAlias;
    this.fallbackBySource = fallbackBySource;
    this.unknownCategoryAlias = fallbackBySource.get(CategorySourceVO.EXPENSE).getAlias();
  }

  /**
   * Loads the active income and expense categories and the two fallback categories.
   *
   * @param categoryQueryRepo source of the active categories
   * @param categoryService source of the Other income and Unknown expense categories
   * @return the catalog
   */
  public static StatementCategoryCatalog load(
      final CategoryQueryRepo categoryQueryRepo, final CategoryService categoryService) {
    final Map<CategorySourceVO, CategoryDTO> fallbackBySource =
        new EnumMap<>(CategorySourceVO.class);
    fallbackBySource.put(CategorySourceVO.INCOME, categoryService.findIncomeOther());
    fallbackBySource.put(CategorySourceVO.EXPENSE, categoryService.findExpenseUnknown());

    final Map<CategorySourceVO, List<CategoryOptionDTO>> optionsBySource =
        new EnumMap<>(CategorySourceVO.class);
    final Map<String, CategoryDTO> categoriesByAlias = new HashMap<>();

    for (final CategorySourceVO source : CategorySourceVO.values()) {
      final CategoryDTO fallback = fallbackBySource.get(source);
      final List<CategoryDTO> categories = categoryQueryRepo.findActiveBySource(source);
      categories.forEach(category -> categoriesByAlias.put(category.getAlias(), category));
      categoriesByAlias.put(fallback.getAlias(), fallback);
      optionsBySource.put(source, optionsOf(categories, fallback));
    }

    return new StatementCategoryCatalog(optionsBySource, categoriesByAlias, fallbackBySource);
  }

  /** The options of one source: its categories without the fallback, then the fallback last. */
  private static List<CategoryOptionDTO> optionsOf(
      final List<CategoryDTO> categories, final CategoryDTO fallback) {
    final String fallbackAlias = fallback.getAlias();
    final Stream<CategoryOptionDTO> known =
        categories.stream()
            .filter(category -> !fallbackAlias.equals(category.getAlias()))
            .map(category -> new CategoryOptionDTO(category.getAlias(), ruleOf(category)));
    return Stream.concat(known, Stream.of(new CategoryOptionDTO(fallbackAlias, FALLBACK_RULE)))
        .toList();
  }

  private static String ruleOf(final CategoryDTO category) {
    final String name = englishOf(category.getDisplayName());
    final String description = englishOf(category.getDescription());
    if (isBlank(name) && isBlank(description)) {
      return category.getAlias();
    }
    if (isBlank(description)) {
      return name;
    }
    if (isBlank(name)) {
      return description;
    }
    return name + RULE_SEPARATOR + description;
  }

  private static String englishOf(final Map<String, String> texts) {
    return texts == null ? null : texts.get(ENGLISH);
  }

  private static boolean isBlank(final String text) {
    return text == null || text.isBlank();
  }

  /**
   * The categories allowed for a direction. The Unknown option is always last.
   *
   * @param direction money in or money out
   * @return the options in order
   */
  public List<CategoryOptionDTO> optionsFor(final StatementMovementDirection direction) {
    return optionsBySource.get(direction.toCategorySource());
  }

  /**
   * Builds the classification question of one movement.
   *
   * @param index position of the movement in the statement
   * @param movement the movement
   * @return the question, keyed by the index
   */
  public ClassificationQuestionDTO questionFor(
      final int index, final ExtractedMovementDTO movement) {
    return new ClassificationQuestionDTO(
        keyOf(index), movement.description(), movement.direction(), optionsFor(movement.direction()));
  }

  /**
   * The question key of a movement.
   *
   * @param index position of the movement in the statement
   * @return the key
   */
  public static String keyOf(final int index) {
    return QUESTION_KEY_PREFIX + index;
  }

  /**
   * Checks that a category may be used for a direction.
   *
   * @param direction money in or money out
   * @param alias the category alias
   * @return {@code true} when the alias is one of the options of that direction
   */
  public boolean isAllowed(final StatementMovementDirection direction, final String alias) {
    return optionsFor(direction).stream().anyMatch(option -> option.alias().equals(alias));
  }

  /**
   * The alias a movement gets when the classifier has no valid answer: Other income for money in,
   * Unknown expense for money out.
   *
   * @param direction money in or money out
   * @return the fallback alias
   */
  public String fallbackAliasFor(final StatementMovementDirection direction) {
    return fallbackBySource.get(direction.toCategorySource()).getAlias();
  }

  /**
   * The category of an alias.
   *
   * @param alias the category alias, one of the options or a fallback
   * @return the category
   */
  public CategoryDTO categoryOf(final String alias) {
    return categoriesByAlias.get(alias);
  }

  /**
   * The alias of the Unknown expense category.
   *
   * @return the alias
   */
  public String unknownAlias() {
    return unknownCategoryAlias;
  }

  /**
   * The display name of a category.
   *
   * @param alias the category alias
   * @return the name per language, or an empty map when the alias is not known
   */
  public Map<String, String> displayNameOf(final String alias) {
    final CategoryDTO category = categoriesByAlias.get(alias);
    if (category == null || category.getDisplayName() == null) {
      return Map.of();
    }
    return category.getDisplayName();
  }
}
