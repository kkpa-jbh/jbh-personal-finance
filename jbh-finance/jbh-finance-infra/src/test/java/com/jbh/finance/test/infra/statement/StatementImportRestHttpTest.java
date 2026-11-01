package com.jbh.finance.test.infra.statement;

import static com.jbh.finance.test.infra.statement.StatementImportTestSupport.COP;
import static com.jbh.finance.test.infra.statement.StatementImportTestSupport.DONE;
import static com.jbh.finance.test.infra.statement.StatementImportTestSupport.FAILED;
import static com.jbh.finance.test.infra.statement.StatementImportTestSupport.PROCESSING;
import static com.jbh.finance.test.infra.statement.StatementImportTestSupport.USD;
import static com.jbh.finance.test.infra.statement.StatementImportTestSupport.awaitFinished;
import static com.jbh.finance.test.infra.statement.StatementImportTestSupport.body;
import static com.jbh.finance.test.infra.statement.StatementImportTestSupport.cannedStatement;
import static com.jbh.finance.test.infra.statement.StatementImportTestSupport.count;
import static com.jbh.finance.test.infra.statement.StatementImportTestSupport.createProduct;
import static com.jbh.finance.test.infra.statement.StatementImportTestSupport.currentBalance;
import static com.jbh.finance.test.infra.statement.StatementImportTestSupport.decimal;
import static com.jbh.finance.test.infra.statement.StatementImportTestSupport.getImport;
import static com.jbh.finance.test.infra.statement.StatementImportTestSupport.newPdf;
import static com.jbh.finance.test.infra.statement.StatementImportTestSupport.postImport;
import static com.jbh.finance.test.infra.statement.StatementImportTestSupport.singleRowStatement;
import static com.jbh.finance.test.infra.statement.StatementImportTestSupport.string;
import static com.jbh.finance.test.infra.statement.StatementImportTestSupport.update;
import static com.jbh.finance.test.testfixtures.fakes.gateway.FakeGatewayClientFactory.USER_A_ID;
import static com.jbh.finance.test.testfixtures.fakes.gateway.FakeGatewayClientFactory.USER_A_TOKEN;
import static com.jbh.finance.test.testfixtures.fakes.gateway.FakeGatewayClientFactory.USER_B_TOKEN;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.acid.UnitOfWork;
import com.jbh.finance.application.feature.category.ports.output.CategoryQueryRepo;
import com.jbh.finance.application.feature.category.services.CategoryService;
import com.jbh.finance.application.feature.statement.dto.StatementMovementDirection;
import com.jbh.finance.application.feature.statement.vo.PdfSha256;
import com.jbh.finance.application.shared.exceptions.BusinessApplicationExceptionType;
import com.jbh.finance.infra.adapters.exceptions.FinanceInfraBusinessExceptionType;
import com.jbh.finance.test.infra.movement.FinanceDatabaseTestProfile;
import com.jbh.finance.test.infra.statement.StatementImportTestSupport.Categories;
import com.jbh.finance.test.testfixtures.fakes.statement.FakeExtractStatementMovementsUseCase;
import io.quarkus.agroal.DataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.response.Response;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Calls the statement import API over HTTP on a real PostgreSQL. The statement reader and the
 * classifier are replaced by {@link FakeExtractStatementMovementsUseCase}, and IAM by {@code
 * FakeGatewayClientFactory}, so nothing here calls an AI provider. The expected values come from
 * the agreed API behavior, not from the implementation.
 */
@QuarkusTest
@TestProfile(FinanceDatabaseTestProfile.class)
@Tag("database")
class StatementImportRestHttpTest {

  private static final String SAVINGS = "SAVINGS";
  private static final String CREDIT_CARD = "CREDIT_CARD";
  private static final String CDT = "CDT";
  private static final int ACCEPTED = 202;
  private static final int NOT_FOUND = 404;
  private static final int CONFLICT = 409;
  private static final int UNPROCESSABLE = 422;
  private static final String OLD_EXTRACT_ENDPOINT = "/jbh-api/finance/statements/extract";
  private static final String TIMED_OUT = BusinessApplicationExceptionType.STATEMENT_IMPORT_TIMED_OUT.name();
  private static final String INSERT_PROCESSING_SQL =
      "INSERT INTO finance.statement_imports"
          + " (id, user_id, product_id, pdf_sha256, status, started_at)"
          + " VALUES (?, ?, ?, ?, 'PROCESSING', now() - ?::interval)";
  private static final String STATUS_SQL = "SELECT status FROM finance.statement_imports WHERE id = ?";
  private static final String ERROR_SQL = "SELECT error_code FROM finance.statement_imports WHERE id = ?";
  private static final String ROWS_OF_PRODUCT_SQL =
      "SELECT count(*) FROM finance.statement_imports WHERE product_id = ?";
  private static final String MOVEMENTS_OF_IMPORT_SQL =
      "SELECT count(*) FROM finance.movements WHERE statement_import_id = ?";
  private static final String NOT_SAVED_MOVEMENTS_SQL =
      "SELECT count(*) FROM finance.movements WHERE statement_import_id = ?"
          + " AND description IN ('AMAZON', 'FUTURE')";

