package com.jbh.notification.infra.adapters.in.rest;

import com.jbh.commons.api.ApiResponse;
import com.jbh.notification.contracts.NotificationType;
import com.jbh.notification.contracts.SendNotificationCommand;
import com.jbh.notification.contracts.validation.NotificationValidationException;
import com.jbh.notification.infra.adapters.in.rest.vo.NotificationResponse;
import com.jbh.notification.infra.adapters.in.rest.vo.NotificationRoutes;
import com.jbh.notification.infra.dto.NotificationDTO;
import com.jbh.notification.infra.ports.input.NotificationServicePort;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequestScoped
@Path(NotificationRoutes.NOTIFICATIONS_API_PATH_V1)
@Tag(
    name = "Notification Operations",
    description = "Endpoints for sending and managing notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NotificationRestAdapterV1 {

  private static final Logger LOG = LoggerFactory.getLogger(NotificationRestAdapterV1.class);

  private final NotificationServicePort notificationService;

  @Inject
  public NotificationRestAdapterV1(final NotificationServicePort notificationService) {
    this.notificationService = notificationService;
  }

  @POST
  @Operation(
      summary = "Send a notification",
      description =
          "Sends a notification of the specified type (EMAIL, SMS, PUSH, IN_APP) to the recipient")
  @APIResponses({
    @APIResponse(
        responseCode = "200",
        description = "Notification sent successfully",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = NotificationResponse.class))),
    @APIResponse(responseCode = "400", description = "Invalid request data"),
    @APIResponse(responseCode = "500", description = "Internal server error")
  })
  public Response sendNotification(
      @Parameter(
              description = "Type of notification to send",
              required = false,
              schema =
                  @Schema(
                      enumeration = {"EMAIL", "SMS", "PUSH", "IN_APP"},
                      defaultValue = "EMAIL"))
          @QueryParam("type")
          @DefaultValue("EMAIL")
          final NotificationType type,
      @RequestBody(
              description = "Notification details",
              required = true,
              content = @Content(schema = @Schema(implementation = SendNotificationCommand.class)))
          @Valid
          final SendNotificationCommand request) {

    if (LOG.isInfoEnabled()) {
      LOG.info(
          "Received request to send {} notification to recipient: {} Request {}",
          type,
          request.recipientId(),
          request);
    }

    final NotificationDTO result;
    try {
      result = notificationService.sendNotification(request, type);
    } catch (final NotificationValidationException e) {
      return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
    }
    final NotificationResponse response = mapDTOToResponse(result);

    if (LOG.isInfoEnabled()) {
      LOG.info("Notification {} processed successfully", result.id());
    }

    return Response.ok(ApiResponse.success(response, "Notification processed successfully"))
        .build();
  }

  private NotificationResponse mapDTOToResponse(final NotificationDTO dto) {
    return new NotificationResponse(
        dto.id(),
        dto.recipientId(),
        dto.recipientEmail(),
        dto.senderUserId(),
        dto.senderEmail(),
        dto.subject(),
        dto.message(),
        dto.notificationType(),
        dto.status(),
        dto.read(),
        dto.metadata(),
        dto.sentAt(),
        dto.createdAt());
  }
}
