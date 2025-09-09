package com.jbh.notification_infra;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path(com.jbh.notification.ApiConstants.BASE_PATH + "/test-notification")
public class NotificationResource {


  @GET
  @Path("/status")
  public String status() {
    return "Notification Service is up and running!";
  }

}
