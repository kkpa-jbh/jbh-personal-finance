package com.jbh.finance.test.infra.statement;

import static io.restassured.RestAssured.given;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.acid.UnitOfWork;
import com.jbh.finance.application.feature.category.dto.CategoryDTO;
import com.jbh.finance.application.feature.category.ports.output.CategoryQueryRepo;
import com.jbh.finance.application.feature.category.services.CategoryService;
import com.jbh.finance.application.feature.statement.dto.CategorizedMovementDTO;
import com.jbh.finance.application.feature.statement.dto.StatementBalanceDTO;
import com.jbh.finance.application.feature.statement.dto.StatementClassificationDTO;
import com.jbh.finance.application.feature.statement.dto.StatementMovementDirection;
import com.jbh.finance.application.feature.statement.dto.TotalsCheckDTO;
import com.jbh.finance.application.feature.statement.dto.TotalsCheckStatus;
import com.jbh.finance.domain.category.vo.CategorySourceVO;
import com.jbh.finance.domain.category.vo.CategoryTypeVO;
import com.jbh.finance.infra.adapters.in.rest.common.FinanceApiRoutes;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.core.HttpHeaders;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

/**
 * Helpers shared by the statement import integration tests: the HTTP calls, the canned statement
 * the fake reader returns, a bounded wait for the background job, and small SQL reads. Expected
 * values live in the tests, never here.
 */
final class StatementImportTestSupport {

  static final ObjectMapper JSON = new ObjectMapper();
  static final String PRODUCTS = FinanceApiRoutes.PRODUCTS_API_PATH;
  static final String IMPORTS = FinanceApiRoutes.STATEMENT_IMPORTS_API_PATH;
  static final String IMPORT_BY_ID = IMPORTS + FinanceApiRoutes.STATEMENT_IMPORT_ID_PATH;
  static final String PROCESSING = "PROCESSING";
  static final String DONE = "DONE";
  static final String FAILED = "FAILED";
  static final String COP = "COP";
  static final String USD = "USD";

  static final LocalDate SEPTEMBER_1 = LocalDate.of(2026, 9, 1);
  static final LocalDate SEPTEMBER_2 = LocalDate.of(2026, 9, 2);
  static final LocalDate SEPTEMBER_3 = LocalDate.of(2026, 9, 3);
  static final LocalDate FAR_FUTURE = LocalDate.of(2099, 1, 1);

  private static final Duration WAIT_LIMIT = Duration.ofSeconds(20);
  private static final long POLL_INTERVAL_MILLIS = 100;

  private StatementImportTestSupport() {
    throw new AssertionError("Utility class - do not instantiate");
  }

  /** The categories the canned statement uses. */
  record Categories(
      CategoryDTO grocery, CategoryDTO salary, CategoryDTO unknownExpense, CategoryDTO shopping) {

    /** Shopping is only used by a row that is never saved, so it needs no database row. */
    static CategoryDTO notSavedShopping() {
      return CategoryDTO.withEntity(
          new CategoryTypeVO(CategorySourceVO.EXPENSE, "EXP_SHOPPING"),
          0L,
          Map.of("en", "Shopping", "es", "Compras"),
          true,
          Map.of());
    }
  }

  // ---------------------------------------------------------------- canned statements

  /** Loads the real categories from the test database, inside a transaction. */
  static Categories loadCategories(
      final UnitOfWork unitOfWork,
      final CategoryQueryRepo categoryRepo,
      final Instance<CategoryService> categoryServices)
      throws BusinessException {
    return unitOfWork.executeInTransaction(
        () ->
            new Categories(
                findByAlias(categoryRepo.findActiveBySource(CategorySourceVO.EXPENSE), "EXP_GROCERY"),
                findByAlias(categoryRepo.findActiveBySource(CategorySourceVO.INCOME), "INC_SALARY"),
                categoryServices.get().findExpenseUnknown(),
                Categories.notSavedShopping()));
  }

