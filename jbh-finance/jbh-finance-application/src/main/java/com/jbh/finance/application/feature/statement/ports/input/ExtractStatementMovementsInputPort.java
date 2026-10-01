package com.jbh.finance.application.feature.statement.ports.input;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.finance.application.common.logging.LoggerFactory;
import com.jbh.finance.application.feature.category.ports.output.CategoryQueryRepo;
import com.jbh.finance.application.feature.category.services.CategoryService;
import com.jbh.finance.application.feature.statement.commands.ExtractStatementMovementsCommand;
import com.jbh.finance.application.feature.statement.dto.CategorizedMovementDTO;
import com.jbh.finance.application.feature.statement.dto.ClassificationAnswerDTO;
import com.jbh.finance.application.feature.statement.dto.ClassificationQuestionDTO;
import com.jbh.finance.application.feature.statement.dto.ClassificationSettings;
import com.jbh.finance.application.feature.statement.dto.ExtractedMovementDTO;
import com.jbh.finance.application.feature.statement.dto.ExtractedStatementDTO;
import com.jbh.finance.application.feature.statement.dto.StatementClassificationDTO;
import com.jbh.finance.application.feature.statement.dto.TotalsCheckDTO;
import com.jbh.finance.application.feature.statement.ports.output.MovementClassifier;
import com.jbh.finance.application.feature.statement.ports.output.StatementReader;
import com.jbh.finance.application.feature.statement.services.StatementCategoryCatalog;
import com.jbh.finance.application.feature.statement.services.StatementTotalsChecker;
import com.jbh.finance.application.feature.statement.usecases.ExtractStatementMovementsUseCase;
import com.jbh.finance.application.shared.exceptions.BusinessApplicationExceptionType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;

/**
 * The bank statement import. It owns the 4 steps, so its log lines trace one import end to end.
 *
 * <p>Every line starts with {@code [StatementImport]}; the adapters use {@code
 * [StatementImport:<Name>]}. {@code grep StatementImport} shows a whole import. Only counts, step
 * names and times are logged: the statement content is personal data.
 */
public class ExtractStatementMovementsInputPort implements ExtractStatementMovementsUseCase {
  private static final Logger LOG =
      LoggerFactory.getLogger(ExtractStatementMovementsInputPort.class);

  private static final String LOG_PREFIX = "[StatementImport]";
  private static final String STEP_READ = "Step 1/4 read statement";
  private static final String STEP_TOTALS = "Step 2/4 check totals";
  private static final String STEP_CATEGORIES = "Step 3/4 load categories";
  private static final String STEP_CLASSIFY = "Step 4/4 classify movements";
  private static final long NANOS_PER_MILLI = 1_000_000L;

  private final StatementReader statementReader;
  private final MovementClassifier movementClassifier;
  private final CategoryQueryRepo categoryQueryRepo;
  private final CategoryService categoryService;
  private final ClassificationSettings settings;
  private final StatementTotalsChecker totalsChecker;

  public ExtractStatementMovementsInputPort(
      final StatementReader statementReader,
      final MovementClassifier movementClassifier,
      final CategoryQueryRepo categoryQueryRepo,
      final CategoryService categoryService,
      final ClassificationSettings settings) {
    this.statementReader = statementReader;
    this.movementClassifier = movementClassifier;
    this.categoryQueryRepo = categoryQueryRepo;
    this.categoryService = categoryService;
    this.settings = settings;
    this.totalsChecker = new StatementTotalsChecker();
  }

