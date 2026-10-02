package com.jbh.finance.infra.adapters.in.rest.statement;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.InternalSystemException;
import com.jbh.finance.application.feature.statement.commands.ExtractStatementMovementsCommand;
import com.jbh.finance.infra.adapters.exceptions.FinanceInfraBusinessExceptionType;
import com.jbh.finance.infra.adapters.in.rest.common.BaseRestAdapter;
import com.jbh.finance.infra.adapters.in.rest.common.FinanceApiRoutes;
import com.jbh.finance.infra.adapters.in.service.PdfStatementUnlocker;
import com.jbh.finance.infra.adapters.in.service.StatementImportDispatcher;
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
import java.util.Map;
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
 *
 * <p>For now the import runs in the background ({@link StatementImportDispatcher}): the call
 * answers {@code 202} at once, and the result is only logged. This avoids HTTP timeouts while the
 * OCR and LLM providers are tested.
 */
@SuppressWarnings({"PMD.UnnecessaryAnnotationValueElement", "PMD.CallSuperInConstructor"})
@RequestScoped
@Path(FinanceApiRoutes.STATEMENTS_API_PATH)
@Tag(name = "Statements", description = "Bank statement import")
public class StatementRestAdapter extends BaseRestAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(StatementRestAdapter.class);
  private static final String LOG_PREFIX = "[StatementImport:Rest]";
  private static final String PDF_EXTENSION = ".pdf";
  private static final String PDF_MIME_TYPE = "application/pdf";

  private static final Map<String, String> PROCESSING = Map.of("status", "PROCESSING");

  private final StatementImportDispatcher dispatcher;
  private final PdfStatementUnlocker unlocker;
  private final StatementImportConfig config;

  public StatementRestAdapter(
      final StatementImportDispatcher dispatcher,
      final PdfStatementUnlocker unlocker,
      final StatementImportConfig config) {
    this.dispatcher = dispatcher;
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
          "Checks and unlocks the PDF in memory, then answers at once. In the background it"
              + " reads the movements with AI, checks the totals, and suggests one finance"
              + " category per movement. The result is only logged for now. Nothing is saved.")
  @APIResponses(
      value = {
        @APIResponse(
            responseCode = "202",
            description = "The PDF is valid; it is being processed in the background",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = Map.class))),
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
                "Missing, too large or unreadable PDF, or missing or wrong password",
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
    dispatcher.dispatch(new ExtractStatementMovementsCommand(unlocked));

    LOG.info(LOG_PREFIX + " Statement import accepted");
    return Response.accepted(PROCESSING).build();
  }

  private static boolean isPdf(final FileUpload fileUpload) {
    final String fileName = fileUpload.fileName();
    final boolean pdfName =
        fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(PDF_EXTENSION);
    return pdfName || PDF_MIME_TYPE.equalsIgnoreCase(fileUpload.contentType());
  }
}
