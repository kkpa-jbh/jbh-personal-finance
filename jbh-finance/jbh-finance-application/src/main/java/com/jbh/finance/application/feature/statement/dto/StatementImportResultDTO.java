package com.jbh.finance.application.feature.statement.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The answer to a poll of a statement import.
 *
 * @param importId the import id
 * @param status the effective status (a stale {@code PROCESSING} shows as {@code FAILED})
 * @param errorCode the error name when {@code FAILED}, else {@code null}
 * @param startedAt when the import started
 * @param finishedAt when the import ended, or {@code null}
 * @param summary the summary when {@code DONE}, else {@code null}
 * @param movements the saved movements sorted by date; empty unless {@code DONE}
 */
public record StatementImportResultDTO(
    UUID importId,
    StatementImportStatus status,
    String errorCode,
    Instant startedAt,
    Instant finishedAt,
    StatementImportSummaryDTO summary,
    List<StatementImportMovementDTO> movements) {}
