package utils;

import com.jbh.account_app.accounts.vo.AddMovementWithDateAmount;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TestDataFactory {

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  /**
   * Creates test data for AddEntryWithDateAmount based on the account movement history. Data represents account entries
   * with dates, movement amounts, and balance snapshots. NU
   */
  public static List<AddMovementWithDateAmount> createAccountMovementTestData() {
    return List.of(
        new AddMovementWithDateAmount(
            parseDate("30/07/2024"),
            parseEuropeanAmount("12.591.000,00"),
            parseEuropeanAmount("12.689.712,00")
        ),
        new AddMovementWithDateAmount(
            parseDate("31/08/2024"),
            parseEuropeanAmount("22.685.312,00"),
            parseEuropeanAmount("35.693.653,00")
        ),
        new AddMovementWithDateAmount(
            parseDate("30/09/2024"),
            parseEuropeanAmount("0,00"),
            parseEuropeanAmount("36.017.457,00")
        ),
        new AddMovementWithDateAmount(
            parseDate("30/10/2024"),
            BigDecimal.ZERO, // No amount specified in the image
            parseEuropeanAmount("36.357.576,00")
        ),
        new AddMovementWithDateAmount(
            parseDate("30/11/2024"),
            parseEuropeanAmount("-673.605,00"),
            parseEuropeanAmount("35.982.309,00")
        ),
        new AddMovementWithDateAmount(
            parseDate("30/12/2024"),
            parseEuropeanAmount("-1.271.000,00"),
            parseEuropeanAmount("35.000.981,00")
        ),
        new AddMovementWithDateAmount(
            parseDate("31/01/2025"),
            parseEuropeanAmount("-9.590.134,00"),
            parseEuropeanAmount("25.638.626,00")
        ),
        new AddMovementWithDateAmount(
            parseDate("01/02/2025"),
            parseEuropeanAmount("-6.780.000,00"),
            parseEuropeanAmount("18.884.107,00")
        ),
        new AddMovementWithDateAmount(
            parseDate("28/02/2025"),
            parseEuropeanAmount("-14.979.690,00"),
            parseEuropeanAmount("3.768.488,00")
        ),
        new AddMovementWithDateAmount(
            parseDate("31/03/2025"),
            parseEuropeanAmount("-3.768.488,00"),
            parseEuropeanAmount("0,00")
        )
    );
  }

  /**
   * Parses European date format (dd/MM/yyyy) to LocalDate
   */
  private static LocalDate parseDate(String dateStr) {
    return LocalDate.parse(dateStr, DATE_FORMATTER);
  }

  /**
   * Parses European number format to BigDecimal European format uses: - Period (.) as thousands separator - Comma (,)
   * as decimal separator
   */
  private static BigDecimal parseEuropeanAmount(String amountStr) {
    if (amountStr == null || amountStr.trim().isEmpty()) {
      return BigDecimal.ZERO;
    }

    // Remove thousands separators (periods) and replace decimal separator (comma) with dot
    String normalizedAmount = amountStr
        .replace(".", "")           // Remove thousands separators
        .replace(",", ".");         // Replace decimal separator

    return new BigDecimal(normalizedAmount);
  }

  /**
   * Creates extended test data for AddEntryWithDateAmount covering the period from 2023 to 2025. Data represents a
   * longer account movement history with various deposits, withdrawals, and interest accruals. PI BI
   */
  public static List<AddMovementWithDateAmount> createExtendedAccountMovementTestData() {
    return List.of(
        new AddMovementWithDateAmount(
            parseDate("31/10/2023"),
            parseEuropeanAmount("13.010.000,00"),
            parseEuropeanAmount("13.062.118,00")
        ),
        new AddMovementWithDateAmount(
            parseDate("30/11/2023"),
            parseEuropeanAmount("13.000.000,00"),
            parseEuropeanAmount("26.309.995,00")
        ),
        new AddMovementWithDateAmount(
            parseDate("31/12/2023"),
            parseEuropeanAmount("13.000.000,00"),
            parseEuropeanAmount("39.645.174,00")
        ),
        new AddMovementWithDateAmount(
            parseDate("12/01/2024"),
            parseEuropeanAmount("0,00"),
            parseEuropeanAmount("39.645.174,00")
        ),
        new AddMovementWithDateAmount(
            parseDate("31/01/2024"),
            parseEuropeanAmount("0,00"),
            parseEuropeanAmount("40.001.713,00")
        ),
        new AddMovementWithDateAmount(
            parseDate("28/02/2024"),
            parseEuropeanAmount("0,00"),
            parseEuropeanAmount("40.001.713,00")
        ),
        new AddMovementWithDateAmount(
            parseDate("31/03/2024"),
            parseEuropeanAmount("0,00"),
            parseEuropeanAmount("40.701.019,00")
        ),
        new AddMovementWithDateAmount(
            parseDate("30/04/2024"),
            parseEuropeanAmount("0,00"),
            parseEuropeanAmount("41.197.965,00")
        ),
        new AddMovementWithDateAmount(
            parseDate("03/05/2024"),
            parseEuropeanAmount("24.100.000,00"),
            parseEuropeanAmount("65.394.365,00")
        ),
        new AddMovementWithDateAmount(
            parseDate("05/05/2024"),
            parseEuropeanAmount("-10.000.000,00"),
            parseEuropeanAmount("55.394.365,00")
        ),
        new AddMovementWithDateAmount(
            parseDate("16/05/2024"),
            parseEuropeanAmount("-3.000.000,00"),
            parseEuropeanAmount("52.394.365,00")
        ),
        new AddMovementWithDateAmount(
            parseDate("31/05/2024"),
            parseEuropeanAmount("-17.000.000,00"),
            parseEuropeanAmount("35.762.219,00")
        ),
        new AddMovementWithDateAmount(
            parseDate("30/06/2024"),
            parseEuropeanAmount("16.600.000,00"),
            parseEuropeanAmount("52.889.397,00")
        ),
        new AddMovementWithDateAmount(
            parseDate("31/07/2024"),
            parseEuropeanAmount("-20.000.000,00"),
            parseEuropeanAmount("32.809.397,00")
        ),
        new AddMovementWithDateAmount(
            parseDate("31/08/2024"),
            parseEuropeanAmount("0,00"),
            parseEuropeanAmount("33.560.000,00")
        ),
        new AddMovementWithDateAmount(
            parseDate("30/09/2024"),
            parseEuropeanAmount("0,00"),
            parseEuropeanAmount("33.876.037,00")
        ),
        new AddMovementWithDateAmount(
            parseDate("31/10/2024"),
            parseEuropeanAmount("0,00"),
            parseEuropeanAmount("34.204.708,00")
        ),
        new AddMovementWithDateAmount(
            parseDate("30/11/2024"),
            parseEuropeanAmount("0,00"),
            parseEuropeanAmount("34.525.863,00")
        ),
        new AddMovementWithDateAmount(
            parseDate("16/12/2024"),
            parseEuropeanAmount("0,00"),
            parseEuropeanAmount("34.838.994,00")
        ),
        new AddMovementWithDateAmount(
            parseDate("31/01/2025"),
            parseEuropeanAmount("0,00"),
            parseEuropeanAmount("35.102.299,00")
        ),
        new AddMovementWithDateAmount(
            parseDate("28/02/2025"),
            parseEuropeanAmount("0,00"),
            parseEuropeanAmount("35.341.920,00")
        ),
        new AddMovementWithDateAmount(
            parseDate("31/03/2025"),
            parseEuropeanAmount("0,00"),
            parseEuropeanAmount("35.593.716,00")
        ),
        new AddMovementWithDateAmount(
            parseDate("30/04/2025"),
            parseEuropeanAmount("0,00"),
            parseEuropeanAmount("35.829.015,00")
        ),
        new AddMovementWithDateAmount(
            parseDate("31/05/2025"),
            parseEuropeanAmount("0,00"),
            parseEuropeanAmount("36.102.719,00")
        ),
        new AddMovementWithDateAmount(
            parseDate("30/08/2025"),
            BigDecimal.ZERO, // No total amount visible in the image
            parseEuropeanAmount("37.074.883,00")
        )
    );
  }
}
