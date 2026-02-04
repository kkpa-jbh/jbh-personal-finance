package com.jbh.products.infra.adapters.in.rest;

import com.jbh.products.infra.adapters.in.rest.vo.CategoryDTO;
import com.jbh.products.infra.adapters.in.rest.vo.FinanceApiRoutes;
import jakarta.enterprise.context.RequestScoped;
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
                    schema = @Schema(implementation = CategoryDTO.class)))
      })
  public Response getAllExpenseCategories() {
    LOG.debug("Retrieving all expense categories");
    final List<CategoryDTO> categories = CategoryDTO.allExpenseCategories();
    LOG.info("Returning {} expense categories", categories.size());
    return Response.ok(categories).build();
  }
}
