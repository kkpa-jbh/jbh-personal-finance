package com.jbh.products.infra.adapters.in.rest.movement;

import com.jbh.products.infra.adapters.in.rest.common.BaseRestAdapter;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.InternalSystemException;
import com.jbh.gateway.client.JbhGatewayException;
import com.jbh.products.application.core.dto.LiquidationResultDTO;
import com.jbh.products.application.core.usecases.LiquidateAccountUseCase;
import com.jbh.products.application.core.vo.commands.ExternalAccountInfoVO;
import com.jbh.products.application.core.vo.commands.LiquidateAccountCommand;
import com.jbh.products.domain.product.vo.ProductId;
import com.jbh.products.domain.product.vo.ProductPK;
import com.jbh.products.infra.adapters.in.rest.movement.response.LiquidationResultResponse;
import com.jbh.products.infra.adapters.in.rest.common.FinanceApiRoutes;
import com.jbh.products.infra.adapters.in.rest.movement.request.LiquidateAccountRequest;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Optional;
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

@SuppressWarnings({"PMD.UnnecessaryAnnotationValueElement", "PMD.AvoidDuplicateLiterals"})
@RequestScoped
@Path(FinanceApiRoutes.PRODUCTS_API_PATH)
@Tag(name = "Product Liquidation", description = "Liquidate investment accounts")
public class LiquidateAccountRestAdapter extends BaseRestAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(LiquidateAccountRestAdapter.class);

  private final LiquidateAccountUseCase liquidateAccountUseCase;

  @Inject
  public LiquidateAccountRestAdapter(final LiquidateAccountUseCase liquidateAccountUseCase) {
    this.liquidateAccountUseCase = liquidateAccountUseCase;
  }

  @POST
  @Path("/{productId}/liquidate")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Liquidate an investment account",
      description = "Liquidate investment account and transfer proceeds to internal or external account")
  @APIResponses(
      value = {
        @APIResponse(
            responseCode = "200",
            description = "Account liquidated successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = LiquidationResultResponse.class))),
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
  public Response liquidateAccount(
      @PathParam("productId") final UUID productId,
      @RequestBody final LiquidateAccountRequest request,
      @HeaderParam("Authorization") @Parameter(description = "JWT Bearer token", required = true)
          final String authorizationHeader)
      throws JbhGatewayException, BusinessException, InternalSystemException {

    final UUID userId = findUserId(authorizationHeader);

    LOG.info("Liquidating account {} for user {}", productId, userId);

    final LiquidateAccountCommand command =
        new LiquidateAccountCommand(
            request.toInternalAccountId() != null
                ? Optional.of(new ProductPK(userId, ProductId.of(request.toInternalAccountId())))
                : Optional.empty(),
            request.toExternalAccountOwner() != null
                ? Optional.of(new ExternalAccountInfoVO(request.toExternalAccountOwner()))
                : Optional.empty(),
            request.currentBalance(),
            request.liquidatedDate());

    final LiquidationResultDTO result =
        liquidateAccountUseCase.liquidateAccount(userId, ProductId.of(productId), command);

    return Response.ok(LiquidationResultResponse.fromDTO(result)).build();
  }
}
