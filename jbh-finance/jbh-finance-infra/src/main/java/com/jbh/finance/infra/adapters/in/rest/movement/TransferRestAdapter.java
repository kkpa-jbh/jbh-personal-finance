package com.jbh.finance.infra.adapters.in.rest.movement;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.InternalSystemException;
import com.jbh.finance.application.feature.movement.commands.AddTransferCommand;
import com.jbh.finance.application.feature.movement.usecases.AddTransferJbhAccountsUseCase;
import com.jbh.finance.domain.product.vo.ProductId;
import com.jbh.finance.domain.product.vo.ProductPK;
import com.jbh.finance.infra.adapters.in.rest.common.BaseRestAdapter;
import com.jbh.finance.infra.adapters.in.rest.common.FinanceApiRoutes;
import com.jbh.finance.infra.adapters.in.rest.movement.request.AddTransferRequest;
import com.jbh.gateway.client.JbhGatewayException;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
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

@SuppressWarnings({"PMD.UnnecessaryAnnotationValueElement", "PMD.AvoidDuplicateLiterals"})
@RequestScoped
@Path(FinanceApiRoutes.TRANSFERS_API_PATH)
@Tag(name = "Transfer Operations", description = "Transfer funds between JBH accounts")
public class TransferRestAdapter extends BaseRestAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(TransferRestAdapter.class);

  private final AddTransferJbhAccountsUseCase addTransferUseCase;

  @Inject
  public TransferRestAdapter(final AddTransferJbhAccountsUseCase addTransferUseCase) {
    this.addTransferUseCase = addTransferUseCase;
  }

  @POST
  @Path("/")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Transfer funds between accounts",
      description = "Transfer funds between two JBH accounts owned by the same user")
  @APIResponses(
      value = {
        @APIResponse(responseCode = "204", description = "Transfer completed successfully"),
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
  public Response addTransfer(
      @QueryParam("fromAccountId") @Parameter(description = "Source account ID", required = true)
          final UUID fromAccountId,
      @RequestBody final AddTransferRequest request,
      @HeaderParam("Authorization") @Parameter(description = "JWT Bearer token", required = true)
          final String authorizationHeader)
      throws JbhGatewayException, BusinessException, InternalSystemException {

    final UUID userId = findUserId(authorizationHeader);

    LOG.info(
        "Processing transfer from account {} to {} for user {}",
        fromAccountId,
        request.toAccountId(),
        userId);

    final ProductPK fromAccount = new ProductPK(userId, ProductId.of(fromAccountId));
    final AddTransferCommand command =
        new AddTransferCommand(
            new ProductPK(userId, ProductId.of(request.toAccountId())),
            request.totalAmount(),
            request.transferDate());

    addTransferUseCase.addTransfer(fromAccount, command);

    return Response.noContent().build();
  }
}
