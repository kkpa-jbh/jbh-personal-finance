package com.jbh.account.infra.adapters.in.rest;

import com.jbh.account.application.core.dto.AddBasicMovementDTO;
import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.application.core.dto.ProductDTO;
import com.jbh.account.application.core.usecases.AddMovementUseCase;
import com.jbh.account.application.core.usecases.CreateProductUseCase;
import com.jbh.account.application.core.usecases.FindMonthlyBalanceUseCase;
import com.jbh.account.application.core.vo.commands.AddMovementCommand;
import com.jbh.account.application.core.vo.commands.CreateProductCommand;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountPK;
import com.jbh.account.domain.vo.MovementCategoryDTO;
import com.jbh.account.domain.vo.ProductMetadata;
import com.jbh.account.infra.adapters.in.rest.vo.AccountApiRoutes;
import com.jbh.account.infra.adapters.in.rest.vo.AddMovementRequest;
import com.jbh.account.infra.adapters.in.rest.vo.CreateAccountRequest;
import com.jbh.account.infra.adapters.in.rest.vo.MonthlyBalanceRequest;
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
@Path(AccountApiRoutes.ACCOUNTS_API_PATH)
@Tag(name = "Account Operations", description = "Account management operations")
public class AccountRestAdapter extends BaseRestAdapter {

  private final Logger log = LoggerFactory.getLogger(AccountRestAdapter.class);
  private final CreateProductUseCase createProductUseCase;
  private final AddMovementUseCase addMovementUseCase;
  private final FindMonthlyBalanceUseCase findMonthlyBalanceUseCase;

  @Inject
  public AccountRestAdapter(
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
      summary = "Create a new account",
      description = "Creates a new account for the given user")
  @APIResponses(
      value = {
        @APIResponse(
            responseCode = "200",
            description = "Account created successfully",
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
  public Response createAccount(
      @RequestBody final CreateAccountRequest request,
      @HeaderParam("Authorization") @Parameter(description = "JWT Bearer token", required = true)
          final String authorizationHeader)
      throws JbhGatewayException, AccountBusinessException {
    log.info("Creating account for user {}", authorizationHeader);

    final UUID userId = findUserId(authorizationHeader);

    final ProductDTO accountDTO =
        createProductUseCase.execute(
            new CreateProductCommand(
                userId, request.name(), request.type(), ProductMetadata.empty()));

    return Response.ok(accountDTO).build();
  }

  @POST
  @Path(AccountApiRoutes.ACCOUNTS_MOVEMENTS_API_PATH + "/{accountId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Add a movement to an account",
      description = "Adds a movement to an account")
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
  public Response addMovementToAccount(
      @PathParam("accountId") final UUID accountId,
      @RequestBody final AddMovementRequest request,
      @HeaderParam("Authorization") @Parameter(description = "JWT Bearer token", required = true)
          final String authorizationHeader)
      throws JbhGatewayException, AccountBusinessException {

    if (request == null) {
      throw new IllegalArgumentException("Command cannot be null");
    }

    final UUID userId = findUserId(authorizationHeader);

    log.info("Adding movement to account {}", accountId);

    final AddMovementCommand command =
        new AddMovementCommand(
            request.entryDate(),
            request.totalAmount(),
            request.balanceSnapshot(),
            request.movementType(),
            MovementCategoryDTO.withName(request.movementType(), request.categoryName()));

    final AddBasicMovementDTO response =
        addMovementUseCase.addMovement(userId, AccountId.of(accountId), command);

    return Response.ok(response).build();
  }

  @POST
  @Path(AccountApiRoutes.ACCOUNTS_MONTHLY_BALANCES_API_PATH + "/{accountId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Find monthly balances for an account")
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
      @PathParam("accountId") final UUID accountId,
      @RequestBody final MonthlyBalanceRequest request,
      @HeaderParam("Authorization") @Parameter(description = "JWT Bearer token", required = true)
          final String authorizationHeader)
      throws JbhGatewayException, AccountBusinessException {

    request.validate();

    final UUID userId = findUserId(authorizationHeader);

    final List<MonthlyBalanceDTO> monthlyBalances =
        findMonthlyBalanceUseCase.findByAccountAndPeriods(
            new AccountPK(userId, AccountId.of(accountId)),
            request.startPeriod(),
            request.endPeriod());

    return Response.ok(monthlyBalances).build();
  }
}
