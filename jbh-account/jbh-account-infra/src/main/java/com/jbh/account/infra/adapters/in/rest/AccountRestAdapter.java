package com.jbh.account.infra.adapters.in.rest;

import com.jbh.account.application.core.dto.AccountDTO;
import com.jbh.account.application.core.usecases.CreateAccountUseCase;
import com.jbh.account.application.core.vo.commands.CreateBasicAccountCommand;
import com.jbh.account.infra.adapters.in.rest.vo.AccountApiRoutes;
import com.jbh.account.infra.adapters.in.rest.vo.CreateAccountRequest;
import com.jbh.gateway.client.JbhGatewayException;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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
  private final CreateAccountUseCase createAccountUseCase;

  @Inject
  public AccountRestAdapter(final CreateAccountUseCase createAccountUseCase) {
    this.createAccountUseCase = createAccountUseCase;
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
  @Tag(name = "Account Operations", description = "Account management operations")
  @SecurityRequirement(name = "JWT")
  public Response createAccount(
      @RequestBody final CreateAccountRequest request,
      @HeaderParam("Authorization") @Parameter(description = "JWT Bearer token", required = true)
          final String authorizationHeader)
      throws JbhGatewayException {
    log.info("Creating account for user {}", authorizationHeader);

    final UUID userId = findUserId(authorizationHeader);

    final AccountDTO accountDTO =
        createAccountUseCase.execute(
            new CreateBasicAccountCommand(userId, request.name(), request.type()));

    return Response.ok(accountDTO).build();
  }
}
