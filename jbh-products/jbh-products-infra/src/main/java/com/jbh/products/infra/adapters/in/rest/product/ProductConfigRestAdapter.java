package com.jbh.products.infra.adapters.in.rest.product;

import static com.jbh.products.infra.adapters.in.rest.common.FinanceApiRoutes.PRODUCTS_API_PATH;

import com.jbh.products.application.feature.product.dto.MetadataFieldConfigDTO;
import com.jbh.products.application.feature.product.usecases.GetProductMetadataConfigUseCase;
import com.jbh.products.domain.product.vo.ProductType;
import com.jbh.products.infra.adapters.in.rest.product.response.MetadataFieldConfigResponse;
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

@SuppressWarnings({"PMD.UnnecessaryAnnotationValueElement"})
@RequestScoped
@Path(PRODUCTS_API_PATH)
@Tag(name = "Product Configuration", description = "Product configuration and metadata operations")
public class ProductConfigRestAdapter {

  private final GetProductMetadataConfigUseCase metadataConfigUseCase;

  @Inject
  public ProductConfigRestAdapter(final GetProductMetadataConfigUseCase metadataConfigUseCase) {
    this.metadataConfigUseCase = metadataConfigUseCase;
  }

  @GET
  @Path("/metadata-config")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get metadata configuration for a product type",
      description =
          "Returns the metadata field configuration for a given product type, "
              + "including required/optional status, data type, and value constraints. "
              + "Required fields are returned first.")
  @APIResponses(
      value = {
        @APIResponse(
            responseCode = "200",
            description = "Metadata configuration retrieved successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = MetadataFieldConfigResponse.class))),
        @APIResponse(
            responseCode = "400",
            description = "Invalid product type",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = String.class)))
      })
  public Response getMetadataConfig(
      @QueryParam("productType")
          @Parameter(description = "The product type to get metadata configuration for")
          final ProductType productType) {

    if (productType == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("productType query parameter is required")
          .build();
    }

    final List<MetadataFieldConfigDTO> config = metadataConfigUseCase.execute(productType);

    return Response.ok(config.stream().map(MetadataFieldConfigResponse::fromDTO).toList()).build();
  }
}
