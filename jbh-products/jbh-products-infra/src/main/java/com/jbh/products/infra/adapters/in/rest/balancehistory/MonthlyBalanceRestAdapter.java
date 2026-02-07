package com.jbh.products.infra.adapters.in.rest.balancehistory;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.InternalSystemException;
import com.jbh.gateway.client.JbhGatewayException;
import com.jbh.products.application.core.dto.MonthlyBalanceDTO;
import com.jbh.products.application.core.dto.balancehistory.BalanceHistoryResponse;
import com.jbh.products.application.core.usecases.FindMonthlyBalanceUseCase;
import com.jbh.products.application.core.usecases.RegisterMonthlyBalanceUseCase;
import com.jbh.products.application.core.vo.commands.AddMonthlyBalanceCommand;
import com.jbh.products.domain.product.vo.ProductId;
import com.jbh.products.domain.product.vo.ProductPK;
import com.jbh.products.infra.adapters.in.rest.common.BaseRestAdapter;
import com.jbh.products.infra.adapters.in.rest.balancehistory.response.MonthlyBalanceResponse;
import com.jbh.products.infra.adapters.in.rest.common.FinanceApiRoutes;
import com.jbh.products.infra.adapters.in.rest.balancehistory.request.MonthlyBalanceRequest;
import com.jbh.products.infra.adapters.in.rest.balancehistory.request.RegisterMonthlyBalanceRequest;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({
  "PMD.UnnecessaryAnnotationValueElement",
  "PMD.CallSuperInConstructor",
  "PMD.AvoidDuplicateLiterals"
})
@RequestScoped
@Path(FinanceApiRoutes.PRODUCTS_API_PATH + FinanceApiRoutes.PRODUCTS_MONTHLY_BALANCES_API_PATH)
@Tag(
    name = "Balance History Operations",
    description =
        "The Balance History page allows users to analyze their financial performance over time."
            + " Users can view monthly balance trends for individual products or across all products, "
            + "helping them understand how their investments, savings, and other financial instruments are performing.")
public class MonthlyBalanceRestAdapter extends BaseRestAdapter {

  private final Logger log = LoggerFactory.getLogger(MonthlyBalanceRestAdapter.class);

  private final FindMonthlyBalanceUseCase findMonthlyBalanceUseCase;
  private final RegisterMonthlyBalanceUseCase registerMonthlyBalanceUseCase;

  @Inject
  public MonthlyBalanceRestAdapter(
      final FindMonthlyBalanceUseCase findMonthlyBalanceUseCase,
      final RegisterMonthlyBalanceUseCase registerMonthlyBalanceUseCase) {
    this.findMonthlyBalanceUseCase = findMonthlyBalanceUseCase;
    this.registerMonthlyBalanceUseCase = registerMonthlyBalanceUseCase;
  }

  @POST
  @Path("/")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Find monthly balances for all active products",
      description = "Retrieves monthly balances for all active products within a date range")
  @APIResponses(
      value = {
        @APIResponse(
            responseCode = "200",
            description = "Monthly Balances found for all active products",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = Map.class))),
        @APIResponse(
            responseCode = "400",
            description = "Invalid request",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = String.class))),
        @APIResponse(
            responseCode = "401",
            description = "Unauthorized",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = String.class)))
      })
  @SecurityRequirement(name = "JWT")
  public Response findBalanceHistoryForUserActiveProducts(
      @RequestBody final MonthlyBalanceRequest request,
      @HeaderParam("Authorization") @Parameter(description = "JWT Bearer token", required = true)
          final String authorizationHeader)
      throws JbhGatewayException, BusinessException, InternalSystemException {

    log.info("Getting Monthly Balances for all active products: {}", request);
    request.validate();

    final UUID userId = findUserId(authorizationHeader);

    final BalanceHistoryResponse balanceHistoryResponse =
        findMonthlyBalanceUseCase.findBalanceHistoryByUser(
            userId, request.startPeriod(), request.endPeriod(), YearMonth.now());

    return Response.ok(balanceHistoryResponse).build();
  }

  @POST
  @Path("/{productId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Find monthly balances for an Product")
  @APIResponses(
      value = {
        @APIResponse(
            responseCode = "200",
            description = "Monthly Balances found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = String.class))),
        @APIResponse(
            responseCode = "400",
            description = "Invalid command",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = String.class))),
        @APIResponse(
            responseCode = "401",
            description = "Unauthorized",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = String.class)))
      })
  @SecurityRequirement(name = "JWT")
  public Response findBalanceHistoryByProductId(
      @PathParam("productId") final UUID productId,
      @RequestBody final MonthlyBalanceRequest request,
      @HeaderParam("Authorization") @Parameter(description = "JWT Bearer token", required = true)
          final String authorizationHeader)
      throws JbhGatewayException, BusinessException, InternalSystemException {

    log.info("Getting Monthly Balances {} for product {} ", request, productId);
    request.validate();

    final UUID userId = findUserId(authorizationHeader);

    final BalanceHistoryResponse balanceHistoryByProduct =
        findMonthlyBalanceUseCase.findBalanceHistoryByProduct(
            new ProductPK(userId, ProductId.of(productId)),
            request.startPeriod(),
            request.endPeriod(),
            YearMonth.now());

    return Response.ok(balanceHistoryByProduct).build();
  }

  @POST
  @Path("/{productId}/register")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Register official monthly balance",
      description = "Register official monthly balance report with closing balance and profit")
  @APIResponses(
      value = {
        @APIResponse(
            responseCode = "200",
            description = "Monthly balance registered successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = MonthlyBalanceResponse.class))),
        @APIResponse(
            responseCode = "400",
            description = "Invalid request",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = String.class))),
        @APIResponse(
            responseCode = "401",
            description = "Unauthorized",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = String.class)))
      })
  @SecurityRequirement(name = "JWT")
  public Response registerOfficialMonthlyBalance(
      @PathParam("productId") final UUID productId,
      @RequestBody final RegisterMonthlyBalanceRequest request,
      @HeaderParam("Authorization") @Parameter(description = "JWT Bearer token", required = true)
          final String authorizationHeader)
      throws JbhGatewayException, BusinessException, InternalSystemException {

    final UUID userId = findUserId(authorizationHeader);

    log.info(
        "Registering official monthly balance for product {} period {}",
        productId,
        request.monthlyPeriod());

    final AddMonthlyBalanceCommand command =
        new AddMonthlyBalanceCommand(
            request.monthlyPeriod(),
            request.closingBalance(),
            request.monthlyProfitReported(),
            request.incomeWithholdingTaxAmount());

    final MonthlyBalanceDTO result =
        registerMonthlyBalanceUseCase.registerOfficialMonthlyBalance(
            LocalDate.now(), userId, ProductId.of(productId), command);

    return Response.ok(MonthlyBalanceResponse.fromDTO(result)).build();
  }
}
