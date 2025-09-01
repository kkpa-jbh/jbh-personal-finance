package com.jbh.account;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/jbh-account/hello")
public class GreetingResource {



  @GET
  @Path("/{name}")
  @Produces(MediaType.TEXT_PLAIN)
  public String helloWithInput(@PathParam("name") String name) {
    return "Hello from Quarkus...Mister " + name;
  }
}