  @Inject FakeExtractStatementMovementsUseCase fakeReader;
  @Inject UnitOfWork unitOfWork;
  @Inject CategoryQueryRepo categoryRepo;
  @Inject Instance<CategoryService> categoryServices;

  @Inject
  @DataSource("finance")
  javax.sql.DataSource dataSource;

  private Categories categories;

  @BeforeEach
  void programTheFakeReader() throws BusinessException {
    categories = StatementImportTestSupport.loadCategories(unitOfWork, categoryRepo, categoryServices);
    fakeReader.returning(cannedStatement(categories));
  }

  // Happy path: the main-currency rows are saved in date order and the product balance changes.
  @Test
  void shouldSaveTheMainCurrencyMovementsSortedByDateWhenAPdfIsImported() throws SQLException {
    final String productId = createProduct(USER_A_TOKEN, SAVINGS, Map.of());

    final Response accepted = postImport(USER_A_TOKEN, productId, newPdf());

    accepted.then().statusCode(ACCEPTED);
    final JsonNode acceptedBody = body(accepted);
    assertNotNull(acceptedBody.get("importId"));
    assertEquals(PROCESSING, acceptedBody.get("status").asText());
    final String importId = acceptedBody.get("importId").asText();

    final JsonNode result = awaitFinished(USER_A_TOKEN, productId, importId);

    assertEquals(DONE, result.get("status").asText());
    final JsonNode movements = result.get("movements");
    assertEquals(3, movements.size());
    assertEquals("NOMINA", movements.get(0).get("description").asText());
    assertEquals("XYZ", movements.get(1).get("description").asText());
    assertEquals("TIENDA", movements.get(2).get("description").asText());

    final JsonNode salary = movements.get(0);
    assertEquals("CREDIT", salary.get("direction").asText());
    assertMoney("1000.00", salary.get("amount"));
    assertEquals("INC_SALARY", salary.get("category").get("alias").asText());

    final JsonNode unknown = movements.get(1);
    assertEquals("DEBIT", unknown.get("direction").asText());
    assertMoney("20.00", unknown.get("amount"));
    assertEquals(categories.unknownExpense().getAlias(), unknown.get("category").get("alias").asText());
    assertTrue(unknown.get("needsReview").asBoolean());
    assertMoney("0.4", unknown.get("confidence"));

    final JsonNode summary = result.get("summary");
    assertEquals(COP, summary.get("mainCurrency").asText());
    assertEquals(3, summary.get("savedCount").asInt());
    assertEquals(2, summary.get("skippedCount").asInt());
    assertEquals(1, summary.get("needsReviewCount").asInt());
    final JsonNode notSaved = summary.get("notSavedMovements");
    assertEquals(2, notSaved.size());
    assertEquals("AMAZON", notSaved.get(0).get("description").asText());
    assertEquals(USD, notSaved.get(0).get("currency").asText());
    assertEquals("OTHER_CURRENCY", notSaved.get(0).get("reason").asText());
    assertEquals("FUTURE", notSaved.get(1).get("description").asText());
    assertEquals(COP, notSaved.get(1).get("currency").asText());
    assertEquals("FUTURE_DATE", notSaved.get(1).get("reason").asText());

    assertMoney("930.00", currentBalance(USER_A_TOKEN, productId));
    final UUID importUuid = UUID.fromString(importId);
    assertEquals(3, count(dataSource, MOVEMENTS_OF_IMPORT_SQL, importUuid));
    assertEquals(0, count(dataSource, NOT_SAVED_MOVEMENTS_SQL, importUuid));
  }

