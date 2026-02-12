package com.jbh.finance.infra.adapters.in.rest.category;

import com.jbh.finance.domain.movement.vo.IncomeCategory;
import com.jbh.finance.infra.adapters.in.rest.category.response.CategoryResponse;
import com.jbh.finance.infra.adapters.in.rest.common.FinanceApiRoutes;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Arrays;
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
@Path(FinanceApiRoutes.INCOME_CATEGORIES_API_PATH)
@Tag(name = "Categories", description = "Category catalog operations")
public class IncomeCategoryRestAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(IncomeCategoryRestAdapter.class);

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get all income categories",
      description = "Returns all available income categories with their translation keys")
  @APIResponses(
      value = {
        @APIResponse(
            responseCode = "200",
            description = "Income categories retrieved successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = CategoryResponse.class)))
      })
  public Response getAllIncomeCategories() {
    LOG.debug("Retrieving all income categories");
    final List<IncomeCategory> categories = Arrays.stream(IncomeCategory.values()).toList();
    LOG.info("Returning {} income categories", categories.size());
    return Response.ok(categories.stream().map(CategoryResponse::fromDTO).toList()).build();
  }
}
