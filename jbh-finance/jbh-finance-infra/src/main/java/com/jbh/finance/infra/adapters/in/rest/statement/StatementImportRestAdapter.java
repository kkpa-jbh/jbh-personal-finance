package com.jbh.finance.infra.adapters.in.rest.statement;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.InternalSystemException;
import com.jbh.finance.application.feature.statement.commands.ExtractStatementMovementsCommand;
import com.jbh.finance.application.feature.statement.commands.FindStatementImportCommand;
import com.jbh.finance.application.feature.statement.commands.RunStatementImportCommand;
import com.jbh.finance.application.feature.statement.commands.StartStatementImportCommand;
import com.jbh.finance.application.feature.statement.usecases.FindStatementImportUseCase;
import com.jbh.finance.application.feature.statement.usecases.StartStatementImportUseCase;
import com.jbh.finance.application.feature.statement.vo.PdfSha256;
import com.jbh.finance.domain.product.vo.ProductId;
import com.jbh.finance.infra.adapters.in.rest.common.BaseRestAdapter;
import com.jbh.finance.infra.adapters.in.rest.common.FinanceApiRoutes;
import com.jbh.finance.infra.adapters.in.rest.statement.request.StatementImportUploadRequest;
import com.jbh.finance.infra.adapters.in.rest.statement.response.StatementImportAcceptedResponse;
import com.jbh.finance.infra.adapters.in.rest.statement.response.StatementImportResponse;
import com.jbh.finance.infra.adapters.in.service.PdfStatementUnlocker;
import com.jbh.finance.infra.adapters.in.service.StatementImportDispatcher;
import com.jbh.finance.infra.adapters.in.service.StatementUploadReader;
import com.jbh.gateway.client.JbhGatewayException;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bank statement import into a product. POST admits the PDF and answers {@code 202} at once; the
 * movements are read and saved in the background ({@link StatementImportDispatcher}). GET polls
 * the state and returns the saved movements.
 *
 * <p>The PDF password comes only as a multipart form field and is never logged.
 */
@SuppressWarnings({"PMD.UnnecessaryAnnotationValueElement", "PMD.CallSuperInConstructor"})
@RequestScoped
@Path(FinanceApiRoutes.STATEMENT_IMPORTS_API_PATH)
@Tag(name = "Statement Imports", description = "Bank statement import into a product")
public class StatementImportRestAdapter extends BaseRestAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(StatementImportRestAdapter.class);
  private static final String LOG_PREFIX = "[StatementImport:Rest]";

  private final StatementImportDispatcher dispatcher;
  private final PdfStatementUnlocker unlocker;
  private final StatementUploadReader uploadReader;
  private final StartStatementImportUseCase startUseCase;
  private final FindStatementImportUseCase findUseCase;

  public StatementImportRestAdapter(
      final StatementImportDispatcher dispatcher,
      final PdfStatementUnlocker unlocker,
      final StatementUploadReader uploadReader,
      final StartStatementImportUseCase startUseCase,
      final FindStatementImportUseCase findUseCase) {
    this.dispatcher = dispatcher;
    this.unlocker = unlocker;
    this.uploadReader = uploadReader;
    this.startUseCase = startUseCase;
    this.findUseCase = findUseCase;
  }

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Import the movements of a bank statement PDF into a product",
      description =
          "Checks and unlocks the PDF in memory, admits the import, and answers at once. In the"
              + " background it reads the movements with AI and saves them in the product."
              + " Poll the import to see the result.")
  @APIResponses(
      value = {
        @APIResponse(
            responseCode = "202",
            description = "The import was admitted and is processing in the background",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = StatementImportAcceptedResponse.class))),
        @APIResponse(
            responseCode = "401",
            description = "Unauthorized",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = String.class))),
        @APIResponse(
            responseCode = "409",
            description = "The same PDF is already processing or done in this product",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = String.class))),
        @APIResponse(
            responseCode = "422",
            description =
                "Missing, too large or unreadable PDF, wrong password, unknown product or product"
                    + " type not allowed",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = String.class)))
      })
  @SecurityRequirement(name = "JWT")
  public Response startImport(
      @PathParam("productId") final UUID productId,
      @BeanParam final StatementImportUploadRequest form,
      @HeaderParam("Authorization") @Parameter(description = "JWT Bearer token", required = true)
          final String authorizationHeader)
      throws JbhGatewayException, BusinessException, InternalSystemException {

    final UUID userId = findUserId(authorizationHeader);
    final byte[] original = uploadReader.read(form.file);
    final byte[] unlocked = unlocker.unlock(original, form.password);

    final ProductId product = ProductId.of(productId);
    final UUID importId =
        startUseCase.start(new StartStatementImportCommand(userId, product, PdfSha256.of(original)));
    dispatcher.dispatch(
        new RunStatementImportCommand(
            importId, userId, product, new ExtractStatementMovementsCommand(unlocked)));

    LOG.info(LOG_PREFIX + " Statement import {} accepted", importId);
    return Response.accepted(StatementImportAcceptedResponse.processing(importId)).build();
  }

  @GET
  @Path(FinanceApiRoutes.STATEMENT_IMPORT_ID_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get the state of a statement import",
      description =
          "Returns the status, the summary and, when done, the saved movements sorted by date.")
  @APIResponses(
      value = {
        @APIResponse(
            responseCode = "200",
            description = "The state of the import",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = StatementImportResponse.class))),
        @APIResponse(
            responseCode = "401",
            description = "Unauthorized",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = String.class))),
        @APIResponse(
            responseCode = "422",
            description = "Unknown product, or unknown import in this product",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = String.class)))
      })
  @SecurityRequirement(name = "JWT")
  public StatementImportResponse findImport(
      @PathParam("productId") final UUID productId,
      @PathParam("importId") final UUID importId,
      @HeaderParam("Authorization") @Parameter(description = "JWT Bearer token", required = true)
          final String authorizationHeader)
      throws JbhGatewayException, BusinessException, InternalSystemException {

    final UUID userId = findUserId(authorizationHeader);
    return StatementImportResponse.fromDTO(
        findUseCase.find(
            new FindStatementImportCommand(userId, ProductId.of(productId), importId)));
  }
}
