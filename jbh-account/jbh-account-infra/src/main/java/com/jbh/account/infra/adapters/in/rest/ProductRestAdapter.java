package com.jbh.account.infra.adapters.in.rest;

import com.jbh.account.application.core.dto.AddBasicMovementDTO;
import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.application.core.dto.ProductDTO;
import com.jbh.account.application.core.usecases.AddMovementUseCase;
import com.jbh.account.application.core.usecases.CreateProductUseCase;
import com.jbh.account.application.core.usecases.FindMonthlyBalanceUseCase;
import com.jbh.account.application.core.vo.commands.AddMovementCommand;
import com.jbh.account.application.core.vo.commands.CreateProductCommand;
import com.jbh.account.domain.exceptions.ProductBusinessException;
import com.jbh.account.domain.vo.MovementCategoryDTO;
import com.jbh.account.domain.vo.ProductId;
import com.jbh.account.domain.vo.ProductMetadata;
import com.jbh.account.domain.vo.ProductPK;
import com.jbh.account.infra.adapters.in.rest.vo.AddMovementRequest;
import com.jbh.account.infra.adapters.in.rest.vo.CreateProductRequest;
import com.jbh.account.infra.adapters.in.rest.vo.FinanceApiRoutes;
import com.jbh.account.infra.adapters.in.rest.vo.MonthlyBalanceRequest;
import com.jbh.commons.exception.InternalSystemException;
import com.jbh.gateway.client.JbhGatewayException;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.*;
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

@SuppressWarnings({"PMD.UnnecessaryAnnotationValueElement", "PMD.CallSuperInConstructor"})
@RequestScoped
@Path(FinanceApiRoutes.PRODUCTS_API_PATH)
@Tag(name = "Product Operations", description = "Product management operations")
public class ProductRestAdapter extends BaseRestAdapter {

  private final Logger log = LoggerFactory.getLogger(ProductRestAdapter.class);
  private final CreateProductUseCase createProductUseCase;
  private final AddMovementUseCase addMovementUseCase;
  private final FindMonthlyBalanceUseCase findMonthlyBalanceUseCase;

  @Inject
  public ProductRestAdapter(
      final CreateProductUseCase createProductUseCase,
      final AddMovementUseCase addMovementUseCase,
      final FindMonthlyBalanceUseCase findMonthlyBalanceUseCase) {
    this.addMovementUseCase = addMovementUseCase;
    this.createProductUseCase = createProductUseCase;
    this.findMonthlyBalanceUseCase = findMonthlyBalanceUseCase;
  }

  @POST
  @Path("/")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Create a new Product",
      description = "Creates a new Product for the given user")
  @APIResponses(
      value = {
        @APIResponse(
            responseCode = "200",
            description = "Product created successfully",
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
  public Response createProduct(
      @RequestBody final CreateProductRequest request,
      @HeaderParam("Authorization") @Parameter(description = "JWT Bearer token", required = true)
          final String authorizationHeader)
      throws JbhGatewayException, ProductBusinessException, InternalSystemException {
    log.info("Creating Product for user {}", authorizationHeader);

    final UUID userId = findUserId(authorizationHeader);

    final ProductDTO createdProduct =
        createProductUseCase.execute(
            new CreateProductCommand(
                userId,
                request.name(),
                request.type(),
                ProductMetadata.fromMap(request.metadata())));

    return Response.ok(createdProduct).build();
  }

  @POST
  @Path(FinanceApiRoutes.PRODUCTS_MOVEMENTS_API_PATH + "/{productId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Add a movement to an Product",
      description = "Adds a movement to an Product")
  @APIResponses(
      value = {
        @APIResponse(
            responseCode = "200",
            description = "Movement created successfully",
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
  public Response addMovementToProduct(
      @PathParam("productId") final UUID productId,
      @RequestBody final AddMovementRequest request,
      @HeaderParam("Authorization") @Parameter(description = "JWT Bearer token", required = true)
          final String authorizationHeader)
      throws JbhGatewayException, ProductBusinessException, InternalSystemException {

    if (request == null) {
      throw new IllegalArgumentException("Command cannot be null");
    }

    final UUID userId = findUserId(authorizationHeader);

    log.info("Adding movement to Product {}", productId);

    final AddMovementCommand command =
        new AddMovementCommand(
            request.entryDate(),
            request.totalAmount(),
            request.balanceSnapshot(),
            request.movementType(),
            MovementCategoryDTO.withName(request.movementType(), request.categoryName()));

    final AddBasicMovementDTO response =
        addMovementUseCase.addMovement(userId, ProductId.of(productId), command);

    return Response.ok(response).build();
  }

  @POST
  @Path(FinanceApiRoutes.PRODUCTS_MONTHLY_BALANCES_API_PATH + "/{productId}")
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
  public Response findMonthlyBalances(
      @PathParam("productId") final UUID productId,
      @RequestBody final MonthlyBalanceRequest request,
      @HeaderParam("Authorization") @Parameter(description = "JWT Bearer token", required = true)
          final String authorizationHeader)
      throws JbhGatewayException, ProductBusinessException, InternalSystemException {

    request.validate();

    final UUID userId = findUserId(authorizationHeader);

    final List<MonthlyBalanceDTO> monthlyBalances =
        findMonthlyBalanceUseCase.findByAccountAndPeriods(
            new ProductPK(userId, ProductId.of(productId)),
            request.startPeriod(),
            request.endPeriod());

    return Response.ok(monthlyBalances).build();
  }
}
