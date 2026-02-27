package com.jbh.finance.infra.adapters.in.rest.category;

import com.jbh.finance.application.feature.category.dto.CategoryDTO;
import com.jbh.finance.application.feature.category.ports.output.CategoryQueryRepo;
import com.jbh.finance.infra.adapters.in.rest.category.response.CategoryResponse;
import com.jbh.finance.infra.adapters.in.rest.common.FinanceApiRoutes;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"PMD.UnnecessaryAnnotationValueElement"})
@RequestScoped
@Path(FinanceApiRoutes.EXPENSE_CATEGORIES_API_PATH)
@Tag(name = "Categories", description = "Category catalog operations")
public class ExpenseCategoryRestAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(ExpenseCategoryRestAdapter.class);

  @Inject private CategoryQueryRepo categoryQueryRepo;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get all expense categories",
      description = "Returns all available expense categories with their translation keys")
  @APIResponses(
      value = {
        @APIResponse(
            responseCode = "200",
            description = "Expense categories retrieved successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = CategoryResponse.class)))
      })
  public Response getAllExpenseCategories() {
    LOG.debug("Retrieving all expense categories");
    final List<CategoryDTO> categories = categoryQueryRepo.findAllSystemCategories();
    LOG.info("Returning {} expense categories", categories.size());
    return Response.ok(categories).build();
  }
}
