package com.jbh.finance.infra.adapters.out.ai.llmcompare;

import java.time.Duration;

/**
 * The result of one provider in the LLM comparison.
 *
 * @param provider the provider class name, for example {@code OllamaLlmAdapter}
 * @param answer the model's answer as it came, or {@code null} when the provider failed
 * @param error the error, or {@code null} when the provider answered
 * @param duration how long the provider took
 */
record ProviderRun(String provider, String answer, String error, Duration duration) {

  static ProviderRun answered(final String provider, final String answer, final Duration duration) {
    return new ProviderRun(provider, answer, null, duration);
  }

  static ProviderRun failed(final String provider, final String error, final Duration duration) {
    return new ProviderRun(provider, null, error, duration);
  }
}
