package com.jbh.products.infra.adapters.in.rest;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.InternalSystemException;
import com.jbh.gateway.client.JbhGatewayException;
import com.jbh.products.application.core.dto.AddBasicMovementDTO;
import com.jbh.products.application.core.dto.MonthlyBalanceDTO;
import com.jbh.products.application.core.dto.ProductDTO;
import com.jbh.products.application.core.usecases.AddMovementUseCase;
import com.jbh.products.application.core.usecases.CreateProductUseCase;
import com.jbh.products.application.core.usecases.DeleteProductUseCase;
import com.jbh.products.application.core.usecases.EditProductUseCase;
import com.jbh.products.application.core.usecases.FindMonthlyBalanceUseCase;
import com.jbh.products.application.core.usecases.FindProductsUseCase;
import com.jbh.products.application.core.usecases.UpdateProductStatusUseCase;
import com.jbh.products.application.core.vo.commands.AddMovementCommand;
import com.jbh.products.application.core.vo.commands.CreateProductCommand;
import com.jbh.products.application.core.vo.commands.DeleteProductCommand;
import com.jbh.products.application.core.vo.commands.EditProductCommand;
import com.jbh.products.application.core.vo.commands.FindProductCommand;
import com.jbh.products.application.core.vo.commands.UpdateProductStatusCommand;
import com.jbh.products.domain.vo.MovementCategoryDTO;
import com.jbh.products.domain.vo.ProductId;
import com.jbh.products.domain.vo.ProductMetadata;
import com.jbh.products.domain.vo.ProductPK;
import com.jbh.products.infra.adapters.in.rest.vo.AddMovementRequest;
import com.jbh.products.infra.adapters.in.rest.vo.CreateProductRequest;
import com.jbh.products.infra.adapters.in.rest.vo.EditProductRequest;
import com.jbh.products.infra.adapters.in.rest.vo.FinanceApiRoutes;
import com.jbh.products.infra.adapters.in.rest.vo.MonthlyBalanceRequest;
import com.jbh.products.infra.adapters.in.rest.vo.UpdateProductStatusRequest;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

@SuppressWarnings({
  "PMD.UnnecessaryAnnotationValueElement",
  "PMD.CallSuperInConstructor",
  "PMD.AvoidDuplicateLiterals"
})
@RequestScoped
@Path(FinanceApiRoutes.PRODUCTS_API_PATH)
@Tag(name = "Product Operations", description = "Product management operations")
public class ProductRestAdapter extends BaseRestAdapter {

  private final Logger log = LoggerFactory.getLogger(ProductRestAdapter.class);
  private final CreateProductUseCase createProductUseCase;
  private final AddMovementUseCase addMovementUseCase;
  private final FindMonthlyBalanceUseCase findMonthlyBalanceUseCase;
  private final FindProductsUseCase findProductsUseCase;
  private final EditProductUseCase editProductUseCase;
  private final DeleteProductUseCase deleteProductUseCase;
  private final UpdateProductStatusUseCase updateProductStatusUseCase;

