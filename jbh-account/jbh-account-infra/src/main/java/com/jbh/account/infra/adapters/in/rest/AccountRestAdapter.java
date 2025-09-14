package com.jbh.account.infra.adapters.in.rest;

import static com.jbh.account.infra.adapters.in.rest.vo.AccountApiRoutes.MOVEMENTS_INBULK_API;
import static com.jbh.account.infra.common.utils.JbhStringUtils.toLowerCase;

import com.jbh.account.application.accounts.usecases.AddMovementUseCase;
import com.jbh.account.application.accounts.usecases.CreateAccountUseCase;
import com.jbh.account.application.accounts.vo.AddBasicMovementRequest;
import com.jbh.account.application.accounts.vo.commands.CreateBasicAccountCommand;
import com.jbh.account.domain.vo.AccountDomainDTO;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.infra.adapters.in.rest.vo.AccountApiRoutes;
import com.jbh.account.infra.adapters.in.rest.vo.ApiResponse;
import com.jbh.account.infra.adapters.in.rest.vo.CreateAccountRequest;
import com.jbh.account.infra.adapters.in.service.ExcelMovementReaderService;
import com.jbh.account.infra.gateway.GatewayClientFactory;
import com.jbh.gateway.client.JbhGatewayException;
import com.jbh.gateway.client.JbhHttpResponse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
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
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("PMD.UnnecessaryAnnotationValueElement")
@RequestScoped
@Path(AccountApiRoutes.ACCOUNTS_API_PATH)
@Tag(name = "Account Operations", description = "Account management operations")
public class AccountRestAdapter {

  private final Logger log = LoggerFactory.getLogger(AccountRestAdapter.class);
  private final CreateAccountUseCase createAccountUseCase;
  private final AddMovementUseCase addMovementUseCase;

  @Inject ExcelMovementReaderService excelMovementReaderService;

  GatewayClientFactory gatewayClientFactory;

  @Inject
  public AccountRestAdapter(
      final GatewayClientFactory gatewayClientFactory,
      final CreateAccountUseCase createAccountUseCase,
      final AddMovementUseCase addMovementUseCase) {
    this.createAccountUseCase = createAccountUseCase;
    this.gatewayClientFactory = gatewayClientFactory;
    this.addMovementUseCase = addMovementUseCase;
  }

  @POST
  @Path(MOVEMENTS_INBULK_API)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Upload Excel file with account movements",
      description =
          "Upload and process an Excel file containing account movements for a specific account")
  @APIResponses(
      value = {
        @APIResponse(
            responseCode = "200",
            description = "Excel file processed successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = String.class))),
        @APIResponse(
            responseCode = "400",
            description = "Invalid file format or missing required parameters",
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
  public Response uploadExcelMovements(
      @FormParam("file")
          @Parameter(description = "Excel file containing account movements", required = true)
          final FileUpload fileUpload,
      @FormParam("sheetName")
          @Parameter(description = "Name of the Excel sheet to process", required = true)
          final String sheetName,
      @FormParam("accountId")
          @Parameter(description = "UUID of the target account", required = true)
          final UUID accountId,
      @HeaderParam("Authorization") @Parameter(description = "JWT Bearer token", required = true)
          final String authorizationHeader)
      throws JbhGatewayException {
    log.info("Uploading excel file {}", authorizationHeader);

    final UUID userId = findUserId(authorizationHeader);

    try {
      // Validate file type
      if (!isExcelFile(fileUpload.fileName())) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(
                ApiResponse.error("Invalid file type. Only Excel files (.xlsx, .xls) are allowed"))
            .build();
      }

      // Process the Excel file
      final List<AddBasicMovementRequest> movements = processExcelFile(fileUpload, sheetName);

      addMovementUseCase.addBasicMovements(userId, AccountId.of(accountId), movements);

      log.info("Successfully processed {} movements from Excel file", movements.size());

      final ApiResponse<Boolean> response =
          ApiResponse.success(
              true,
              String.format(
                  "Successfully processed %d movements from sheet '%s'",
                  movements.size(), sheetName));

      return Response.ok(response).build();

    } catch (final ExcelMovementReaderService.ExcelReadingException e) {
      log.error("Excel reading error: {}", e.getMessage(), e);
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(ApiResponse.error("Excel processing error: " + e.getMessage()))
          .build();

    } catch (final IOException e) {
      log.error("File I/O error: {}", e.getMessage(), e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(ApiResponse.error("File processing error"))
          .build();
    }
  }

  private UUID findUserId(final String authorizationHeader) throws JbhGatewayException {
    final UUID userId;
    final JbhHttpResponse gatewayResponse =
        gatewayClientFactory
            .getUserClient()
            .findUserId(Map.of(HttpHeaders.AUTHORIZATION, authorizationHeader));

    if (gatewayResponse.isSuccessful() && gatewayResponse.getBodyAs(UUID.class).isPresent()) {
      userId = gatewayResponse.getBodyAs(UUID.class).get();
    } else {
      throw new IllegalArgumentException("Invalid user ID");
    }
    return userId;
  }

  /**
   * Validates if the uploaded file is an Excel file based on its extension.
   *
   * @param fileName The name of the uploaded file
   * @return true if it's an Excel file, false otherwise
   */
  private boolean isExcelFile(final String fileName) {
    if (fileName == null || fileName.isEmpty()) {
      return false;
    }

    final String lowerCaseFileName = toLowerCase(fileName);
    return lowerCaseFileName.endsWith(".xlsx") || lowerCaseFileName.endsWith(".xls");
  }

  /**
   * Processes the uploaded Excel file and extracts movement data.
   *
   * @param fileUpload The uploaded file
   * @param sheetName The sheet name to process
   * @return List of movement requests
   * @throws IOException if there's an error reading the file
   * @throws ExcelMovementReaderService.ExcelReadingException if there's an error processing the
   *     Excel
   */
  private List<AddBasicMovementRequest> processExcelFile(
      final FileUpload fileUpload, final String sheetName)
      throws IOException, ExcelMovementReaderService.ExcelReadingException {

    try (InputStream fileInputStream = Files.newInputStream(fileUpload.uploadedFile())) {
      return excelMovementReaderService.readMovementsFromExcel(fileInputStream, sheetName);
    }
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

    final AccountDomainDTO accountDTO =
        createAccountUseCase.execute(
            new CreateBasicAccountCommand(userId, request.name(), request.type()));

    return Response.ok(accountDTO).build();
  }
}
