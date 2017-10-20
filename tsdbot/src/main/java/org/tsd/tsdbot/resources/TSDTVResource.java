package org.tsd.tsdbot.resources;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.rest.v1.tsdtv.Heartbeat;
import org.tsd.rest.v1.tsdtv.HeartbeatResponse;
import org.tsd.rest.v1.tsdtv.PlayMediaRequest;
import org.tsd.tsdbot.tsdtv.*;
import org.tsd.tsdbot.tsdtv.library.TSDTVLibrary;
import org.tsd.tsdbot.view.TSDTVView;

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
    private final TSDTVQueue tsdtvQueue;

    @Inject
    public TSDTVResource(AgentRegistry agentRegistry, TSDTVLibrary tsdtvLibrary, TSDTVQueue tsdtvQueue) {
        this.agentRegistry = agentRegistry;
        this.tsdtvLibrary = tsdtvLibrary;
        this.tsdtvQueue = tsdtvQueue;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TSDTVView getTsdtvPage(@QueryParam("playerType") String playerType) {
        TSDTVView.PlayerType playerTypeEnum = StringUtils.isBlank(playerType) ?
                TSDTVView.PlayerType.videojs : TSDTVView.PlayerType.valueOf(playerType);
        return new TSDTVView(tsdtvLibrary, playerTypeEnum);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/listings")
    public Response getListings() {
        return Response.ok(tsdtvLibrary.getListings()).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/nowPlaying")
    public Response getNowPlaying() {
        return Response.ok(tsdtvQueue.getFullQueue()).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/agent/{agentId}")
    public Response agentHeartbeat(@Context HttpServletRequest request,
                                   @PathParam("agentId") String agentId,
                                   Heartbeat heartbeat) {
        log.info("Received TSDTV agent heartbeat: {}", heartbeat);
        try {
            HeartbeatResponse response = agentRegistry.handleHeartbeat(heartbeat, request.getRemoteAddr());
            return Response.accepted(response).build();
        } catch (BlacklistedAgentException e) {
            log.warn("Received heartbeat from blacklisted agent: {}", agentId);
            return Response.status(403).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/play")
    public Response play(PlayMediaRequest playMediaRequest) {
        log.info("Received playMedia instruction: data={}", playMediaRequest);
        try {
            tsdtvQueue.add(playMediaRequest.getAgentId(), Integer.parseInt(playMediaRequest.getMediaId()));
            return Response.accepted("Accepted").build();
        } catch (MediaNotFoundException e) {
            return Response.serverError().entity("Could not find selected media").build();
        } catch (DuplicateMediaQueuedException e) {
            return Response.serverError().entity("Media already exists in queue").build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/stop")
    public Response stop(@Context HttpServletRequest request) {
        log.info("Received stop instruction");
        /*
        Job playMediaJob = new Job();
        playMediaJob.setType(JobType.tsdtv_play);
        playMediaJob.getParameters().put("mediaId", playMediaRequest.getMediaId());
        agentRegistry.submitJob(playMediaRequest.getAgentId(), playMediaJob);
        */
        return Response.accepted("Accepted").build();
    }
}
