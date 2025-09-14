package com.jbh.account.infra.adapters.in.service;

import static com.jbh.account.infra.common.utils.JbhStringUtils.isBlank;

import com.jbh.account.application.accounts.vo.AddBasicMovementRequest;
import com.jbh.account.infra.common.utils.JbhStringUtils;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service responsible for reading financial movement data from Excel files. Follows hexagonal
 * architecture principles with clear separation of concerns.
 */
@ApplicationScoped
public final class ExcelMovementReaderService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExcelMovementReaderService.class);

  private static final int ENTRY_DATE_COLUMN = 0;
  private static final int TOTAL_AMOUNT_COLUMN = 1;
  private static final int BALANCE_SNAPSHOT_COLUMN = 2;
  private static final int HEADER_ROW_INDEX = 0;
  private static final int CONSECUTIVE_EMPTY_ROWS_LIMIT = 2;

  private static final DateTimeFormatter[] DATE_FORMATTERS = {
    DateTimeFormatter.ofPattern("dd/MM/yyyy"),
    DateTimeFormatter.ofPattern("dd-MM-yyyy"),
    DateTimeFormatter.ofPattern("yyyy-MM-dd"),
    DateTimeFormatter.ofPattern("MM/dd/yyyy")
  };
  private FormulaEvaluator evaluator;

  /**
   * Parses European number format to BigDecimal. European format uses: - Period (.) as thousands
   * separator - Comma (,) as decimal separator
   *
   * @param amountStr The amount string in European format
   * @return BigDecimal representation of the amount
   * @throws NumberFormatException if the format is invalid
   */
  private static BigDecimal parseEuropeanAmount(final String amountStr) {
    if (isBlank(amountStr)) {
      return BigDecimal.ZERO;
    }

    // Handle negative numbers
    final boolean isNegative = amountStr.startsWith("-");
    final String absoluteAmountStr = isNegative ? amountStr.substring(1) : amountStr;

    // Remove thousands separators (periods) and replace decimal separator (comma) with dot
    final String normalizedAmount =
        absoluteAmountStr
            .replace(".", "") // Remove thousands separators
            .replace(",", "."); // Replace decimal separator

    final BigDecimal result = new BigDecimal(normalizedAmount);
    return isNegative ? result.negate() : result;
  }

  /**
   * Main method that reads Excel data and transforms it to AddBasicMovementRequest objects.
   *
   * @param fileInputStream The Excel file input stream
   * @param sheetName The name of the sheet to read from
   * @return List of AddBasicMovementRequest objects in the same order as Excel rows
   * @throws ExcelReadingException if there's an error reading or parsing the Excel file
   */
  public List<AddBasicMovementRequest> readMovementsFromExcel(
      final InputStream fileInputStream, final String sheetName) throws ExcelReadingException {

    LOGGER.info("Starting to read movements from Excel sheet: {}", sheetName);

    final List<List<String>> rawData = readRawDataFromExcel(fileInputStream, sheetName);
    final List<AddBasicMovementRequest> movements = transformRawDataToMovements(rawData);

    LOGGER.info("Successfully read {} movements from Excel sheet: {}", movements.size(), sheetName);
    return movements;
  }

  /**
   * Reads raw string data from the specified Excel sheet. Stops reading when encountering 2
   * consecutive empty rows.
   *
   * @param fileInputStream The Excel file input stream
   * @param sheetName The name of the sheet to read from
   * @return List of string arrays representing each row (excluding headers)
   * @throws ExcelReadingException if there's an error accessing the Excel file or sheet
   */
  private List<List<String>> readRawDataFromExcel(
      final InputStream fileInputStream, final String sheetName) throws ExcelReadingException {

    final List<List<String>> rawData = new ArrayList<>();

    try (Workbook workbook = new XSSFWorkbook(fileInputStream)) {
      final Sheet sheet = workbook.getSheet(sheetName);
      evaluator = workbook.getCreationHelper().createFormulaEvaluator();

      if (sheet == null) {
        throw new ExcelReadingException(
            String.format("Sheet '%s' not found in the Excel file", sheetName));
      }

      int consecutiveEmptyRows = 0;
      final int lastRowNum = sheet.getLastRowNum();

      // Start from row 1 to skip headers (row 0)
      for (int rowIndex = HEADER_ROW_INDEX + 1; rowIndex <= lastRowNum; rowIndex++) {
        final Row row = sheet.getRow(rowIndex);
        final List<String> rowData = extractRowData(row);

        if (isRowEmpty(rowData)) {
          consecutiveEmptyRows++;
          if (consecutiveEmptyRows >= CONSECUTIVE_EMPTY_ROWS_LIMIT) {
            LOGGER.debug(
                "Found {} consecutive empty rows at row {}. Stopping reading.",
                CONSECUTIVE_EMPTY_ROWS_LIMIT,
                rowIndex);
            break;
          }
        } else {
          consecutiveEmptyRows = 0;
          rawData.add(rowData);
        }
      }

    } catch (final IOException e) {
      throw new ExcelReadingException("Failed to read Excel file: " + e.getMessage(), e);
    }

    return rawData;
  }

  /**
   * Extracts data from the first three columns of a row.
   *
   * @param row The Excel row to extract data from
   * @return List containing the string values of the first three columns
   */
  private List<String> extractRowData(final Row row) {
    final List<String> rowData = new ArrayList<>();

    if (row == null) {
      // Add empty strings for null rows
      rowData.add("");
      rowData.add("");
      rowData.add("");
      return rowData;
    }

    // Extract only the first three columns
    for (int columnIndex = 0; columnIndex < 3; columnIndex++) {
      final Cell cell = row.getCell(columnIndex);
      final String cellValue = getCellValueAsString(cell);
      rowData.add(cellValue);
    }

    return rowData;
  }

  /**
   * Converts a cell value to string, handling different cell types.
   *
   * @param cell The Excel cell to convert
   * @return String representation of the cell value, empty string if null
   */
  private String getCellValueAsString(final Cell cell) {
    if (cell == null) {
      return "";
    }

    return switch (cell.getCellType()) {
      case STRING -> cell.getStringCellValue().trim();
      case NUMERIC -> {
        if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
          final LocalDate date = cell.getLocalDateTimeCellValue().toLocalDate();
          yield date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } else {
          // Format numeric values to avoid scientific notation
          final double numericValue = cell.getNumericCellValue();
          yield String.format("%.2f", numericValue).replace(",", ".");
        }
      }
      case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
      case FORMULA -> getCellValueAsString(evaluator.evaluateInCell(cell));
      default -> "";
    };
  }

  /**
   * Checks if a row is considered empty (all three columns are empty or whitespace).
   *
   * @param rowData List of string values representing the row
   * @return true if the row is empty, false otherwise
   */
  private boolean isRowEmpty(final List<String> rowData) {
    return rowData.stream().allMatch(JbhStringUtils::isBlank);
  }

  /**
   * Transforms raw string data to AddBasicMovementRequest objects.
   *
   * @param rawData List of string arrays representing Excel rows
   * @return List of AddBasicMovementRequest objects
   * @throws ExcelReadingException if there's an error parsing the data
   */
  private List<AddBasicMovementRequest> transformRawDataToMovements(
      final List<List<String>> rawData) throws ExcelReadingException {

    final List<AddBasicMovementRequest> movements = new ArrayList<>();

    for (int i = 0; i < rawData.size(); i++) {
      final List<String> rowData = rawData.get(i);
      final int excelRowNumber = i + 2; // +1 for 0-based index, +1 for skipped header

      final LocalDate entryDate = parseEntryDate(rowData.get(ENTRY_DATE_COLUMN), excelRowNumber);
      final BigDecimal totalAmount = parseAmount(rowData.get(TOTAL_AMOUNT_COLUMN));
      final BigDecimal balanceSnapshot = parseAmount(rowData.get(BALANCE_SNAPSHOT_COLUMN));
      movements.add(new AddBasicMovementRequest(entryDate, totalAmount, balanceSnapshot));
    }

    return movements;
  }

  /**
   * Parses an entry date string to LocalDate. Throws an exception if the date is null, empty, or in
   * an invalid format.
   *
   * @param dateStr The date string to parse
   * @param rowNumber The Excel row number (for error reporting)
   * @return LocalDate object
   * @throws ExcelReadingException if the date cannot be parsed
   */
  private LocalDate parseEntryDate(final String dateStr, final int rowNumber)
      throws ExcelReadingException {
    if (isBlank(dateStr)) {
      throw new ExcelReadingException(
          String.format("Entry date is null or empty in row %d", rowNumber));
    }

    final String trimmedDateStr = dateStr.trim();

    for (final DateTimeFormatter formatter : DATE_FORMATTERS) {
      try {
        return LocalDate.parse(trimmedDateStr, formatter);
      } catch (final DateTimeParseException e) {
        // Continue to next formatter
      }
    }

    throw new ExcelReadingException(
        String.format(
            "Invalid date format '%s' in row %d. Expected formats: dd/MM/yyyy, dd-MM-yyyy, yyyy-MM-dd, MM/dd/yyyy",
            trimmedDateStr, rowNumber));
  }

  /**
   * Parses an amount string to BigDecimal. Returns BigDecimal.ZERO for null or empty strings.
   *
   * @param amountStr The amount string to parse
   * @return BigDecimal representation of the amount
   * @throws ExcelReadingException if the amount cannot be parsed
   */
  @SuppressWarnings("PMD.PreserveStackTrace")
  private BigDecimal parseAmount(final String amountStr) throws ExcelReadingException {
    if (isBlank(amountStr)) {
      return BigDecimal.ZERO;
    }

    final String trimmedAmountStr = amountStr.trim();

    try {
      // First, try European format
      return parseEuropeanAmount(trimmedAmountStr);
    } catch (final NumberFormatException e) {
      try {
        // Fallback to plain number format
        return new BigDecimal(trimmedAmountStr);
      } catch (final NumberFormatException fallbackException) {
        final String message =
            String.format(
                "Invalid number format '%s'. Expected European format (1.234,56) or plain format (1234.56)",
                trimmedAmountStr);
        throw new ExcelReadingException(message, fallbackException);
      }
    }
  }

  /** Custom exception for Excel reading operations. */
  public static final class ExcelReadingException extends Exception implements Serializable {
    @Serial private static final long serialVersionUID = -7904385600828409187L;

    public ExcelReadingException(final String message) {
      super(message);
    }

    public ExcelReadingException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }
}
