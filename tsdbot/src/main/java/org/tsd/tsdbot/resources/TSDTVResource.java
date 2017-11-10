package org.tsd.tsdbot.resources;

import com.google.inject.name.Named;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.Constants;
import org.tsd.rest.v1.tsdtv.Heartbeat;
import org.tsd.rest.v1.tsdtv.HeartbeatResponse;
import org.tsd.rest.v1.tsdtv.PlayMediaRequest;
import org.tsd.rest.v1.tsdtv.StoppedPlayingNotification;
import org.tsd.tsdbot.tsdtv.*;
import org.tsd.tsdbot.tsdtv.library.TSDTVLibrary;
import org.tsd.tsdbot.util.FileUtils;
import org.tsd.tsdbot.view.TSDTVView;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;

@Path("/tsdtv")
public class TSDTVResource {

    private static final Logger log = LoggerFactory.getLogger(TSDTVResource.class);

    private final AgentRegistry agentRegistry;
    private final TSDTVLibrary tsdtvLibrary;
    private final TSDTV tsdtv;
    private final String ownerKey;
    private final FileUtils fileUtils;

    @Inject
    public TSDTVResource(AgentRegistry agentRegistry,
                         TSDTVLibrary tsdtvLibrary,
                         TSDTV tsdtv,
                         FileUtils fileUtils,
                         @Named(Constants.Annotations.OWNER_KEY) String ownerKey) {
        this.agentRegistry = agentRegistry;
        this.tsdtvLibrary = tsdtvLibrary;
        this.tsdtv = tsdtv;
        this.ownerKey = ownerKey;
        this.fileUtils = fileUtils;
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
    @Path("/img/{mediaId}")
    public Response getQueueImage(@PathParam("mediaId") Integer mediaId) throws IOException, MediaNotFoundException {
        byte[] data = tsdtvLibrary.getQueueImage(mediaId);
        String contentType = fileUtils.detectMimeType(data);
        return Response.ok()
                .header("Content-Type", contentType)
                .entity((StreamingOutput) output -> {
                    output.write(data);
                    output.flush();
                })
                .build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/nowPlaying")
    public Response getNowPlaying() {
        try {
            return Response.ok(tsdtv.getLineup()).build();
        } catch (Exception e) {
            log.error("Error building TSDTV lineup", e);
            return Response.serverError().build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/agent/{agentId}")
    public Response agentHeartbeat(@Context HttpServletRequest request,
                                   @PathParam("agentId") String agentId,
                                   Heartbeat heartbeat) {
        log.info("Received TSDTV agent heartbeat: {}", agentId);
        log.debug("Heartbeat detail: {}", heartbeat);
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
            tsdtv.playOrEnqueue(playMediaRequest.getAgentId(), Integer.parseInt(playMediaRequest.getMediaId()));
            return Response.accepted("Accepted").build();
        } catch (TSDTVException e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/stop")
    public Response stop(@Context HttpServletRequest request,
                         @QueryParam("password") String password) {
        log.info("Received stop instruction, password={}", password);
        if (!StringUtils.equals(password, ownerKey)) {
            throw new NotAuthorizedException("Invalid password");
        }
        tsdtv.stopNowPlaying();
        return Response.accepted("Accepted").build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/stopped")
    public Response stopped(@Context HttpServletRequest request,
                            @QueryParam("password") String password,
                            StoppedPlayingNotification notification) {
        log.info("Received stopped notification, password={}: {}", password, notification);
        if (!StringUtils.equals(password, ownerKey)) {
            throw new NotAuthorizedException("Invalid password");
        }
        tsdtv.reportStopped(notification.getMediaId());
        return Response.accepted("Accepted").build();
    }
}
