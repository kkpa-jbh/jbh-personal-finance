package com.jbh.finance.application.feature.statement.ports.output;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.statement.dto.ExtractedStatementDTO;

/** Reads an unlocked statement PDF and returns its movements. One implementation per AI provider. */
public interface StatementReader {
  ExtractedStatementDTO read(byte[] pdfContent) throws BusinessException;
}
