package com.jbh.finance.infra.adapters.out.ai.llmcompare;

import com.jbh.finance.infra.adapters.out.ai.llm.StatementExtractionFormat;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Writes one comparison as text files, in a new folder per import:
 *
 * <ul>
 *   <li>{@code input.txt}: the statement text every provider received.
 *   <li>{@code prompt.txt}: the system prompt and the user prompt, as plain text.
 *   <li>{@code <Provider>-request.json}: the exact request body each provider sends.
 *   <li>{@code <Provider>.txt}: the time, the error if any, and the model's answer as it came.
 * </ul>
 *
 * <p>The files hold real bank data. Keep the output folder out of git ({@code target/} is
 * ignored).
 */
final class LlmCompareReport {

  static final DateTimeFormatter FOLDER_NAME =
      DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
  private static final String TXT = ".txt";
  private static final String NONE = "-";

  private LlmCompareReport() {}

  /**
   * Writes the files of one comparison.
   *
   * @param outputDir the parent folder; a new folder is created inside it
   * @param statementText the text every provider received
   * @param runs one run per provider
   * @return the new folder
   * @throws IOException when a file cannot be written
   */
  static Path write(final Path outputDir, final String statementText, final List<ProviderRun> runs)
      throws IOException {
    final Path folder = outputDir.resolve(LocalDateTime.now().format(FOLDER_NAME));
    Files.createDirectories(folder);
    Files.writeString(folder.resolve("input" + TXT), statementText, StandardCharsets.UTF_8);
    Files.writeString(folder.resolve("prompt" + TXT), prompt(statementText), StandardCharsets.UTF_8);
    for (final ProviderRun run : runs) {
      Files.writeString(folder.resolve(run.provider() + TXT), content(run), StandardCharsets.UTF_8);
    }
    return folder;
  }

  /**
   * Writes the request body of each provider, so you can see exactly what it was sent.
   *
   * @param folder the folder of this comparison
   * @param requests provider name to request body
   * @throws IOException when a file cannot be written
   */
  static void writeRequests(final Path folder, final Map<String, String> requests)
      throws IOException {
    for (final Map.Entry<String, String> request : requests.entrySet()) {
      Files.writeString(
          folder.resolve(request.getKey() + "-request.json"),
          request.getValue(),
          StandardCharsets.UTF_8);
    }
  }

  private static String prompt(final String statementText) {
    return "SYSTEM PROMPT (every provider; Ollama also adds the JSON shape at the end, see"
        + " OllamaLlmAdapter-request.json)\n\n"
        + StatementExtractionFormat.SYSTEM_PROMPT
        + "\n\nUSER PROMPT\n\n"
        + StatementExtractionFormat.userPrompt(statementText);
  }

  private static String content(final ProviderRun run) {
    return "Provider: "
        + run.provider()
        + "\nTime:     "
        + run.duration().toMillis()
        + " ms\nError:    "
        + (run.error() == null ? NONE : run.error())
        + "\n\n"
        + (run.answer() == null ? "" : run.answer());
  }
}
