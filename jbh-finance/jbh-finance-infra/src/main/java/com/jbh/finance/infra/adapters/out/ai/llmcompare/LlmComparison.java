package com.jbh.finance.infra.adapters.out.ai.llmcompare;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.infra.adapters.out.ai.llm.StatementLlm;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Temporary: sends the same statement text to every {@link StatementLlm} and writes each model's
 * answer, as it came, to a text file (see {@link LlmCompareReport}). You compare them by hand.
 *
 * <p>The only entry point is {@link #wrap}, called once in {@code StatementUseCasesCDIConfig}. When
 * {@code jbh.statement.compare.enabled} is false, it returns the active LLM unchanged.
 *
 * <p>The other providers run in parallel, in the background, after the import already has its
 * answer: the import does not wait for them. Every {@link StatementLlm} bean is compared, so a new
 * provider joins the comparison with no change here.
 */
@ApplicationScoped
public class LlmComparison {

  private static final Logger LOGGER = LoggerFactory.getLogger(LlmComparison.class);
  private static final String LOG_PREFIX = "[StatementImport:LlmCompare]";

  private final LlmCompareConfig config;
  private final Instance<StatementLlm> llms;
  private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

  @Inject
  public LlmComparison(final LlmCompareConfig config, @Any final Instance<StatementLlm> llms) {
    this.config = config;
    this.llms = llms;
  }

  /**
   * Wraps the active LLM so each extraction also starts the comparison.
   *
   * @param active the LLM chosen by {@code jbh.statement.llm}
   * @return {@code active} itself when the comparison is off, else a wrapper around it
   */
  public StatementLlm wrap(final StatementLlm active) {
    if (!config.enabled()) {
      return active;
    }
    LOGGER.info(LOG_PREFIX + " Comparison is on. Results go to {}", config.outputDir());
    return new ComparingStatementLlm(active, providerName(active), this);
  }

  void startInBackground(final String statementText, final ProviderRun activeRun) {
    executor.submit(() -> compare(statementText, activeRun));
  }

  // A background task: any failure is logged here, or it would be lost.
  // CloseResource: every StatementLlm is @ApplicationScoped, so Handle.close() does nothing,
  // and the async run below still needs handle.get().
  @SuppressWarnings({"PMD.AvoidCatchingGenericException", "PMD.CloseResource"})
  private void compare(final String statementText, final ProviderRun activeRun) {
    try {
      final List<CompletableFuture<ProviderRun>> others = new ArrayList<>();
      final Map<String, String> requests = new LinkedHashMap<>();
      for (final Instance.Handle<StatementLlm> handle : llms.handles()) {
        final String name = handle.getBean().getBeanClass().getSimpleName();
        requests.put(name, handle.get().requestBody(statementText));
        if (!name.equals(activeRun.provider())) {
          others.add(
              CompletableFuture.supplyAsync(
                  () -> run(name, handle.get(), statementText), executor));
        }
      }

      final List<ProviderRun> runs = new ArrayList<>();
      runs.add(activeRun);
      others.forEach(other -> runs.add(other.join()));

      final Path folder = LlmCompareReport.write(Path.of(config.outputDir()), statementText, runs);
      LlmCompareReport.writeRequests(folder, requests);
      LOGGER.info(
          LOG_PREFIX + " Compared {} providers. Results in {}", runs.size(), folder.toAbsolutePath());
    } catch (final IOException | RuntimeException e) {
      LOGGER.warn(LOG_PREFIX + " Comparison failed: {}", e.getClass().getSimpleName());
    }
  }

  // One provider must not stop the others: every failure becomes a failed run.
  @SuppressWarnings("PMD.AvoidCatchingGenericException")
  private static ProviderRun run(
      final String name, final StatementLlm llm, final String statementText) {
    final long started = System.nanoTime();
    try {
      return ProviderRun.answered(name, llm.answer(statementText), since(started));
    } catch (final BusinessException e) {
      return ProviderRun.failed(
          name, String.valueOf(e.getBusinessExceptionType()), since(started));
    } catch (final RuntimeException e) {
      return ProviderRun.failed(name, e.getClass().getSimpleName(), since(started));
    }
  }

  /** The bean class name of {@code active}. A CDI client proxy is a subclass of the bean class. */
  // CloseResource: the handles hold @ApplicationScoped beans; Handle.close() does nothing.
  @SuppressWarnings("PMD.CloseResource")
  private String providerName(final StatementLlm active) {
    for (final Instance.Handle<StatementLlm> handle : llms.handles()) {
      final Class<?> beanClass = handle.getBean().getBeanClass();
      if (beanClass.isInstance(active)) {
        return beanClass.getSimpleName();
      }
    }
    return active.getClass().getSimpleName();
  }

  private static Duration since(final long started) {
    return Duration.ofNanos(System.nanoTime() - started);
  }

  @PreDestroy
  void stop() {
    executor.shutdown();
  }
}
