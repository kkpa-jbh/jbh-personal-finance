package com.jbh.finance.infra.adapters.in.rest.movement;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.InternalSystemException;
import com.jbh.finance.application.feature.movement.commands.AddMovementCommand;
import com.jbh.finance.application.feature.movement.dto.AddMovementResultDTO;
import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.application.feature.movement.usecases.AddMovementUseCase;
import com.jbh.finance.application.feature.movement.usecases.DeleteMovementUseCase;
import com.jbh.finance.application.feature.movement.usecases.FindMovementsUseCase;
import com.jbh.finance.domain.product.vo.ProductId;
import com.jbh.finance.infra.adapters.in.rest.common.BaseRestAdapter;
import com.jbh.finance.infra.adapters.in.rest.common.FinanceApiRoutes;
import com.jbh.finance.infra.adapters.in.rest.movement.request.AddMovementRequest;
import com.jbh.finance.infra.adapters.in.rest.movement.response.AddMovementResponse;
import com.jbh.finance.infra.adapters.in.rest.movement.response.MovementResponse;
import com.jbh.gateway.client.JbhGatewayException;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
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

@SuppressWarnings({"PMD.UnnecessaryAnnotationValueElement"})
@RequestScoped
@Path(FinanceApiRoutes.MOVEMENTS_API_PATH)
@Tag(name = "Movement Operations", description = "Product movement management operations")
public class MovementRestAdapter extends BaseRestAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(MovementRestAdapter.class);

  private final AddMovementUseCase addMovementUseCase;
  private final FindMovementsUseCase findMovementsUseCase;
  private final DeleteMovementUseCase deleteMovementUseCase;

  @Inject
  public MovementRestAdapter(
      final AddMovementUseCase addMovementUseCase,
      final FindMovementsUseCase findMovementsUseCase,
      final DeleteMovementUseCase deleteMovementUseCase) {
    this.addMovementUseCase = addMovementUseCase;
    this.findMovementsUseCase = findMovementsUseCase;
    this.deleteMovementUseCase = deleteMovementUseCase;
  }

  @POST
  @Path("/{productId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Add a movement to a Product",
      description = "Adds a movement (deposit, withdrawal, or balance snapshot) to a Product")
  @APIResponses(
      value = {
        @APIResponse(
            responseCode = "200",
            description = "Movement created successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = AddMovementResponse.class))),
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
      throw new IllegalArgumentException("Request cannot be null");
    }

    final UUID userId = findUserId(authorizationHeader);

    LOG.info("Adding movement to Product {}", productId);

    final AddMovementCommand command =
        new AddMovementCommand(
            request.entryDate(),
            request.totalAmount(),
            request.balanceSnapshot(),
            request.movementType(),
            request.categoryRequest().toDTO(),
            request.description());

    final AddMovementResultDTO response =
        addMovementUseCase.addMovement(userId, ProductId.of(productId), command);

    return Response.ok(AddMovementResponse.fromDTO(response)).build();
  }

  @GET
  @Path("/{productId}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get movements for a product",
      description =
          "Retrieves movements for a product within a specified time period (default 3 months back, inclusive)")
  @APIResponses(
      value = {
        @APIResponse(
            responseCode = "200",
            description = "Movements retrieved successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = MovementResponse.class))),
        @APIResponse(
            responseCode = "400",
            description = "Invalid request parameters",
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
  public Response getMovementsByProduct(
      @PathParam("productId") final UUID productId,
      @HeaderParam("Authorization") @Parameter(description = "JWT Bearer token", required = true)
          final String authorizationHeader)
      throws JbhGatewayException, BusinessException, InternalSystemException {
    final int monthsBack = 3;
    final UUID userId = findUserId(authorizationHeader);

    LOG.info(
        "Fetching movements for Product {} (user: {}, monthsBack: {})",
        productId,
        userId,
        monthsBack);

    final List<MovementDTO> movements =
        findMovementsUseCase.findMovementsByProduct(userId, ProductId.of(productId), monthsBack);

    final List<MovementResponse> responseList =
        movements.stream().map(MovementResponse::fromDTO).toList();

    return Response.ok(responseList).build();
  }

  @DELETE
  @Path("/{productId}/{movementId}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Delete a movement from a product",
      description =
          "Deletes a movement and reverses its effect on product balances. Only movements created in the current month can be deleted.")
  @APIResponses(
      value = {
        @APIResponse(responseCode = "204", description = "Movement deleted successfully"),
        @APIResponse(
            responseCode = "400",
            description = "Movement cannot be removed (not in current month)",
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
            description = "Movement or product not found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = String.class)))
      })
  @SecurityRequirement(name = "JWT")
  public Response deleteMovement(
      @PathParam("productId") final UUID productId,
      @PathParam("movementId") final UUID movementId,
      @HeaderParam("Authorization") @Parameter(description = "JWT Bearer token", required = true)
          final String authorizationHeader)
      throws JbhGatewayException, BusinessException, InternalSystemException {

    final UUID userId = findUserId(authorizationHeader);

    LOG.info("Deleting movement {} from product {}", movementId, productId);

    deleteMovementUseCase.deleteMovement(userId, ProductId.of(productId), movementId);

    return Response.noContent().build();
  }
}