  // Duplicate PDF: only the same PDF in the same product is refused.
  @Test
  void shouldAnswerConflictOnlyForTheSamePdfInTheSameProduct() {
    final String firstProduct = createProduct(USER_A_TOKEN, SAVINGS, Map.of());
    final String secondProduct = createProduct(USER_A_TOKEN, SAVINGS, Map.of());
    final byte[] pdfOne = newPdf();
    final byte[] pdfTwo = newPdf();
    final Response first = postImport(USER_A_TOKEN, firstProduct, pdfOne);
    first.then().statusCode(ACCEPTED);
    awaitFinished(USER_A_TOKEN, firstProduct, body(first).get("importId").asText());

    postImport(USER_A_TOKEN, firstProduct, pdfOne)
        .then()
        .statusCode(CONFLICT)
        .body("errors", hasItem(BusinessApplicationExceptionType.STATEMENT_ALREADY_IMPORTED.getMessage()));

    final Response otherProduct = postImport(USER_A_TOKEN, secondProduct, pdfOne);
    final Response otherPdf = postImport(USER_A_TOKEN, firstProduct, pdfTwo);
    otherProduct.then().statusCode(ACCEPTED);
    otherPdf.then().statusCode(ACCEPTED);
    awaitFinished(USER_A_TOKEN, secondProduct, body(otherProduct).get("importId").asText());
    awaitFinished(USER_A_TOKEN, firstProduct, body(otherPdf).get("importId").asText());
  }

  // Admission: wrong product type or another user's product is refused and stores nothing.
  @Test
  void shouldRejectAProductTypeThatIsNotAllowedAndAProductOfAnotherUser() throws SQLException {
    final String cdtProduct = createProduct(USER_A_TOKEN, CDT, Map.of());
    final String foreignProduct = createProduct(USER_B_TOKEN, SAVINGS, Map.of());

    postImport(USER_A_TOKEN, cdtProduct, newPdf())
        .then()
        .statusCode(UNPROCESSABLE)
        .body("errors", hasItem(BusinessApplicationExceptionType.STATEMENT_PRODUCT_TYPE_NOT_ALLOWED.getMessage()));
    postImport(USER_A_TOKEN, foreignProduct, newPdf())
        .then()
        .statusCode(UNPROCESSABLE)
        .body("errors", hasItem(BusinessApplicationExceptionType.PRODUCT_NOT_FOUND.getMessage()));

    assertEquals(0, count(dataSource, ROWS_OF_PRODUCT_SQL, UUID.fromString(cdtProduct)));
    assertEquals(0, count(dataSource, ROWS_OF_PRODUCT_SQL, UUID.fromString(foreignProduct)));
  }

  // A failed import can be retried with the same PDF.
  @Test
  void shouldMarkTheImportFailedAndAllowARetryWhenTheReaderFails() {
    final String productId = createProduct(USER_A_TOKEN, SAVINGS, Map.of());
    final byte[] pdf = newPdf();
    fakeReader.failingWith(FinanceInfraBusinessExceptionType.STATEMENT_READER_FAILED);

    final Response first = postImport(USER_A_TOKEN, productId, pdf);
    first.then().statusCode(ACCEPTED);
    final String firstId = body(first).get("importId").asText();
    final JsonNode failed = awaitFinished(USER_A_TOKEN, productId, firstId);

    assertEquals(FAILED, failed.get("status").asText());
    assertEquals(
        FinanceInfraBusinessExceptionType.STATEMENT_READER_FAILED.name(),
        failed.get("errorCode").asText());
    assertEquals(0, failed.get("movements").size());
    assertTrue(isNullOrAbsent(failed, "summary"));

    fakeReader.returning(cannedStatement(categories));
    final Response retry = postImport(USER_A_TOKEN, productId, pdf);
    retry.then().statusCode(ACCEPTED);
    final String retryId = body(retry).get("importId").asText();
    assertNotEquals(firstId, retryId);
    assertEquals(DONE, awaitFinished(USER_A_TOKEN, productId, retryId).get("status").asText());
    assertEquals(FAILED, awaitFinished(USER_A_TOKEN, productId, firstId).get("status").asText());
  }