  @Inject
  public ProductRestAdapter(
      final CreateProductUseCase createProductUseCase,
      final AddMovementUseCase addMovementUseCase,
      final FindMonthlyBalanceUseCase findMonthlyBalanceUseCase,
      final FindProductsUseCase findProductsUseCase,
      final EditProductUseCase editProductUseCase,
      final DeleteProductUseCase deleteProductUseCase,
      final UpdateProductStatusUseCase updateProductStatusUseCase) {
    this.addMovementUseCase = addMovementUseCase;
    this.createProductUseCase = createProductUseCase;
    this.findMonthlyBalanceUseCase = findMonthlyBalanceUseCase;
    this.findProductsUseCase = findProductsUseCase;
    this.editProductUseCase = editProductUseCase;
    this.deleteProductUseCase = deleteProductUseCase;
    this.updateProductStatusUseCase = updateProductStatusUseCase;
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
      throws JbhGatewayException, BusinessException, InternalSystemException {
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
      throws JbhGatewayException, BusinessException, InternalSystemException {

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
      throws JbhGatewayException, BusinessException, InternalSystemException {

    log.info("Getting Monthly Balances {} for product {} ", request, productId);
    request.validate();

    final UUID userId = findUserId(authorizationHeader);

    final List<MonthlyBalanceDTO> monthlyBalances =
        findMonthlyBalanceUseCase.findByAccountAndPeriods(
            new ProductPK(userId, ProductId.of(productId)),
            request.startPeriod(),
            request.endPeriod());

    return Response.ok(monthlyBalances).build();
  }

  @POST
  @Path(FinanceApiRoutes.PRODUCTS_MONTHLY_BALANCES_API_PATH + "/")
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
  public Response findAllActiveProductsMonthlyBalances(
      @RequestBody final MonthlyBalanceRequest request,
      @HeaderParam("Authorization") @Parameter(description = "JWT Bearer token", required = true)
          final String authorizationHeader)
      throws JbhGatewayException, BusinessException, InternalSystemException {

    log.info("Getting Monthly Balances for all active products: {}", request);
    request.validate();

    final UUID userId = findUserId(authorizationHeader);

    final Map<ProductId, List<MonthlyBalanceDTO>> monthlyBalances =
        findMonthlyBalanceUseCase.findByActiveProductsAndPeriods(
            userId, request.startPeriod(), request.endPeriod());

    // Join all collections
    var allMonthlyBalances = monthlyBalances.values().stream().flatMap(List::stream).toList();

    return Response.ok(allMonthlyBalances).build();
  }

  @GET
  @Path("/")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get active products",
      description = "Retrieves all active products for the authenticated user")
  @APIResponses(
      value = {
        @APIResponse(
            responseCode = "200",
            description = "Active products retrieved successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ProductDTO.class))),
        @APIResponse(
            responseCode = "401",
            description = "Unauthorized",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = String.class)))
      })
  @SecurityRequirement(name = "JWT")
  public Response getActiveProducts(
      @HeaderParam("Authorization") @Parameter(description = "JWT Bearer token", required = true)
          final String authorizationHeader)
      throws JbhGatewayException, BusinessException, InternalSystemException {

    final UUID userId = findUserId(authorizationHeader);

    log.info("Finding active products for user {}", userId);

    final List<ProductDTO> activeProducts = findProductsUseCase.findActiveByUserId(userId);

    return Response.ok(activeProducts).build();
  }

  @PUT
  @Path("/{productId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Edit a Product", description = "Updates an existing Product")
  @APIResponses(
      value = {
        @APIResponse(
            responseCode = "200",
            description = "Product updated successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ProductDTO.class))),
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
                    schema = @Schema(implementation = String.class))),
        @APIResponse(
            responseCode = "404",
            description = "Product not found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = String.class)))
      })
  @SecurityRequirement(name = "JWT")
  public Response editProduct(
      @PathParam("productId") final UUID productId,
      @RequestBody final EditProductRequest request,
      @HeaderParam("Authorization") @Parameter(description = "JWT Bearer token", required = true)
          final String authorizationHeader)
      throws JbhGatewayException, BusinessException, InternalSystemException {

    final UUID userId = findUserId(authorizationHeader);

    log.info("Editing Product {} for user {}", productId, userId);

    final EditProductCommand command =
        new EditProductCommand(
            userId,
            ProductId.of(productId),
            request.name(),
            ProductMetadata.fromMap(request.metadata()));

    final ProductDTO editedProduct = editProductUseCase.execute(command);

    return Response.ok(editedProduct).build();
  }

  @DELETE
  @Path("/{productId}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Delete a Product", description = "Drop a product from DB")
  @APIResponses(
      value = {
        @APIResponse(responseCode = "204", description = "Product deleted successfully"),
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
                    schema = @Schema(implementation = String.class))),
        @APIResponse(
            responseCode = "404",
            description = "Product not found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = String.class)))
      })
  @SecurityRequirement(name = "JWT")
  public Response deleteProduct(
      @PathParam("productId") final UUID productId,
      @HeaderParam("Authorization") @Parameter(description = "JWT Bearer token", required = true)
          final String authorizationHeader)
      throws JbhGatewayException, BusinessException, InternalSystemException {

    final UUID userId = findUserId(authorizationHeader);

    log.info("Deleting Product {} for user {}", productId, userId);

    final DeleteProductCommand command = new DeleteProductCommand(userId, ProductId.of(productId));

    deleteProductUseCase.execute(command);

    return Response.noContent().build();
  }

  @GET
  @Path("/{productId}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Find a Product by ID", description = "Find products by ID")
  @APIResponses(
      value = {
        @APIResponse(responseCode = "200", description = "Product found"),
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
                    schema = @Schema(implementation = String.class))),
        @APIResponse(
            responseCode = "404",
            description = "Product not found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = String.class)))
      })
  @SecurityRequirement(name = "JWT")
  public Response findProductById(
      @PathParam("productId") final UUID productId,
      @HeaderParam("Authorization") @Parameter(description = "JWT Bearer token", required = true)
          final String authorizationHeader)
      throws JbhGatewayException, BusinessException, InternalSystemException {

    log.info("Finding product by ID {}", productId);

    final UUID userId = findUserId(authorizationHeader);

    final Optional<ProductDTO> foundProduct =
        findProductsUseCase.findProductById(
            new FindProductCommand(userId, ProductId.of(productId)));
    return foundProduct.isPresent()
        ? Response.ok(foundProduct.get()).build()
        : Response.status(Response.Status.NOT_FOUND).build();
  }

  @PATCH
  @Path("/{productId}/status")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Update Product status",
      description = "Partially updates a Product's active status (activate or deactivate)")
  @APIResponses(
      value = {
        @APIResponse(
            responseCode = "200",
            description = "Product status updated successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ProductDTO.class))),
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
                    schema = @Schema(implementation = String.class))),
        @APIResponse(
            responseCode = "404",
            description = "Product not found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = String.class)))
      })
  @SecurityRequirement(name = "JWT")
  public Response updateProductStatus(
      @PathParam("productId") final UUID productId,
      @RequestBody final UpdateProductStatusRequest request,
      @HeaderParam("Authorization") @Parameter(description = "JWT Bearer token", required = true)
          final String authorizationHeader)
      throws JbhGatewayException, BusinessException, InternalSystemException {

    final UUID userId = findUserId(authorizationHeader);

    log.info("Updating Product {} status to {} for user {}", productId, request.active(), userId);

    final UpdateProductStatusCommand command =
        new UpdateProductStatusCommand(userId, ProductId.of(productId), request.active());

    final ProductDTO updatedProduct = updateProductStatusUseCase.execute(command);

    return Response.ok(updatedProduct).build();
  }
}
