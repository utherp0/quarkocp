package org.uth.quarkube;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

public class Endpoints
{
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("/watch")
  public String envtest( @QueryParam("payload") String payload )
  {
    System.out.println( payload );
    return "Received...";
  }
}