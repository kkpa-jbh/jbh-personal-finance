package com.jbh.finance.infra.adapters.in.rest.statement.response;

import com.jbh.finance.application.feature.statement.dto.StatementImportStatus;
import java.util.UUID;

public record StatementImportAcceptedResponse(UUID importId, String status) {

  /** The answer of an accepted upload: the import starts as {@code PROCESSING}. */
  public static StatementImportAcceptedResponse processing(final UUID importId) {
    return new StatementImportAcceptedResponse(importId, StatementImportStatus.PROCESSING.name());
  }
}
