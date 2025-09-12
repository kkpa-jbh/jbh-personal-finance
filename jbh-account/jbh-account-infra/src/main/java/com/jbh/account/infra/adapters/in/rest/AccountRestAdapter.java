package com.jbh.account.infra.adapters.in.rest;

import com.jbh.account.application.accounts.usecases.CreateAccountUseCase;
import com.jbh.account.application.accounts.usecases.NoOperationUseCase;
import com.jbh.account.application.accounts.vo.AddBasicMovementRequest;
import com.jbh.account.application.accounts.vo.commands.CreateBasicAccountCommand;
import com.jbh.account.domain.vo.AccountDomainDTO;
import com.jbh.account.infra.ApiConstants;
import com.jbh.account.infra.adapters.in.service.ExcelMovementReaderService;
import com.jbh.account.infra.gateway.JbhGatewayClients;
import com.jbh.api.client.api.JbhApiException;
import com.jbh.api.client.api.JbhHttpHeaderNames;
import com.jbh.api.client.core.http.model.JbhHttpResponse;
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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
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
import org.jboss.resteasy.reactive.multipart.FileUpload;
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
  ExcelMovementReaderService excelMovementReaderService;

  @Inject
  JbhGatewayClients jbhGatewayClients;

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
      @Parameter(description = "Excel file containing account movements", required = true) final FileUpload fileUpload,

      @FormParam("sheetName")
      @Parameter(description = "Name of the Excel sheet to process", required = true) final String sheetName,

      @FormParam("accountId")
      @Parameter(description = "UUID of the target account", required = true) final UUID accountId,

      @HeaderParam("Authorization")
      @Parameter(description = "JWT Bearer token", required = true) final String authorizationHeader
  ) {
    log.info("Uploading excel file {}", authorizationHeader);

    testingUseCase.healthCheck();

    try {
      JbhHttpResponse response = jbhGatewayClients.getUserClient().findUserId(
          Map.of(JbhHttpHeaderNames.REQ_JBH_TOKEN, authorizationHeader, JbhHttpHeaderNames.REQ_SOURCE_HEADER,
              "JBH-ACCOUNT-API")
      );

      log.info("UserId {}", response.getBody());

    } catch (JbhApiException e) {
      throw new RuntimeException(e);
    }

    try {
      // Validate file type
      if (!isExcelFile(fileUpload.fileName())) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(ApiResponse.error("Invalid file type. Only Excel files (.xlsx, .xls) are allowed"))
            .build();
      }

      // Process the Excel file
      final List<AddBasicMovementRequest> movements = processExcelFile(fileUpload, sheetName);

      log.info("Successfully processed {} movements from Excel file", movements.size());

      final ApiResponse<List<AddBasicMovementRequest>> response = ApiResponse.success(
          movements,
          String.format("Successfully processed %d movements from sheet '%s'", movements.size(), sheetName)
      );

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

    } catch (final Exception e) {
      log.error("Unexpected error processing Excel file: {}", e.getMessage(), e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(ApiResponse.error("An unexpected error occurred"))
          .build();
    }

  }

  /**
   * Processes the uploaded Excel file and extracts movement data.
   *
   * @param fileUpload The uploaded file
   * @param sheetName  The sheet name to process
   * @return List of movement requests
   * @throws IOException                                      if there's an error reading the file
   * @throws ExcelMovementReaderService.ExcelReadingException if there's an error processing the Excel
   */
  private List<AddBasicMovementRequest> processExcelFile(
      final FileUpload fileUpload,
      final String sheetName) throws IOException, ExcelMovementReaderService.ExcelReadingException {

    try (final InputStream fileInputStream = Files.newInputStream(fileUpload.uploadedFile())) {
      return excelMovementReaderService.readMovementsFromExcel(fileInputStream, sheetName);
    }
  }

  /**
   * Validates if the uploaded file is an Excel file based on its extension.
   *
   * @param fileName The name of the uploaded file
   * @return true if it's an Excel file, false otherwise
   */
  private boolean isExcelFile(final String fileName) {
    if (fileName == null || fileName.trim().isEmpty()) {
      return false;
    }

    final String lowerCaseFileName = fileName.toLowerCase();
    return lowerCaseFileName.endsWith(".xlsx") || lowerCaseFileName.endsWith(".xls");
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
