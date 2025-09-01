package com.jbh.notification_infra;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/test-notification")
public class TestNotificationResource {


  @GET
  @Path("/status")
  public String status() {
    return "Notification Service is up and running!";
  }

}
