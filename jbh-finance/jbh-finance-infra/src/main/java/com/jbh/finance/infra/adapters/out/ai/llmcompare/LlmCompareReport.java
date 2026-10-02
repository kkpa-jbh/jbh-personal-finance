package com.jbh.finance.infra.adapters.out.ai.llmcompare;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Writes one comparison as text files, in a new folder per import:
 *
 * <ul>
 *   <li>{@code input.txt}: the statement text every provider received.
 *   <li>{@code <Provider>.txt}: the time, the error if any, and the model's answer as it came.
 * </ul>
 *
 * <p>The files hold real bank data. Keep the output folder out of git ({@code target/} is
 * ignored).
 */
final class LlmCompareReport {

  private static final DateTimeFormatter FOLDER_NAME =
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
    for (final ProviderRun run : runs) {
      Files.writeString(folder.resolve(run.provider() + TXT), content(run), StandardCharsets.UTF_8);
    }
    return folder;
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
