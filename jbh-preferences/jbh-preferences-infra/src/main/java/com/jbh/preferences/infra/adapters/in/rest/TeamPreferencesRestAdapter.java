package com.jbh.preferences.infra.adapters.in.rest;

import static com.jbh.preferences.infra.adapters.in.rest.vo.PreferencesApiConstants.BASE_API_PATH;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.InternalSystemException;
import com.jbh.gateway.client.JbhGatewayException;
import com.jbh.preferences.application.core.dto.TeamPreferencesDTO;
import com.jbh.preferences.application.core.ports.input.CreateTeamPreferencesInputPort;
import com.jbh.preferences.application.core.ports.input.GetTeamPreferencesInputPort;
import com.jbh.preferences.application.core.ports.input.UpdateTeamPreferencesInputPort;
import com.jbh.preferences.infra.adapters.in.rest.vo.CreateTeamPreferencesRequest;
import com.jbh.preferences.infra.adapters.in.rest.vo.TeamPreferencesResponse;
import com.jbh.preferences.infra.adapters.in.rest.vo.UpdateTeamPreferencesRequest;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
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
@Path(BASE_API_PATH + "/team-preferences/v1")
@Tag(name = "Team Preferences", description = "Team preferences management operations")
public class TeamPreferencesRestAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(TeamPreferencesRestAdapter.class);

  private final GetTeamPreferencesInputPort getPreferencesUseCase;
  private final UpdateTeamPreferencesInputPort updatePreferencesUseCase;
  private final CreateTeamPreferencesInputPort createPreferencesUseCase;
  private final BaseRestAdapter baseRestAdapter;

  @Inject
  public TeamPreferencesRestAdapter(
      final GetTeamPreferencesInputPort getPreferencesUseCase,
      final UpdateTeamPreferencesInputPort updatePreferencesUseCase,
      final CreateTeamPreferencesInputPort createPreferencesUseCase,
      final BaseRestAdapter baseRestAdapter) {
    this.getPreferencesUseCase = getPreferencesUseCase;
    this.updatePreferencesUseCase = updatePreferencesUseCase;
    this.createPreferencesUseCase = createPreferencesUseCase;
    this.baseRestAdapter = baseRestAdapter;
  }

  @GET
  @Path("/{teamId}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get team preferences",
      description = "Retrieves preferences for the specified team")
  @APIResponses(
      value = {
        @APIResponse(
            responseCode = "200",
            description = "Preferences retrieved successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = TeamPreferencesResponse.class))),
        @APIResponse(responseCode = "401", description = "Unauthorized"),
        @APIResponse(responseCode = "404", description = "Team preferences not found")
      })
  public Response getPreferences(
      @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorizationHeader,
      @PathParam("teamId") final UUID teamId)
      throws BusinessException, JbhGatewayException, InternalSystemException {

    final UUID userId = baseRestAdapter.findUserId(authorizationHeader);
    LOG.debug("Getting preferences for team: {} (requested by user: {})", teamId, userId);

    final TeamPreferencesDTO preferences = getPreferencesUseCase.execute(teamId);
    return Response.ok(TeamPreferencesResponse.fromDTO(preferences)).build();
  }

  @PUT
  @Path("/{teamId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Update team preferences",
      description = "Updates preferences for the specified team")
  @APIResponses(
      value = {
        @APIResponse(
            responseCode = "200",
            description = "Preferences updated successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = TeamPreferencesResponse.class))),
        @APIResponse(responseCode = "400", description = "Invalid request"),
        @APIResponse(responseCode = "401", description = "Unauthorized"),
        @APIResponse(responseCode = "404", description = "Team preferences not found")
      })
  public Response updatePreferences(
      @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorizationHeader,
      @PathParam("teamId") final UUID teamId,
      @RequestBody(
              description = "Updated team preferences",
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON,
                      schema = @Schema(implementation = UpdateTeamPreferencesRequest.class)))
          final UpdateTeamPreferencesRequest request)
      throws BusinessException, JbhGatewayException, InternalSystemException {

    final UUID userId = baseRestAdapter.findUserId(authorizationHeader);
    LOG.debug("Updating preferences {} for team: {} by user: {}", request, teamId, userId);

    final TeamPreferencesDTO updated =
        updatePreferencesUseCase.execute(teamId, request.toCommand(userId));
    return Response.ok(TeamPreferencesResponse.fromDTO(updated)).build();
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Create default team preferences",
      description = "Creates default preferences for a team")
  @APIResponses(
      value = {
        @APIResponse(
            responseCode = "201",
            description = "Preferences created successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = TeamPreferencesResponse.class))),
        @APIResponse(responseCode = "400", description = "Invalid request"),
        @APIResponse(responseCode = "401", description = "Unauthorized"),
        @APIResponse(responseCode = "409", description = "Team preferences already exist")
      })
  public Response createPreferences(
      @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorizationHeader,
      @RequestBody(
              description = "Team ID to create preferences for",
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON,
                      schema = @Schema(implementation = CreateTeamPreferencesRequest.class)))
          final CreateTeamPreferencesRequest request)
      throws BusinessException, JbhGatewayException, InternalSystemException {

    final UUID userId = baseRestAdapter.findUserId(authorizationHeader);
    LOG.debug("Creating default preferences for team: {} by user: {}", request.teamId(), userId);

    final TeamPreferencesDTO created =
        createPreferencesUseCase.execute(request.teamId(), userId);
    return Response.status(Response.Status.CREATED)
        .entity(TeamPreferencesResponse.fromDTO(created))
        .build();
  }
}
