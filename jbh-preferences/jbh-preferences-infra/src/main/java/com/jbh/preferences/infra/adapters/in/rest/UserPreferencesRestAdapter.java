package com.jbh.preferences.infra.adapters.in.rest;

import static com.jbh.preferences.infra.adapters.in.rest.vo.PreferencesApiRoutes.PREFERENCES_API_PATH;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.InternalSystemException;
import com.jbh.gateway.client.JbhGatewayException;
import com.jbh.preferences.application.core.dto.UserPreferencesDTO;
import com.jbh.preferences.application.core.ports.input.CreateUserPreferencesInputPort;
import com.jbh.preferences.application.core.ports.input.GetUserPreferencesInputPort;
import com.jbh.preferences.application.core.ports.input.UpdateUserPreferencesInputPort;
import com.jbh.preferences.infra.adapters.in.rest.vo.UpdatePreferencesRequest;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"PMD.UnnecessaryAnnotationValueElement"})
@RequestScoped
@Path(PREFERENCES_API_PATH)
@Tag(name = "User Preferences", description = "User preferences management operations")
public class UserPreferencesRestAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(UserPreferencesRestAdapter.class);

  private final GetUserPreferencesInputPort getPreferencesUseCase;
  private final UpdateUserPreferencesInputPort updatePreferencesUseCase;
  private final CreateUserPreferencesInputPort createPreferencesUseCase;
  private final BaseRestAdapter baseRestAdapter;

  @Inject
  public UserPreferencesRestAdapter(
      final GetUserPreferencesInputPort getPreferencesUseCase,
      final UpdateUserPreferencesInputPort updatePreferencesUseCase,
      final CreateUserPreferencesInputPort createPreferencesUseCase,
      final BaseRestAdapter baseRestAdapter) {
    this.getPreferencesUseCase = getPreferencesUseCase;
    this.updatePreferencesUseCase = updatePreferencesUseCase;
    this.createPreferencesUseCase = createPreferencesUseCase;
    this.baseRestAdapter = baseRestAdapter;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get user preferences",
      description = "Retrieves the current user's preferences")
  @APIResponses(
      value = {
        @APIResponse(
            responseCode = "200",
            description = "Preferences retrieved successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = UserPreferencesDTO.class))),
        @APIResponse(responseCode = "401", description = "Unauthorized"),
        @APIResponse(responseCode = "404", description = "Preferences not found")
      })
  public Response getPreferences(
      @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorizationHeader)
      throws BusinessException, JbhGatewayException, InternalSystemException {

    final UUID userId = baseRestAdapter.findUserId(authorizationHeader);
    LOG.debug("Getting preferences for user: {}", userId);

    final UserPreferencesDTO preferences = getPreferencesUseCase.execute(userId);
    return Response.ok(preferences).build();
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Update user preferences",
      description = "Updates the current user's preferences")
  @APIResponses(
      value = {
        @APIResponse(
            responseCode = "200",
            description = "Preferences updated successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = UserPreferencesDTO.class))),
        @APIResponse(responseCode = "400", description = "Invalid request"),
        @APIResponse(responseCode = "401", description = "Unauthorized"),
        @APIResponse(responseCode = "404", description = "Preferences not found")
      })
  public Response updatePreferences(
      @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorizationHeader,
      @RequestBody(
              description = "Updated preferences",
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON,
                      schema = @Schema(implementation = UpdatePreferencesRequest.class)))
          final UpdatePreferencesRequest request)
      throws BusinessException, JbhGatewayException, InternalSystemException {

    final UUID userId = baseRestAdapter.findUserId(authorizationHeader);
    LOG.debug("Updating preferences for user: {}", userId);

    final UserPreferencesDTO updated =
        updatePreferencesUseCase.execute(userId, request.toCommand());
    return Response.ok(updated).build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Create default user preferences",
      description = "Creates default preferences for a new user")
  @APIResponses(
      value = {
        @APIResponse(
            responseCode = "201",
            description = "Preferences created successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = UserPreferencesDTO.class))),
        @APIResponse(responseCode = "401", description = "Unauthorized"),
        @APIResponse(responseCode = "409", description = "Preferences already exist")
      })
  public Response createPreferences(
      @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorizationHeader)
      throws BusinessException, JbhGatewayException, InternalSystemException {

    final UUID userId = baseRestAdapter.findUserId(authorizationHeader);
    LOG.debug("Creating default preferences for user: {}", userId);

    final UserPreferencesDTO created = createPreferencesUseCase.execute(userId);
    return Response.status(Response.Status.CREATED).entity(created).build();
  }
}
