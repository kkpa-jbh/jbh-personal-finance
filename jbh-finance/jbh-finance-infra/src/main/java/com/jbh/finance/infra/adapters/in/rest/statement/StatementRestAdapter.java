package com.jbh.finance.infra.adapters.in.rest.statement;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.InternalSystemException;
import com.jbh.finance.application.feature.statement.commands.ExtractStatementMovementsCommand;
import com.jbh.finance.application.feature.statement.dto.StatementClassificationDTO;
import com.jbh.finance.application.feature.statement.usecases.ExtractStatementMovementsUseCase;
import com.jbh.finance.infra.adapters.exceptions.FinanceInfraBusinessExceptionType;
import com.jbh.finance.infra.adapters.in.rest.common.BaseRestAdapter;
import com.jbh.finance.infra.adapters.in.rest.common.FinanceApiRoutes;
import com.jbh.finance.infra.adapters.in.rest.statement.response.StatementClassificationResponse;
import com.jbh.finance.infra.adapters.in.service.PdfStatementUnlocker;
import com.jbh.finance.infra.config.StatementImportConfig;
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
import java.nio.file.Files;
import java.util.Locale;
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

/**
 * Bank statement import: reads the movements of a PDF and suggests a category for each one.
 *
 * <p>The PDF password comes only as a multipart form field and is never logged. Nothing is saved.
 */
@SuppressWarnings({"PMD.UnnecessaryAnnotationValueElement", "PMD.CallSuperInConstructor"})
@RequestScoped
@Path(FinanceApiRoutes.STATEMENTS_API_PATH)
@Tag(name = "Statements", description = "Bank statement import")
public class StatementRestAdapter extends BaseRestAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(StatementRestAdapter.class);
  private static final String PDF_EXTENSION = ".pdf";
  private static final String PDF_MIME_TYPE = "application/pdf";

  private final ExtractStatementMovementsUseCase useCase;
  private final PdfStatementUnlocker unlocker;
  private final StatementImportConfig config;

  public StatementRestAdapter(
      final ExtractStatementMovementsUseCase useCase,
      final PdfStatementUnlocker unlocker,
      final StatementImportConfig config) {
    this.useCase = useCase;
    this.unlocker = unlocker;
    this.config = config;
  }

  @POST
  @Path(FinanceApiRoutes.STATEMENTS_EXTRACT_API)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Read and classify the movements of a bank statement PDF",
      description =
          "Unlocks the PDF in memory, reads its movements with AI, checks the totals, and"
              + " suggests one finance category per movement. Nothing is saved.")
  @APIResponses(
      value = {
        @APIResponse(
            responseCode = "200",
            description = "Movements read and classified",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = StatementClassificationResponse.class))),
        @APIResponse(
            responseCode = "401",
            description = "Unauthorized",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = String.class))),
        @APIResponse(
            responseCode = "422",
            description =
                "Missing, too large or unreadable PDF, missing or wrong password, no movements,"
                    + " or the AI providers failed",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = String.class)))
      })
  @SecurityRequirement(name = "JWT")
  // The cause is dropped on purpose: the upload is personal data.
  @SuppressWarnings("PMD.PreserveStackTrace")
  public Response extractMovements(
      @FormParam("file") @Parameter(description = "Bank statement PDF", required = true)
          final FileUpload fileUpload,
      @FormParam("password")
          @Parameter(description = "PDF password, only for locked PDFs. Never saved.")
          final String password,
      @HeaderParam("Authorization") @Parameter(description = "JWT Bearer token", required = true)
          final String authorizationHeader)
      throws JbhGatewayException, BusinessException, InternalSystemException {

    findUserId(authorizationHeader);

    if (fileUpload == null) {
      throw new BusinessException(FinanceInfraBusinessExceptionType.STATEMENT_FILE_REQUIRED);
    }
    if (!isPdf(fileUpload)) {
      throw new BusinessException(FinanceInfraBusinessExceptionType.STATEMENT_FILE_NOT_PDF);
    }
    if (fileUpload.size() > config.maxFileSizeBytes()) {
      throw new BusinessException(FinanceInfraBusinessExceptionType.STATEMENT_FILE_TOO_LARGE);
    }

    final byte[] content;
    try {
      content = Files.readAllBytes(fileUpload.uploadedFile());
    } catch (final IOException e) {
      throw new BusinessException(FinanceInfraBusinessExceptionType.STATEMENT_UNREADABLE_PDF);
    }

    final byte[] unlocked = unlocker.unlock(content, password);
    final StatementClassificationDTO dto =
        useCase.extract(new ExtractStatementMovementsCommand(unlocked));

    LOG.info("Statement import returned {} movements", dto.movements().size());
    return Response.ok(StatementClassificationResponse.fromDTO(dto)).build();
  }

  private static boolean isPdf(final FileUpload fileUpload) {
    final String fileName = fileUpload.fileName();
    final boolean pdfName =
        fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(PDF_EXTENSION);
    return pdfName || PDF_MIME_TYPE.equalsIgnoreCase(fileUpload.contentType());
  }
}
