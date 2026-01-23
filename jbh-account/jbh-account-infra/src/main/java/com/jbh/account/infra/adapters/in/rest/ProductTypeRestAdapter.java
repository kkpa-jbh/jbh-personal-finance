package com.jbh.account.infra.adapters.in.rest;

import com.jbh.account.infra.adapters.in.rest.vo.FinanceApiRoutes;
import com.jbh.account.infra.adapters.in.rest.vo.ProductTypeDTO;
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
@Path(FinanceApiRoutes.PRODUCT_TYPES_API_PATH)
@Tag(name = "Product Types", description = "Product type catalog operations")
public class ProductTypeRestAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(ProductTypeRestAdapter.class);

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get all product types",
      description = "Returns all available product types with their translation keys")
  @APIResponses(
      value = {
        @APIResponse(
            responseCode = "200",
            description = "Product types retrieved successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ProductTypeDTO.class)))
      })
  public Response getAllProductTypes() {
    LOG.debug("Retrieving all product types");
    final List<ProductTypeDTO> productTypes = ProductTypeDTO.allProductTypes();
    LOG.info("Returning {} product types", productTypes.size());
    return Response.ok(productTypes).build();
  }
}
