package com.jbh.finance.infra.adapters.out.ai.llmcompare;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Temporary: keeps the request and the response of each Jev batch of one import, so {@link
 * JevReport} can write them to the text file.
 *
 * <p>Request scoped: {@code StatementImportRunner} activates one request context per import, so
 * each import has its own trace. Records nothing when {@code jbh.statement.compare.enabled} is off.
 */
@RequestScoped
public class JevTrace {

  private final LlmCompareConfig config;
  private final List<Batch> recordedBatches = new ArrayList<>();

  @Inject
  public JevTrace(final LlmCompareConfig config) {
    this.config = config;
  }

  /**
   * Keeps one batch.
   *
   * @param request the JSON body sent to Jev
   * @param response the body Jev answered, as it came
   */
  public void record(final String request, final String response) {
    if (config.enabled()) {
      recordedBatches.add(new Batch(request, response));
    }
  }

  /**
   * The batches of this import, in call order.
   *
   * @return a copy of the batches
   */
  public List<Batch> batches() {
    return List.copyOf(recordedBatches);
  }

  /**
   * One Jev call.
   *
   * @param request the JSON body sent to Jev
   * @param response the body Jev answered
   */
  public record Batch(String request, String response) {}
}
