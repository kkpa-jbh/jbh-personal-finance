package com.jbh.products.infra.adapters.in.rest.movement;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.InternalSystemException;
import com.jbh.gateway.client.JbhGatewayException;
import com.jbh.products.application.core.dto.AddBasicMovementDTO;
import com.jbh.products.application.core.usecases.AddMovementUseCase;
import com.jbh.products.application.core.vo.commands.AddMovementCommand;
import com.jbh.products.domain.movement.vo.MovementCategoryVO;
import com.jbh.products.domain.product.vo.ProductId;
import com.jbh.products.infra.adapters.in.rest.common.BaseRestAdapter;
import com.jbh.products.infra.adapters.in.rest.movement.response.AddBasicMovementResponse;
import com.jbh.products.infra.adapters.in.rest.movement.request.AddMovementRequest;
import com.jbh.products.infra.adapters.in.rest.common.FinanceApiRoutes;
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

  @Inject
  public MovementRestAdapter(final AddMovementUseCase addMovementUseCase) {
    this.addMovementUseCase = addMovementUseCase;
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
                    schema = @Schema(implementation = AddBasicMovementResponse.class))),
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
            MovementCategoryVO.withName(request.movementType(), request.categoryName()),
            request.description());

    final AddBasicMovementDTO response =
        addMovementUseCase.addMovement(userId, ProductId.of(productId), command);

    return Response.ok(AddBasicMovementResponse.fromDTO(response)).build();
  }
}
