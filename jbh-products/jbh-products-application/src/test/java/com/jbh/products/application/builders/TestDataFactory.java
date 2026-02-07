package com.jbh.products.application.builders;

import static com.jbh.commons.util.JbhMoneyUtils.withJBHDecimals;
import static com.jbh.products.application.core.usecases.utils.MonthlyBalanceITUtils.createMonthlyBalanceCommand;

import com.jbh.products.application.feature.monthlybalance.commands.AddMonthlyBalanceCommand;
import com.jbh.products.application.feature.monthlybalance.commands.MonthlyBalanceCommandVO;
import com.jbh.products.application.feature.movement.commands.AddMovementUploadedFileCommand;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TestDataFactory {

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  /**
   * Creates test data for AddEntryWithDateAmount based on the account movement history. Data
   * represents account entries with dates, movement amounts, and balance snapshots. NU
   */
  public static List<AddMovementUploadedFileCommand> createAccountMovementTestData() {
    return List.of(
        new AddMovementUploadedFileCommand(
            parseDate("30/07/2024"),
            parseEuropeanAmount("12.591.000,00"),
            parseEuropeanAmount("12.689.712,00")),
        new AddMovementUploadedFileCommand(
            parseDate("31/08/2024"),
            parseEuropeanAmount("22.685.312,00"),
            parseEuropeanAmount("35.693.653,00")),
        new AddMovementUploadedFileCommand(
            parseDate("30/09/2024"),
            parseEuropeanAmount("0,00"),
            parseEuropeanAmount("36.017.457,00")),
        new AddMovementUploadedFileCommand(
            parseDate("30/10/2024"),
            BigDecimal.ZERO, // No amount specified in the image
            parseEuropeanAmount("36.357.576,00")),
        new AddMovementUploadedFileCommand(
            parseDate("30/11/2024"),
            parseEuropeanAmount("-673.605,00"),
            parseEuropeanAmount("35.982.309,00")),
        new AddMovementUploadedFileCommand(
            parseDate("30/12/2024"),
            parseEuropeanAmount("-1.271.000,00"),
            parseEuropeanAmount("35.000.981,00")),
        new AddMovementUploadedFileCommand(
            parseDate("31/01/2025"),
            parseEuropeanAmount("-9.590.134,00"),
            parseEuropeanAmount("25.638.626,00")),
        new AddMovementUploadedFileCommand(
            parseDate("01/02/2025"),
            parseEuropeanAmount("-6.780.000,00"),
            parseEuropeanAmount("18.884.107,00")),
        new AddMovementUploadedFileCommand(
            parseDate("28/02/2025"),
            parseEuropeanAmount("-14.979.690,00"),
            parseEuropeanAmount("3.768.488,00")),
        new AddMovementUploadedFileCommand(
            parseDate("31/03/2025"),
            parseEuropeanAmount("-3.768.488,00"),
            parseEuropeanAmount("0,00")));
  }

  /** Parses European date format (dd/MM/yyyy) to LocalDate */
  private static LocalDate parseDate(final String dateStr) {
    return LocalDate.parse(dateStr, DATE_FORMATTER);
  }

  /**
   * Parses European number format to BigDecimal European format uses: - Period (.) as thousands
   * separator - Comma (,) as decimal separator
   */
  public static BigDecimal parseEuropeanAmount(final String amountStr) {
    if (amountStr == null || amountStr.trim().isEmpty()) {
      return BigDecimal.ZERO;
    }

    // Remove thousands separators (periods) and replace decimal separator (comma) with dot
    final String normalizedAmount =
        amountStr
            .replace(".", "") // Remove thousands separators
            .replace(",", "."); // Replace decimal separator

    return new BigDecimal(normalizedAmount);
  }

  /**
   * Creates extended test data for AddBasicMovementRequest covering the period from 2023 to 2025.
   * Data represents a longer account movement history with various deposits, withdrawals, and
   * interest accruals. PI BI
   */
  public static List<AddMovementUploadedFileCommand> movementsForPIBI() {
    return List.of(
        new AddMovementUploadedFileCommand(
            parseDate("31/10/2023"),
            parseEuropeanAmount("13.010.000,00"),
            parseEuropeanAmount("13.062.118,00")),
        new AddMovementUploadedFileCommand(
            parseDate("30/11/2023"),
            parseEuropeanAmount("13.000.000,00"),
            parseEuropeanAmount("26.309.995,00")),
        new AddMovementUploadedFileCommand(
            parseDate("31/12/2023"),
            parseEuropeanAmount("13.000.000,00"),
            parseEuropeanAmount("39.645.174,00")),
        new AddMovementUploadedFileCommand(
            parseDate("12/01/2024"),
            parseEuropeanAmount("0,00"),
            parseEuropeanAmount("39.645.174,00")),
        new AddMovementUploadedFileCommand(
            parseDate("31/01/2024"),
            parseEuropeanAmount("0,00"),
            parseEuropeanAmount("40.001.713,00")),
        new AddMovementUploadedFileCommand(
            parseDate("28/02/2024"),
            parseEuropeanAmount("0,00"),
            parseEuropeanAmount("40.001.713,00")),
        new AddMovementUploadedFileCommand(
            parseDate("31/03/2024"),
            parseEuropeanAmount("0,00"),
            parseEuropeanAmount("40.701.019,00")),
        new AddMovementUploadedFileCommand(
            parseDate("30/04/2024"),
            parseEuropeanAmount("0,00"),
            parseEuropeanAmount("41.197.965,00")),
        new AddMovementUploadedFileCommand(
            parseDate("03/05/2024"),
            parseEuropeanAmount("24.100.000,00"),
            parseEuropeanAmount("65.394.365,00")),
        new AddMovementUploadedFileCommand(
            parseDate("05/05/2024"),
            parseEuropeanAmount("-10.000.000,00"),
            parseEuropeanAmount("55.394.365,00")),
        new AddMovementUploadedFileCommand(
            parseDate("16/05/2024"),
            parseEuropeanAmount("-3.000.000,00"),
            parseEuropeanAmount("52.394.365,00")),
        new AddMovementUploadedFileCommand(
            parseDate("31/05/2024"),
            parseEuropeanAmount("-17.000.000,00"),
            parseEuropeanAmount("35.762.219,00")),
        new AddMovementUploadedFileCommand(
            parseDate("30/06/2024"),
            parseEuropeanAmount("16.600.000,00"),
            parseEuropeanAmount("52.889.397,00")),
        new AddMovementUploadedFileCommand(
            parseDate("31/07/2024"),
            parseEuropeanAmount("-20.000.000,00"),
            parseEuropeanAmount("32.809.397,00")),
        new AddMovementUploadedFileCommand(
            parseDate("31/08/2024"),
            parseEuropeanAmount("0,00"),
            parseEuropeanAmount("33.560.000,00")),
        new AddMovementUploadedFileCommand(
            parseDate("30/09/2024"),
            parseEuropeanAmount("0,00"),
            parseEuropeanAmount("33.876.037,00")),
        new AddMovementUploadedFileCommand(
            parseDate("31/10/2024"),
            parseEuropeanAmount("0,00"),
            parseEuropeanAmount("34.204.708,00")),
        new AddMovementUploadedFileCommand(
            parseDate("30/11/2024"),
            parseEuropeanAmount("0,00"),
            parseEuropeanAmount("34.525.863,00")),
        new AddMovementUploadedFileCommand(
            parseDate("16/12/2024"),
            parseEuropeanAmount("0,00"),
            parseEuropeanAmount("34.838.994,00")),
        new AddMovementUploadedFileCommand(
            parseDate("31/01/2025"),
            parseEuropeanAmount("0,00"),
            parseEuropeanAmount("35.102.299,00")),
        new AddMovementUploadedFileCommand(
            parseDate("28/02/2025"),
            parseEuropeanAmount("0,00"),
            parseEuropeanAmount("35.341.920,00")),
        new AddMovementUploadedFileCommand(
            parseDate("31/03/2025"),
            parseEuropeanAmount("0,00"),
            parseEuropeanAmount("35.593.716,00")),
        new AddMovementUploadedFileCommand(
            parseDate("30/04/2025"),
            parseEuropeanAmount("0,00"),
            parseEuropeanAmount("35.829.015,00")),
        new AddMovementUploadedFileCommand(
            parseDate("31/05/2025"),
            parseEuropeanAmount("0,00"),
            parseEuropeanAmount("36.102.719,00")),
        new AddMovementUploadedFileCommand(
            parseDate("30/08/2025"),
            BigDecimal.ZERO, // No total amount visible in the image
            parseEuropeanAmount("37.074.883,00")));
  }

  public static List<AddMovementUploadedFileCommand> movementsForPIKMI() {
    return List.of(
        new AddMovementUploadedFileCommand(
            parseDate("31/07/2023"),
            parseEuropeanAmount("15.000.000,00"),
            parseEuropeanAmount("15.074.686,00")),
        new AddMovementUploadedFileCommand(
            parseDate("01/08/2023"),
            parseEuropeanAmount("30.000.000,00"),
            parseEuropeanAmount("45.194.686,00")),
        new AddMovementUploadedFileCommand(
            parseDate("02/08/2023"),
            parseEuropeanAmount("25.000.000,00"),
            parseEuropeanAmount("70.294.686,00")),
        new AddMovementUploadedFileCommand(
            parseDate("07/08/2023"),
            parseEuropeanAmount("-45.000.000,00"),
            parseEuropeanAmount("25.354.686,00")),
        new AddMovementUploadedFileCommand(
            parseDate("09/08/2023"),
            parseEuropeanAmount("-11.000.000,00"),
            parseEuropeanAmount("14.354.686,00")),
        new AddMovementUploadedFileCommand(
            parseDate("09/08/2023"),
            parseEuropeanAmount("10.857.000,00"),
            parseEuropeanAmount("25.211.686,00")),
        new AddMovementUploadedFileCommand(
            parseDate("10/08/2023"),
            parseEuropeanAmount("24.200.000,00"),
            parseEuropeanAmount("49.411.686,00")),
        new AddMovementUploadedFileCommand(
            parseDate("20/08/2023"),
            parseEuropeanAmount("-4.000.000,00"),
            parseEuropeanAmount("45.411.686,00")),
        new AddMovementUploadedFileCommand(
            parseDate("22/08/2023"),
            parseEuropeanAmount("-400.000,00"),
            parseEuropeanAmount("45.525.753,00")),
        new AddMovementUploadedFileCommand(
            parseDate("01/09/2023"),
            parseEuropeanAmount("-10.010.000,00"),
            parseEuropeanAmount("35.515.753,00")),
        new AddMovementUploadedFileCommand(
            parseDate("27/09/2023"),
            parseEuropeanAmount("35.349.382,00"),
            parseEuropeanAmount("71.073.909,00")),
        new AddMovementUploadedFileCommand(
            parseDate("29/09/2023"),
            parseEuropeanAmount("-16.221.968,00"),
            parseEuropeanAmount("54.787.054,00")),
        new AddMovementUploadedFileCommand(
            parseDate("30/09/2023"),
            parseEuropeanAmount("0,00"),
            parseEuropeanAmount("54.787.054,00")),
        new AddMovementUploadedFileCommand(
            parseDate("01/10/2023"),
            parseEuropeanAmount("-22.000.000,00"),
            parseEuropeanAmount("33.146.628,00")),
        new AddMovementUploadedFileCommand(
            parseDate("06/10/2023"),
            parseEuropeanAmount("9.900.000,00"),
            parseEuropeanAmount("43.086.228,00")),
        new AddMovementUploadedFileCommand(
            parseDate("30/10/2023"),
            parseEuropeanAmount("6.549.382,00"),
            parseEuropeanAmount("49.661.808,00")),
        new AddMovementUploadedFileCommand(
            parseDate("31/10/2023"),
            parseEuropeanAmount("0,00"),
            parseEuropeanAmount("50.069.218,00")),
        new AddMovementUploadedFileCommand(
            parseDate("01/11/2023"),
            parseEuropeanAmount("-22.000.000,00"),
            parseEuropeanAmount("28.069.218,00")),
        new AddMovementUploadedFileCommand(
            parseDate("07/11/2023"),
            parseEuropeanAmount("24.000.000,00"),
            parseEuropeanAmount("54.104.823,00")),
        new AddMovementUploadedFileCommand(
            parseDate("09/11/2023"),
            parseEuropeanAmount("-1.000.000,00"),
            parseEuropeanAmount("53.100.826,00")),
        new AddMovementUploadedFileCommand(
            parseDate("17/11/2023"),
            parseEuropeanAmount("-400.000,00"),
            parseEuropeanAmount("52.699.223,00")),
        new AddMovementUploadedFileCommand(
            parseDate("20/11/2023"),
            parseEuropeanAmount("-13.000.000,00"),
            parseEuropeanAmount("39.647.223,00")),
        new AddMovementUploadedFileCommand(
            parseDate("30/11/2023"),
            parseEuropeanAmount("671.155,00"),
            parseEuropeanAmount("40.726.167,00")),
        new AddMovementUploadedFileCommand(
            parseDate("06/12/2023"),
            parseEuropeanAmount("-14.500.000,00"),
            parseEuropeanAmount("26.227.544,00")),
        new AddMovementUploadedFileCommand(
            parseDate("14/12/2023"),
            parseEuropeanAmount("-400.000,00"),
            parseEuropeanAmount("25.825.944,00")),
        new AddMovementUploadedFileCommand(
            parseDate("31/12/2023"),
            parseEuropeanAmount("-2.346.355,00"),
            parseEuropeanAmount("23.720.010,00")),
        new AddMovementUploadedFileCommand(
            parseDate("01/01/2024"),
            parseEuropeanAmount("-13.000.000,00"),
            parseEuropeanAmount("10.720.010,00")),
        new AddMovementUploadedFileCommand(
            parseDate("12/01/2024"),
            parseEuropeanAmount("0,00"),
            parseEuropeanAmount("10.720.010,00")),
        new AddMovementUploadedFileCommand(
            parseDate("31/01/2024"),
            parseEuropeanAmount("653.045,00"),
            parseEuropeanAmount("11.480.093,00")),
        new AddMovementUploadedFileCommand(
            parseDate("04/02/2024"),
            parseEuropeanAmount("-10.000.000,00"),
            parseEuropeanAmount("1.440.093,00")),
        new AddMovementUploadedFileCommand(
            parseDate("29/02/2024"),
            parseEuropeanAmount("2.653.045,00"),
            parseEuropeanAmount("4.105.556,00")),
        new AddMovementUploadedFileCommand(
            parseDate("31/03/2024"),
            parseEuropeanAmount("653.645,00"),
            parseEuropeanAmount("4.839.561,00")),
        new AddMovementUploadedFileCommand(
            parseDate("30/04/2024"),
            parseEuropeanAmount("653.645,00"),
            parseEuropeanAmount("5.542.958,00")),
        new AddMovementUploadedFileCommand(
            parseDate("30/05/2024"),
            parseEuropeanAmount("2.401.286,00"),
            parseEuropeanAmount("8.016.341,00")),
        new AddMovementUploadedFileCommand(
            parseDate("30/06/2024"),
            parseEuropeanAmount("520.531,00"),
            parseEuropeanAmount("8.613.715,00")),
        new AddMovementUploadedFileCommand(
            parseDate("31/07/2024"),
            parseEuropeanAmount("520.531,00"),
            parseEuropeanAmount("9.136.328,00")),
        new AddMovementUploadedFileCommand(
            parseDate("31/08/2024"),
            parseEuropeanAmount("60.520.531,00"),
            parseEuropeanAmount("72.058.459,00")),
        new AddMovementUploadedFileCommand(
            parseDate("30/09/2024"),
            parseEuropeanAmount("520.531,00"),
            parseEuropeanAmount("73.257.353,00")),
        new AddMovementUploadedFileCommand(
            parseDate("31/10/2024"),
            parseEuropeanAmount("520.531,00"),
            parseEuropeanAmount("74.490.431,00")),
        new AddMovementUploadedFileCommand(
            parseDate("21/11/2024"),
            parseEuropeanAmount("520.531,00"),
            parseEuropeanAmount("75.711.997,00")),
        new AddMovementUploadedFileCommand(
            parseDate("31/12/2024"),
            parseEuropeanAmount("0,00"),
            parseEuropeanAmount("76.398.664,00")),
        new AddMovementUploadedFileCommand(
            parseDate("31/01/2025"),
            parseEuropeanAmount("0,00"),
            parseEuropeanAmount("76.976.067,00")),
        new AddMovementUploadedFileCommand(
            parseDate("25/02/2025"),
            parseEuropeanAmount("0,00"),
            parseEuropeanAmount("77.501.534,00")),
        new AddMovementUploadedFileCommand(
            parseDate("31/03/2025"),
            parseEuropeanAmount("0,00"),
            parseEuropeanAmount("78.053.700,00")),
        new AddMovementUploadedFileCommand(
            parseDate("30/04/2025"),
            parseEuropeanAmount("-10.000.000,00"),
            parseEuropeanAmount("68.053.700,00")),
        new AddMovementUploadedFileCommand(
            parseDate("30/08/2025"),
            parseEuropeanAmount("0,00"),
            parseEuropeanAmount("70.908.065,00")));
  }

  public static List<AddMonthlyBalanceCommand> getAddMonthlyBalanceCommandsWithProfit(
      final YearMonth initialPeriod, final BigDecimal initialBalance) {
    return List.of(
        createMonthlyBalanceCommand(
            initialPeriod, new MonthlyBalanceCommandVO(initialBalance, null)),
        createMonthlyBalanceCommand(initialPeriod.plusMonths(1), getCommandVO("1050", "50")),
        createMonthlyBalanceCommand(initialPeriod.plusMonths(2), getCommandVO("1200", "30")),
        createMonthlyBalanceCommand(initialPeriod.plusMonths(3), getCommandVO("1000", "50")),
        createMonthlyBalanceCommand(initialPeriod.plusMonths(4), getCommandVO("500", "100")),
        createMonthlyBalanceCommand(initialPeriod.plusMonths(5), getCommandVO("420", "20")),
        // 2025
        createMonthlyBalanceCommand(initialPeriod.plusMonths(6), getCommandVO("575", "10")),
        createMonthlyBalanceCommand(
            initialPeriod.plusMonths(7), getCommandVO("582", "5"), new BigDecimal("3"))
        // To separate the tests
        );
  }

  private static MonthlyBalanceCommandVO getCommandVO(final String balance, final String profit) {
    return new MonthlyBalanceCommandVO(
        withJBHDecimals(new BigDecimal(balance)), withJBHDecimals(new BigDecimal(profit)));
  }
}
