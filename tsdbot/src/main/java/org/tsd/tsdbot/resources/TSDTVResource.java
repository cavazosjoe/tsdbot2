package org.tsd.tsdbot.resources;

import com.codahale.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.rest.v1.tsdtv.Heartbeat;
import org.tsd.tsdbot.tsdtv.TSDTVLibrary;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/tsdtv")
public class TSDTVResource {

    private static final Logger log = LoggerFactory.getLogger(TSDTVResource.class);

    private final TSDTVLibrary tsdtvLibrary;

    @Inject
    public TSDTVResource(TSDTVLibrary tsdtvLibrary) {
        this.tsdtvLibrary = tsdtvLibrary;
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Timed
    public Response agentHeartbeat(@Context HttpServletRequest request, Heartbeat heartbeat) {
        log.info("Received heartbeat: {}", heartbeat);
        return Response.accepted().build();
    }

}
