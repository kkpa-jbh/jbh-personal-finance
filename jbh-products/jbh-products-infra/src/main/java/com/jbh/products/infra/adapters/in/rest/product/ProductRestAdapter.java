package com.jbh.products.infra.adapters.in.rest.product;

import com.jbh.products.infra.adapters.in.rest.common.BaseRestAdapter;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.InternalSystemException;
import com.jbh.gateway.client.JbhGatewayException;
import com.jbh.products.application.core.dto.ProductDTO;
import com.jbh.products.application.core.usecases.CreateProductUseCase;
import com.jbh.products.application.core.usecases.DeleteProductUseCase;
import com.jbh.products.application.core.usecases.EditProductUseCase;
import com.jbh.products.application.core.usecases.FindProductsUseCase;
import com.jbh.products.application.core.usecases.UpdateProductStatusUseCase;
import com.jbh.products.application.core.usecases.UpdateProductUseCase;
import com.jbh.products.application.core.vo.commands.CreateProductCommand;
import com.jbh.products.application.core.vo.commands.DeleteProductCommand;
import com.jbh.products.application.core.vo.commands.EditProductCommand;
import com.jbh.products.application.core.vo.commands.FindProductCommand;
import com.jbh.products.application.core.vo.commands.UpdateMetadataProductCommand;
import com.jbh.products.application.core.vo.commands.UpdateProductStatusCommand;
import com.jbh.products.domain.vo.ProductId;
import com.jbh.products.domain.vo.ProductMetadata;
import com.jbh.products.domain.vo.ProductPK;
import com.jbh.products.infra.adapters.in.rest.product.response.ProductResponse;
import com.jbh.products.infra.adapters.in.rest.product.request.CreateProductRequest;
import com.jbh.products.infra.adapters.in.rest.product.request.EditProductRequest;
import com.jbh.products.infra.adapters.in.rest.common.FinanceApiRoutes;
import com.jbh.products.infra.adapters.in.rest.product.request.UpdateProductMetadataRequest;
import com.jbh.products.infra.adapters.in.rest.product.request.UpdateProductStatusRequest;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
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

@SuppressWarnings({"PMD.UnnecessaryAnnotationValueElement", "PMD.AvoidDuplicateLiterals"})
@RequestScoped
@Path(FinanceApiRoutes.PRODUCTS_API_PATH)
@Tag(name = "Product Operations", description = "Product management operations")
public class ProductRestAdapter extends BaseRestAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(ProductRestAdapter.class);

  private final CreateProductUseCase createProductUseCase;
  private final FindProductsUseCase findProductsUseCase;
  private final EditProductUseCase editProductUseCase;
  private final DeleteProductUseCase deleteProductUseCase;
  private final UpdateProductStatusUseCase updateProductStatusUseCase;
  private final UpdateProductUseCase updateProductUseCase;

  @Inject
  public ProductRestAdapter(
      final CreateProductUseCase createProductUseCase,
      final FindProductsUseCase findProductsUseCase,
      final EditProductUseCase editProductUseCase,
      final DeleteProductUseCase deleteProductUseCase,
      final UpdateProductStatusUseCase updateProductStatusUseCase,
      final UpdateProductUseCase updateProductUseCase) {
    this.createProductUseCase = createProductUseCase;
    this.findProductsUseCase = findProductsUseCase;
    this.editProductUseCase = editProductUseCase;
    this.deleteProductUseCase = deleteProductUseCase;
    this.updateProductStatusUseCase = updateProductStatusUseCase;
    this.updateProductUseCase = updateProductUseCase;
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
    LOG.info("Creating Product for user {}", authorizationHeader);

    final UUID userId = findUserId(authorizationHeader);

    final ProductDTO createdProduct =
        createProductUseCase.execute(
            new CreateProductCommand(
                userId,
                request.name(),
                request.type(),
                ProductMetadata.fromMap(request.metadata())));

    return Response.ok(ProductResponse.fromDTO(createdProduct)).build();
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
                    schema = @Schema(implementation = ProductResponse.class))),
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

    LOG.info("Finding active products for user {}", userId);

    final List<ProductDTO> activeProducts = findProductsUseCase.findActiveByUserId(userId);

    return Response.ok(activeProducts.stream().map(ProductResponse::fromDTO).toList()).build();
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
                    schema = @Schema(implementation = ProductResponse.class))),
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

    LOG.info("Editing Product {} for user {}", productId, userId);

    final EditProductCommand command =
        new EditProductCommand(
            userId,
            ProductId.of(productId),
            request.name(),
            ProductMetadata.fromMap(request.metadata()));

    final ProductDTO editedProduct = editProductUseCase.execute(command);

    return Response.ok(ProductResponse.fromDTO(editedProduct)).build();
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

    LOG.info("Deleting Product {} for user {}", productId, userId);

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

    LOG.info("Finding product by ID {}", productId);

    final UUID userId = findUserId(authorizationHeader);

    final Optional<ProductDTO> foundProduct =
        findProductsUseCase.findProductById(
            new FindProductCommand(userId, ProductId.of(productId)));
    return foundProduct.isPresent()
        ? Response.ok(ProductResponse.fromDTO(foundProduct.get())).build()
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
                    schema = @Schema(implementation = ProductResponse.class))),
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

    LOG.info("Updating Product {} status to {} for user {}", productId, request.active(), userId);

    final UpdateProductStatusCommand command =
        new UpdateProductStatusCommand(userId, ProductId.of(productId), request.active());

    final ProductDTO updatedProduct = updateProductStatusUseCase.execute(command);

    return Response.ok(ProductResponse.fromDTO(updatedProduct)).build();
  }

  @PUT
  @Path("/{productId}/metadata")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Update product metadata",
      description = "Replace all metadata for a product")
  @APIResponses(
      value = {
        @APIResponse(responseCode = "204", description = "Metadata updated successfully"),
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
  public Response updateProductMetadata(
      @PathParam("productId") final UUID productId,
      @RequestBody final UpdateProductMetadataRequest request,
      @HeaderParam("Authorization") @Parameter(description = "JWT Bearer token", required = true)
          final String authorizationHeader)
      throws JbhGatewayException, BusinessException, InternalSystemException {

    final UUID userId = findUserId(authorizationHeader);

    LOG.info("Updating metadata for product {} for user {}", productId, userId);

    final ProductPK accountPK = new ProductPK(userId, ProductId.of(productId));
    final ProductMetadata metadata = ProductMetadata.fromMap(request.metadata());
    final UpdateMetadataProductCommand command = new UpdateMetadataProductCommand(metadata);

    updateProductUseCase.replaceMetadata(accountPK, command);

    return Response.noContent().build();
  }
}
