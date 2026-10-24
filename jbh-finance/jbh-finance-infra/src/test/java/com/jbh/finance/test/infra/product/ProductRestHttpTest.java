package com.jbh.finance.test.infra.product;

import static com.jbh.finance.test.testfixtures.fakes.gateway.FakeGatewayClientFactory.USER_A_ID;
import static com.jbh.finance.test.testfixtures.fakes.gateway.FakeGatewayClientFactory.USER_A_TOKEN;
import static com.jbh.finance.test.testfixtures.fakes.gateway.FakeGatewayClientFactory.USER_B_TOKEN;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import com.jbh.finance.infra.adapters.in.rest.common.FinanceApiRoutes;
import com.jbh.finance.test.infra.movement.FinanceDatabaseTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import jakarta.ws.rs.core.HttpHeaders;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Calls the product API over HTTP on a real PostgreSQL. IAM is replaced by {@code
 * FakeGatewayClientFactory}, so the tests can act as two users and check that one user never sees
 * the products of the other.
 */
@QuarkusTest
@TestProfile(FinanceDatabaseTestProfile.class)
@Tag("database")
class ProductRestHttpTest {

  private static final String PRODUCTS = FinanceApiRoutes.PRODUCTS_API_PATH;
  private static final String PRODUCT_BY_ID = PRODUCTS + "/{productId}";
  private static final String PRODUCT_NAME = "http-test-savings";
  private static final int OK = 200;
  private static final int UNAUTHORIZED = 401;
  private static final int NOT_FOUND = 404;

  @Test
  void shouldCreateAProductAndReadItBackWhenTheOwnerAsks() {
    final String productId = createProductAs(USER_A_TOKEN);

    given()
        .header(HttpHeaders.AUTHORIZATION, USER_A_TOKEN)
        .when()
        .get(PRODUCT_BY_ID, productId)
        .then()
        .statusCode(OK)
        .body("id.value", equalTo(productId))
        .body("name", equalTo(PRODUCT_NAME))
        .body("type", equalTo("SAVINGS"))
        .body("userId", equalTo(USER_A_ID.toString()));
  }

  @Test
  void shouldAnswerNotFoundWhenAnotherUserAsksForTheProduct() {
    final String productId = createProductAs(USER_A_TOKEN);

    given()
        .header(HttpHeaders.AUTHORIZATION, USER_B_TOKEN)
        .when()
        .get(PRODUCT_BY_ID, productId)
        .then()
        .statusCode(NOT_FOUND);
  }

  @Test
  void shouldNotListTheProductsOfAnotherUser() {
    final String productId = createProductAs(USER_A_TOKEN);

    given()
        .header(HttpHeaders.AUTHORIZATION, USER_B_TOKEN)
        .when()
        .get(PRODUCTS)
        .then()
        .statusCode(OK)
        .body("id.value", not(hasItem(productId)));
  }

  @Test
  void shouldAnswerUnauthorizedWhenTheTokenIsMissing() {
    given().when().get(PRODUCTS).then().statusCode(UNAUTHORIZED);
  }

  @Test
  void shouldAnswerUnauthorizedWhenIamDoesNotKnowTheToken() {
    given()
        .header(HttpHeaders.AUTHORIZATION, "Bearer unknown")
        .when()
        .get(PRODUCTS)
        .then()
        .statusCode(UNAUTHORIZED);
  }

  private static String createProductAs(final String token) {
    return given()
        .header(HttpHeaders.AUTHORIZATION, token)
        .contentType(ContentType.JSON)
        .body(Map.of("name", PRODUCT_NAME, "type", "SAVINGS", "metadata", Map.of()))
        .when()
        .post(PRODUCTS)
        .then()
        .statusCode(OK)
        .extract()
        .path("id.value");
  }
}
