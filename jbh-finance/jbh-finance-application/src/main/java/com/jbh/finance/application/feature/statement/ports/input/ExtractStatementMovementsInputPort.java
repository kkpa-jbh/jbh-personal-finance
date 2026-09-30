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

public class ExtractStatementMovementsInputPort implements ExtractStatementMovementsUseCase {
  private static final Logger LOG =
      LoggerFactory.getLogger(ExtractStatementMovementsInputPort.class);

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

    final ExtractedStatementDTO statement = statementReader.read(command.pdfContent());
    final List<ExtractedMovementDTO> movements = statement.movements();
    if (movements.isEmpty()) {
      throw new BusinessException(BusinessApplicationExceptionType.STATEMENT_NO_MOVEMENTS_FOUND);
    }

    final TotalsCheckDTO totals = totalsChecker.check(statement);
    final StatementCategoryCatalog catalog =
        StatementCategoryCatalog.load(categoryQueryRepo, categoryService);

    final Map<String, ClassificationAnswerDTO> answers = classify(movements, catalog);
    final List<CategorizedMovementDTO> categorized = categorize(movements, answers, catalog);

    return new StatementClassificationDTO(categorized, totals);
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
    int batches = 0;
    for (int start = 0; start < questions.size(); start += batchSize) {
      final int end = Math.min(start + batchSize, questions.size());
      for (final ClassificationAnswerDTO answer :
          movementClassifier.classify(questions.subList(start, end))) {
        answers.put(answer.key(), answer);
      }
      batches++;
    }

    LOG.info("Classified {} statement movements in {} batches", movements.size(), batches);
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

    LOG.info("{} of {} statement movements need review", lowConfidenceCount, movements.size());
    return categorized;
  }
}
