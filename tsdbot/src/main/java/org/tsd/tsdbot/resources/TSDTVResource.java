package org.tsd.tsdbot.resources;

import com.codahale.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.rest.v1.tsdtv.Heartbeat;
import org.tsd.tsdbot.tsdtv.AgentRegistry;
import org.tsd.tsdbot.tsdtv.BlacklistedAgentException;
import org.tsd.tsdbot.tsdtv.library.TSDTVLibrary;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/tsdtv")
public class TSDTVResource {

    private static final Logger log = LoggerFactory.getLogger(TSDTVResource.class);

    private final AgentRegistry agentRegistry;
    private final TSDTVLibrary tsdtvLibrary;

    @Inject
    public TSDTVResource(AgentRegistry agentRegistry, TSDTVLibrary tsdtvLibrary) {
        this.agentRegistry = agentRegistry;
        this.tsdtvLibrary = tsdtvLibrary;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/listings")
    @Timed
    public Response getListings() {
        return Response.ok(tsdtvLibrary.getListings()).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/agent/{agentId}")
    @Timed
    public Response agentHeartbeat(@Context HttpServletRequest request,
                                   @PathParam("agentId") String agentId,
                                   Heartbeat heartbeat) {
        log.info("Received TSDTV agent heartbeat: {}", heartbeat);
        try {
            agentRegistry.handleHeartbeat(heartbeat, request.getRemoteAddr());
            return Response.accepted(/*Constants.TSDTV.AGENT_HEARTBEAT_PERIOD_MILLIS*/5000).build();
        } catch (BlacklistedAgentException e) {
            log.warn("Received heartbeat from blacklisted agent: {}", agentId);
            return Response.status(403).build();
        }
    }

}
