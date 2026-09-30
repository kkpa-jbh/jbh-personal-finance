package com.jbh.finance.infra.adapters.exceptions;

import com.jbh.commons.exception.BusinessExceptionType;
import com.jbh.commons.util.JbhStringUtils;

@SuppressWarnings("PMD.LongVariable")
public enum FinanceInfraBusinessExceptionType implements BusinessExceptionType {
  INVALID_RANGE_DATES_FOR_MONTHLY_BALANCES(
      "Invalid range dates for monthly balances",
      "Rango de fechas no válido para los saldos mensuales"),
  STATEMENT_AI_NOT_CONFIGURED(
      "Statement import is not configured on the server",
      "La importación de extractos no está configurada en el servidor"),
  STATEMENT_CLASSIFIER_FAILED(
      "The movements could not be classified. Try again later",
      "No se pudieron clasificar los movimientos. Intenta más tarde"),
  STATEMENT_FILE_NOT_PDF("The file must be a PDF", "El archivo debe ser un PDF"),
  STATEMENT_FILE_REQUIRED("Upload a PDF file", "Sube un archivo PDF"),
  STATEMENT_FILE_TOO_LARGE("The PDF is too large", "El PDF es demasiado grande"),
  STATEMENT_PASSWORD_REQUIRED(
      "This PDF is locked. Type its password", "Este PDF está protegido. Escribe su contraseña"),
  STATEMENT_READER_FAILED(
      "The statement could not be read. Try again later",
      "No se pudo leer el extracto. Intenta más tarde"),
  STATEMENT_UNREADABLE_PDF("The PDF could not be opened", "No se pudo abrir el PDF"),
  STATEMENT_WRONG_PASSWORD("The PDF password is not correct", "La contraseña del PDF no es correcta"),
  ;

  private final String en;
  private final String es;

  FinanceInfraBusinessExceptionType(final String en, final String es) {
    this.en = en;
    this.es = es;
  }

  @Override
  public String getMessage() {
    return JbhStringUtils.buildJsonMessage(en, es);
  }
}