  private static CategoryDTO findByAlias(final List<CategoryDTO> categories, final String alias) {
    return categories.stream()
        .filter(category -> alias.equals(category.getAlias()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Seed category missing: " + alias));
  }

  /** Canned classification C1 of the test contract, in PDF order. */
  static StatementClassificationDTO cannedStatement(final Categories categories) {
    return statement(
        List.of(
            row(SEPTEMBER_3, "TIENDA", "50.00", COP, StatementMovementDirection.DEBIT,
                categories.grocery(), "0.9", false),
            row(SEPTEMBER_1, "NOMINA", "1000.00", COP, StatementMovementDirection.CREDIT,
                categories.salary(), "0.95", false),
            row(SEPTEMBER_2, "XYZ", "20.00", COP, StatementMovementDirection.DEBIT,
                categories.unknownExpense(), "0.4", true),
            row(SEPTEMBER_2, "AMAZON", "5.00", USD, StatementMovementDirection.DEBIT,
                categories.shopping(), "0.9", false),
            row(FAR_FUTURE, "FUTURE", "9.00", COP, StatementMovementDirection.DEBIT,
                categories.grocery(), "0.9", false)));
  }

  /** A statement with one COP row. */
  static StatementClassificationDTO singleRowStatement(
      final StatementMovementDirection direction,
      final String amount,
      final CategoryDTO category) {
    return statement(List.of(row(SEPTEMBER_1, "ONE ROW", amount, COP, direction, category, "0.9", false)));
  }

  private static StatementClassificationDTO statement(final List<CategorizedMovementDTO> rows) {
    return new StatementClassificationDTO(
        rows,
        List.of(
            new TotalsCheckDTO(
                COP, TotalsCheckStatus.MATCH, new BigDecimal("930.00"), new BigDecimal("930.00"))),
        List.of(new StatementBalanceDTO(COP, BigDecimal.ZERO, new BigDecimal("930.00"))));
  }

  private static CategorizedMovementDTO row(
      final LocalDate date,
      final String description,
      final String amount,
      final String currency,
      final StatementMovementDirection direction,
      final CategoryDTO category,
      final String confidence,
      final boolean lowConfidence) {
    final Map<String, String> displayName =
        category.getDisplayName() == null ? Map.of() : category.getDisplayName();
    return new CategorizedMovementDTO(
        date,
        description,
        new BigDecimal(amount),
        currency,
        direction,
        category.getAlias(),
        displayName,
        new BigDecimal(confidence),
        lowConfidence,
        category);
  }

  // ---------------------------------------------------------------- HTTP

  /** A small valid PDF. The title makes every call a different file, so a different SHA-256. */
  static byte[] newPdf() {
    try (PDDocument document = new PDDocument();
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      document.addPage(new PDPage());
      document.getDocumentInformation().setTitle(UUID.randomUUID().toString());
      document.save(output);
      return output.toByteArray();
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  static String createProduct(
      final String token, final String type, final Map<String, Object> metadata) {
    return given()
        .header(HttpHeaders.AUTHORIZATION, token)
        .contentType(ContentType.JSON)
        .body(Map.of("name", "statement-it-" + type, "type", type, "metadata", metadata))
        .when()
        .post(PRODUCTS)
        .then()
        .statusCode(200)
        .extract()
        .path("id.value");
  }

  static Response postImport(final String token, final String productId, final byte[] pdf) {
    return given()
        .header(HttpHeaders.AUTHORIZATION, token)
        .multiPart("file", "statement.pdf", pdf, "application/pdf")
        .when()
        .post(IMPORTS, productId);
  }

  static Response getImport(final String token, final String productId, final String importId) {
    return given()
        .header(HttpHeaders.AUTHORIZATION, token)
        .when()
        .get(IMPORT_BY_ID, productId, importId);
  }

  static JsonNode body(final Response response) {
    try {
      return JSON.readTree(response.asString());
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Reads the product current balance. */
  static BigDecimal currentBalance(final String token, final String productId) {
    final Response response =
        given()
            .header(HttpHeaders.AUTHORIZATION, token)
            .when()
            .get(PRODUCTS + "/{productId}", productId);
    return decimal(body(response).get("currentBalance"));
  }

  /** Money comes back as a JSON number: read it as a decimal and compare with compareTo. */
  static BigDecimal decimal(final JsonNode number) {
    return new BigDecimal(number.asText());
  }

  /** Polls the GET until the status is not PROCESSING, up to a bounded time. */
  static JsonNode awaitFinished(
      final String token, final String productId, final String importId) {
    final long deadline = System.nanoTime() + WAIT_LIMIT.toNanos();
    JsonNode last = null;
    while (System.nanoTime() < deadline) {
      final Response response = getImport(token, productId, importId);
      response.then().statusCode(200);
      last = body(response);
      if (!PROCESSING.equals(last.get("status").asText())) {
        return last;
      }
      pause();
    }
    throw new AssertionError("The import did not finish in " + WAIT_LIMIT + ". Last answer: " + last);
  }

  private static void pause() {
    try {
      Thread.sleep(POLL_INTERVAL_MILLIS);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new AssertionError("Interrupted while waiting for the import", e);
    }
  }

  // ---------------------------------------------------------------- SQL

  static long count(final DataSource dataSource, final String sql, final Object... params)
      throws SQLException {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      bind(statement, params);
      try (ResultSet rows = statement.executeQuery()) {
        rows.next();
        return rows.getLong(1);
      }
    }
  }

  static String string(final DataSource dataSource, final String sql, final Object... params)
      throws SQLException {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      bind(statement, params);
      try (ResultSet rows = statement.executeQuery()) {
        return rows.next() ? rows.getString(1) : null;
      }
    }
  }

  static int update(final DataSource dataSource, final String sql, final Object... params)
      throws SQLException {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      bind(statement, params);
      return statement.executeUpdate();
    }
  }

  private static void bind(final PreparedStatement statement, final Object... params)
      throws SQLException {
    for (int index = 0; index < params.length; index++) {
      statement.setObject(index + 1, params[index]);
    }
  }
}
