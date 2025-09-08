package com.jbh.account.infra.adapters;

import com.jbh.account.infra.ApiConstants;
import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@ApplicationScoped
@Path(ApiConstants.BASE_API_PATH + "/hello")
public class GreetingResource {

  @PostConstruct
  void init() {
    Log.info("Initializing GreetingResource for jbh-account module.");
  }


  @GET
  @Path("/{name}")
  @Produces(MediaType.TEXT_PLAIN)
  public String helloWithInput(@PathParam("name") String name) {
    return "Hello from Quarkus...Mister " + name;
  }
}