  @Override
  public StatementClassificationDTO extract(final ExtractStatementMovementsCommand command)
      throws BusinessException {
    if (command == null) {
      throw new GenericSpecificationException("Command cannot be null");
    }
    command.validate();

    final byte[] pdfContent = command.pdfContent();
    final long started = System.nanoTime();
    LOG.info(LOG_PREFIX + " Start: PDF of {} bytes", pdfContent.length);

    // Tells the failure line which step broke.
    String step = STEP_READ;
    try {
      // Step 1: the StatementReader adapter turns the PDF into movements (OCR + LLM).
      long stepStarted = startStep(step);
      final ExtractedStatementDTO statement = statementReader.read(pdfContent);
      final List<ExtractedMovementDTO> movements = statement.movements();
      LOG.info(
          LOG_PREFIX + " {} done in {} ms: {} movements",
          step,
          millisSince(stepStarted),
          movements.size());
      if (movements.isEmpty()) {
        throw new BusinessException(BusinessApplicationExceptionType.STATEMENT_NO_MOVEMENTS_FOUND);
      }

      // Step 2: compare the movements with the balance change printed on the statement.
      step = STEP_TOTALS;
      final TotalsCheckDTO totals = totalsChecker.check(statement);
      LOG.info(LOG_PREFIX + " {} done: {}", step, totals.status());

      // Step 3: the categories the classifier may choose from (database).
      step = STEP_CATEGORIES;
      stepStarted = startStep(step);
      final StatementCategoryCatalog catalog =
          StatementCategoryCatalog.load(categoryQueryRepo, categoryService);
      LOG.info(LOG_PREFIX + " {} done in {} ms", step, millisSince(stepStarted));

      // Step 4: the MovementClassifier adapter picks one category per movement, in batches.
      step = STEP_CLASSIFY;
      stepStarted = startStep(step);
      final Map<String, ClassificationAnswerDTO> answers = classify(movements, catalog);
      final List<CategorizedMovementDTO> categorized = categorize(movements, answers, catalog);
      LOG.info(LOG_PREFIX + " {} done in {} ms", step, millisSince(stepStarted));

      LOG.info(
          LOG_PREFIX + " Done in {} ms: {} movements, totals {}",
          millisSince(started),
          categorized.size(),
          totals.status());
      return new StatementClassificationDTO(categorized, totals);

    } catch (final BusinessException e) {
      LOG.warn(LOG_PREFIX + " Failed at {} after {} ms", step, millisSince(started));
      throw e;
    }
  }

  private static long startStep(final String step) {
    LOG.info(LOG_PREFIX + " {} started", step);
    return System.nanoTime();
  }

  private static long millisSince(final long startNanos) {
    return (System.nanoTime() - startNanos) / NANOS_PER_MILLI;
  }

  private Map<String, ClassificationAnswerDTO> classify(
      final List<ExtractedMovementDTO> movements, final StatementCategoryCatalog catalog)
      throws BusinessException {
    final List<ClassificationQuestionDTO> questions = new ArrayList<>(movements.size());
    for (int i = 0; i < movements.size(); i++) {
      questions.add(catalog.questionFor(i, movements.get(i)));
    }

    final Map<String, ClassificationAnswerDTO> answers = new HashMap<>();
    final int batchSize = settings.batchSize();
    final int totalBatches = (questions.size() + batchSize - 1) / batchSize;
    int batch = 0;
    for (int start = 0; start < questions.size(); start += batchSize) {
      final int end = Math.min(start + batchSize, questions.size());
      batch++;
      final long batchStarted = System.nanoTime();
      for (final ClassificationAnswerDTO answer :
          movementClassifier.classify(questions.subList(start, end))) {
        answers.put(answer.key(), answer);
      }
      LOG.info(
          LOG_PREFIX + " {} batch {}/{} ({} movements) done in {} ms",
          STEP_CLASSIFY,
          batch,
          totalBatches,
          end - start,
          millisSince(batchStarted));
    }
    return answers;
  }

  private List<CategorizedMovementDTO> categorize(
      final List<ExtractedMovementDTO> movements,
      final Map<String, ClassificationAnswerDTO> answers,
      final StatementCategoryCatalog catalog) {
    final List<CategorizedMovementDTO> categorized = new ArrayList<>(movements.size());
    int lowConfidenceCount = 0;

    for (int i = 0; i < movements.size(); i++) {
      final ExtractedMovementDTO movement = movements.get(i);
      final ClassificationAnswerDTO answer = answers.get(StatementCategoryCatalog.keyOf(i));
      final boolean validAnswer =
          answer != null
              && answer.confidence() != null
              && catalog.isAllowed(movement.direction(), answer.alias());

      final String alias = validAnswer ? answer.alias() : catalog.unknownAlias();
      final BigDecimal confidence = validAnswer ? answer.confidence() : BigDecimal.ZERO;
      final boolean lowConfidence =
          alias.equals(catalog.unknownAlias())
              || confidence.compareTo(settings.minConfidence()) < 0;
      if (lowConfidence) {
        lowConfidenceCount++;
      }

      categorized.add(
          new CategorizedMovementDTO(
              movement.date(),
              movement.description(),
              movement.amount(),
              movement.direction(),
              alias,
              catalog.displayNameOf(alias),
              confidence,
              lowConfidence));
    }

    LOG.info(
        LOG_PREFIX + " {} of {} movements need review", lowConfidenceCount, movements.size());
    return categorized;
  }
}
