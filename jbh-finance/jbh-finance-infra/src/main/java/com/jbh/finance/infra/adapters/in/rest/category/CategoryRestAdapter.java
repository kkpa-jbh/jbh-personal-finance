package com.jbh.finance.infra.adapters.in.rest.category;

import com.jbh.finance.domain.category.vo.CategorySourceVO;
import com.jbh.finance.infra.adapters.in.rest.category.response.CategoryEntityResponse;
import com.jbh.finance.infra.adapters.in.rest.common.FinanceApiRoutes;
import com.jbh.finance.infra.adapters.out.persistence.category.CategoryJPARepository;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"PMD.UnnecessaryAnnotationValueElement"})
@RequestScoped
@Path(FinanceApiRoutes.CATEGORIES_API_PATH)
@Tag(name = "Categories", description = "Category catalog operations")
public class CategoryRestAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(CategoryRestAdapter.class);

  private final CategoryJPARepository categoryRepository;

  @Inject
  public CategoryRestAdapter(final CategoryJPARepository categoryRepository) {
    this.categoryRepository = categoryRepository;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get categories by source",
      description =
          "Returns all categories filtered by source (INCOME or EXPENSE), sorted by short alias")
  @APIResponses(
      value = {
        @APIResponse(
            responseCode = "200",
            description = "Categories retrieved successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = CategoryEntityResponse.class))),
        @APIResponse(
            responseCode = "400",
            description = "Invalid or missing source parameter",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = String.class)))
      })
  public Response getCategoriesBySource(
      @QueryParam("source")
          @Parameter(description = "Category source: INCOME or EXPENSE", required = true)
          final CategorySourceVO source) {

    if (source == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Invalid type").build();
    }

    LOG.debug("Retrieving categories for source: {}", source);

    final List<CategoryEntityResponse> categories =
        categoryRepository.findBySource(source.name()).stream()
            .map(CategoryEntityResponse::fromEntity)
            .toList();

    LOG.info("Returning {} categories for source {}", categories.size(), source);
    return Response.ok(categories).build();
  }
}