  // A stale PROCESSING row counts as failed and is freed by a new upload.
  @Test
  void shouldTreatAStaleProcessingImportAsFailedAndFreeItForANewUpload() throws SQLException {
    final String productId = createProduct(USER_A_TOKEN, SAVINGS, Map.of());
    final byte[] stalePdf = newPdf();
    final UUID staleId = insertProcessingRow(productId, stalePdf, "11 minutes");

    final JsonNode polled = body(getImport(USER_A_TOKEN, productId, staleId.toString()));

    assertEquals(FAILED, polled.get("status").asText());
    assertEquals(TIMED_OUT, polled.get("errorCode").asText());
    assertEquals(PROCESSING, string(dataSource, STATUS_SQL, staleId));

    final Response retry = postImport(USER_A_TOKEN, productId, stalePdf);
    retry.then().statusCode(ACCEPTED);
    final String newId = body(retry).get("importId").asText();
    assertEquals(FAILED, string(dataSource, STATUS_SQL, staleId));
    assertEquals(TIMED_OUT, string(dataSource, ERROR_SQL, staleId));
    assertNotEquals(staleId.toString(), newId);
    assertEquals(DONE, awaitFinished(USER_A_TOKEN, productId, newId).get("status").asText());

    final byte[] freshPdf = newPdf();
    insertProcessingRow(productId, freshPdf, "2 minutes");
    postImport(USER_A_TOKEN, productId, freshPdf)
        .then()
        .statusCode(CONFLICT)
        .body("errors", hasItem(BusinessApplicationExceptionType.STATEMENT_ALREADY_IMPORTED.getMessage()));
  }

  // The funds check is off for statement imports.
  @Test
  void shouldSaveADebitOverTheCreditLimitWhenTheStatementIsTheTruth() {
    final String productId =
        createProduct(USER_A_TOKEN, CREDIT_CARD, Map.of("CREDIT_LIMIT", 100, "PAYMENT_DUE_DAY", 15));
    fakeReader.returning(
        singleRowStatement(StatementMovementDirection.DEBIT, "500.00", categories.grocery()));

    final Response accepted = postImport(USER_A_TOKEN, productId, newPdf());
    accepted.then().statusCode(ACCEPTED);
    final JsonNode result = awaitFinished(USER_A_TOKEN, productId, body(accepted).get("importId").asText());

    assertEquals(DONE, result.get("status").asText());
    assertMoney("-500.00", currentBalance(USER_A_TOKEN, productId));
  }

  // The poll is hidden from other users and from other products.
  @Test
  void shouldHideAnImportFromOtherUsersAndFromOtherProducts() {
    final String productId = createProduct(USER_A_TOKEN, SAVINGS, Map.of());
    final String otherProductId = createProduct(USER_A_TOKEN, SAVINGS, Map.of());
    final Response accepted = postImport(USER_A_TOKEN, productId, newPdf());
    accepted.then().statusCode(ACCEPTED);
    final String importId = body(accepted).get("importId").asText();
    awaitFinished(USER_A_TOKEN, productId, importId);

    getImport(USER_B_TOKEN, productId, importId)
        .then()
        .statusCode(UNPROCESSABLE)
        .body("errors", hasItem(BusinessApplicationExceptionType.PRODUCT_NOT_FOUND.getMessage()));
    getImport(USER_A_TOKEN, productId, UUID.randomUUID().toString())
        .then()
        .statusCode(UNPROCESSABLE)
        .body("errors", hasItem(BusinessApplicationExceptionType.STATEMENT_IMPORT_NOT_FOUND.getMessage()));
    getImport(USER_A_TOKEN, otherProductId, importId)
        .then()
        .statusCode(UNPROCESSABLE)
        .body("errors", hasItem(BusinessApplicationExceptionType.STATEMENT_IMPORT_NOT_FOUND.getMessage()));
  }

  // The old extract endpoint is gone.
  @Test
  void shouldAnswerNotFoundWhenTheOldExtractEndpointIsCalled() {
    given()
        .header(HttpHeaders.AUTHORIZATION, USER_A_TOKEN)
        .multiPart("file", "statement.pdf", newPdf(), "application/pdf")
        .when()
        .post(OLD_EXTRACT_ENDPOINT)
        .then()
        .statusCode(NOT_FOUND);
  }

  private UUID insertProcessingRow(final String productId, final byte[] pdf, final String age)
      throws SQLException {
    final UUID importId = UUID.randomUUID();
    update(
        dataSource,
        INSERT_PROCESSING_SQL,
        importId,
        USER_A_ID,
        UUID.fromString(productId),
        PdfSha256.of(pdf).value(),
        age);
    return importId;
  }

  private static boolean isNullOrAbsent(final JsonNode node, final String field) {
    return node.get(field) == null || node.get(field).isNull();
  }

  private static void assertMoney(final String expected, final JsonNode actual) {
    assertMoney(expected, decimal(actual));
  }

  private static void assertMoney(final String expected, final BigDecimal actual) {
    assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " but was " + actual);
  }
}
