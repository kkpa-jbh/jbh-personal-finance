package com.jbh.account.infra.adapters.in.rest;

import static com.jbh.account.infra.adapters.in.rest.vo.AccountApiRoutes.MOVEMENTS_INBULK_API;
import static com.jbh.account.infra.common.utils.JbhStringUtils.toLowerCase;

import com.jbh.account.application.core.usecases.AddMovementsUploadedFileUseCase;
import com.jbh.account.application.core.vo.commands.AddMovementUploadedFileCommand;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.infra.adapters.in.rest.vo.AccountApiRoutes;
import com.jbh.account.infra.adapters.in.rest.vo.AddMovementsUploadedFileRequest;
import com.jbh.account.infra.adapters.in.rest.vo.ApiResponse;
import com.jbh.account.infra.adapters.in.service.ExcelMovementReaderService;
import com.jbh.gateway.client.JbhGatewayException;
import jakarta.enterprise.context.RequestScoped;
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
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"PMD.UnnecessaryAnnotationValueElement", "PMD.CallSuperInConstructor"})
@RequestScoped
@Path(AccountApiRoutes.ACCOUNTS_API_PATH)
@Tag(name = "Upload movements to an account", description = "Register multiple movements")
public class AccountUploadFileRestAdapter extends BaseRestAdapter {

  private final ExcelMovementReaderService excelMovementReaderService;
  private final AddMovementsUploadedFileUseCase addUploadedMvmntsUseCase;
  private final Logger log = LoggerFactory.getLogger(AccountUploadFileRestAdapter.class);

  public AccountUploadFileRestAdapter(
      final AddMovementsUploadedFileUseCase addUploadedMvmntsUseCase,
      final ExcelMovementReaderService excelMovementReaderService) {
    this.excelMovementReaderService = excelMovementReaderService;
    this.addUploadedMvmntsUseCase = addUploadedMvmntsUseCase;
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
      final List<AddMovementsUploadedFileRequest> movements =
          processExcelFile(fileUpload, sheetName);
      final List<AddMovementUploadedFileCommand> movementsCommandList =
          transformMovementsToCommands(movements);
      addUploadedMvmntsUseCase.uploadMovementsFromFile(
          userId, AccountId.of(accountId), movementsCommandList);

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
  private List<AddMovementsUploadedFileRequest> processExcelFile(
      final FileUpload fileUpload, final String sheetName)
      throws IOException, ExcelMovementReaderService.ExcelReadingException {

    try (InputStream fileInputStream = Files.newInputStream(fileUpload.uploadedFile())) {
      return excelMovementReaderService.readMovementsFromExcel(fileInputStream, sheetName);
    }
  }

  private List<AddMovementUploadedFileCommand> transformMovementsToCommands(
      final List<AddMovementsUploadedFileRequest> movements) {
    return movements.stream().map(AddMovementsUploadedFileRequest::toCommand).toList();
  }
}
