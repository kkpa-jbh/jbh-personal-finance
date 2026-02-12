package com.jbh.finance.infra.adapters.in.rest.product;

import com.jbh.finance.domain.product.vo.ProductType;
import com.jbh.finance.infra.adapters.in.rest.common.FinanceApiRoutes;
import com.jbh.finance.infra.adapters.in.rest.product.response.ProductTypeResponse;
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
                    schema = @Schema(implementation = ProductTypeResponse.class)))
      })
  public Response getAllProductTypes() {
    LOG.debug("Retrieving all product types");
    final List<ProductType> productTypes = Arrays.stream(ProductType.values()).toList();
    LOG.info("Returning {} product types", productTypes.size());
    return Response.ok(productTypes.stream().map(ProductTypeResponse::fromDTO).toList()).build();
  }
}
