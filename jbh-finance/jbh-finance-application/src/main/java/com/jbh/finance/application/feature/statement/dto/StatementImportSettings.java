package com.jbh.finance.application.feature.statement.dto;

import java.time.Duration;

/**
 * Settings of the statement import jobs.
 *
 * @param staleAfter a {@code PROCESSING} import older than this counts as failed
 */
public record StatementImportSettings(Duration staleAfter) {}
