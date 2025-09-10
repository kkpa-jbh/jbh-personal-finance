package com.jbh.account.infra.adapters.in.rest;

import com.jbh.account.application.accounts.usecases.CreateAccountUseCase;
import com.jbh.account.application.accounts.usecases.NoOperationUseCase;
import com.jbh.account.application.accounts.vo.commands.CreateBasicAccountCommand;
import com.jbh.account.domain.vo.AccountDomainDTO;
import com.jbh.account.infra.ApiConstants;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;
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

@SuppressWarnings("PMD.UnnecessaryAnnotationValueElement")
@ApplicationScoped
@Path(ApiConstants.BASE_API_PATH + "/accounts")
@Tag(name = "Account Operations", description = "Account management operations")
public class AccountRestAdapter {

  private final Logger log = LoggerFactory.getLogger(AccountRestAdapter.class);
  private final NoOperationUseCase testingUseCase;
  private final CreateAccountUseCase createAccountUseCase;

  @Inject
  public AccountRestAdapter(final NoOperationUseCase testingUseCase, final CreateAccountUseCase createAccountUseCase) {
    this.createAccountUseCase = createAccountUseCase;
    this.testingUseCase = testingUseCase;
  }

  @POST
  @Path("/movements/upload-excel")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Upload Excel file with account movements",
      description = "Upload and process an Excel file containing account movements for a specific account"
  )
  @APIResponses(value = {
      @APIResponse(
          responseCode = "200",
          description = "Excel file processed successfully",
          content = @Content(
              mediaType = MediaType.APPLICATION_JSON,
              schema = @Schema(implementation = String.class)
          )
      ),
      @APIResponse(
          responseCode = "400",
          description = "Invalid file format or missing required parameters",
          content = @Content(
              mediaType = MediaType.APPLICATION_JSON,
              schema = @Schema(implementation = String.class)
          )
      ),
      @APIResponse(
          responseCode = "401",
          description = "Unauthorized",
          content = @Content(
              mediaType = MediaType.APPLICATION_JSON,
              schema = @Schema(implementation = String.class)
          )
      )
  })
  @Tag(name = "Account Operations", description = "Account management operations")
  @SecurityRequirement(name = "JWT")
  public Response uploadExcelMovements(
      @FormParam("file")
      @Parameter(description = "Excel file containing account movements", required = true) final InputStream fileInputStream,

      @FormParam("sheetName")
      @Parameter(description = "Name of the Excel sheet to process", required = true) final String sheetName,

      @FormParam("accountId")
      @Parameter(description = "UUID of the target account", required = true) final UUID accountId,

      @HeaderParam("Authorization")
      @Parameter(description = "JWT Bearer token", required = true) final String authorizationHeader
  ) {
    log.info("Uploading excel file {}", authorizationHeader);

    testingUseCase.healthCheck();

    return Response.ok().build();
  }

  @POST
  @Path("/")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Create a new account",
      description = "Creates a new account for the given user"
  )
  @APIResponses(value = {
      @APIResponse(
          responseCode = "200",
          description = "Account created successfully",
          content = @Content(
              mediaType = MediaType.APPLICATION_JSON,
              schema = @Schema(implementation = String.class)
          )
      ),
      @APIResponse(
          responseCode = "400",
          description = "Invalid command",
          content = @Content(
              mediaType = MediaType.APPLICATION_JSON,
              schema = @Schema(implementation = String.class)
          )
      ),
      @APIResponse(
          responseCode = "401",
          description = "Unauthorized",
          content = @Content(
              mediaType = MediaType.APPLICATION_JSON,
              schema = @Schema(implementation = String.class)
          )
      )
  })
  @Tag(name = "Account Operations", description = "Account management operations")
  @SecurityRequirement(name = "JWT")
  public Response createAccount(
      final @RequestBody CreateBasicAccountCommand command,
      final @HeaderParam("Authorization")
      @Parameter(description = "JWT Bearer token", required = true) String authorizationHeader
  ) {
    log.info("Creating account for user {}", authorizationHeader);

    final AccountDomainDTO accountDTO = createAccountUseCase.execute(command);

    return Response.ok(accountDTO).build();
  }
}
