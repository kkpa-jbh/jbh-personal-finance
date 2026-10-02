package com.jbh.finance.infra;

import com.jbh.finance.application.feature.category.ports.output.CategoryQueryRepo;
import com.jbh.finance.application.feature.category.services.CategoryService;
import com.jbh.finance.application.feature.statement.dto.ClassificationSettings;
import com.jbh.finance.application.feature.statement.ports.input.ExtractStatementMovementsInputPort;
import com.jbh.finance.application.feature.statement.ports.output.MovementClassifier;
import com.jbh.finance.application.feature.statement.ports.output.StatementReader;
import com.jbh.finance.application.feature.statement.usecases.ExtractStatementMovementsUseCase;
import com.jbh.finance.infra.adapters.out.ai.AzureLayoutTextSourceAdapter;
import com.jbh.finance.infra.adapters.out.ai.OcrLlmStatementReaderAdapter;
import com.jbh.finance.infra.adapters.out.ai.PdfBoxTextSource;
import com.jbh.finance.infra.adapters.out.ai.StatementTextSource;
import com.jbh.finance.infra.adapters.out.ai.llm.GeminiLlmAdapter;
import com.jbh.finance.infra.adapters.out.ai.llm.OpenRouterLlmAdapter;
import com.jbh.finance.infra.adapters.out.ai.llm.StatementLlm;
import com.jbh.finance.infra.adapters.out.ai.llmcompare.LlmComparison;
import com.jbh.finance.infra.config.StatementImportConfig;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

/** CDI wiring of the bank statement import. Kept apart so ProductUseCasesCDIConfig does not grow. */
@ApplicationScoped
@RegisterForReflection(targets = {ExtractStatementMovementsInputPort.class})
public class StatementUseCasesCDIConfig {

  @Inject CategoryQueryRepo categoryQueryRepo;

  @Inject CategoryService categoryService;

  // Instance, not a plain injection: each step has several implementations, and the config
  // (jbh.statement.text-source, jbh.statement.llm) picks one of each.
  @Inject @Any Instance<StatementTextSource> textSources;

  @Inject @Any Instance<StatementLlm> llms;

  @Inject MovementClassifier movementClassifier;

  @Inject StatementImportConfig config;

  // Temporary LLM comparison; a no-op unless jbh.statement.compare.enabled=true.
  @Inject LlmComparison llmComparison;

  @Produces
  @ApplicationScoped
  public ExtractStatementMovementsUseCase extractStatementMovementsUseCase() {
    return new ExtractStatementMovementsInputPort(
        statementReader(),
        movementClassifier,
        categoryQueryRepo,
        categoryService,
        new ClassificationSettings(config.jev().batchSize(), config.jev().minConfidence()));
  }

  private StatementReader statementReader() {
    return new OcrLlmStatementReaderAdapter(textSource(), llmComparison.wrap(llm()));
  }

  private StatementTextSource textSource() {
    return switch (config.textSource()) {
      case AZURE -> textSources.select(AzureLayoutTextSourceAdapter.class).get();
      case PDFBOX -> textSources.select(PdfBoxTextSource.class).get();
    };
  }

  private StatementLlm llm() {
    return switch (config.llm()) {
      case GEMINI -> llms.select(GeminiLlmAdapter.class).get();
      case OPENROUTER -> llms.select(OpenRouterLlmAdapter.class).get();
    };
  }
}
