package org.tsd.tsdbot.resources;

import org.tsd.rest.v1.tsdtv.job.JobUpdate;
import org.tsd.tsdbot.tsdtv.TSDTVQueue;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/job")
public class JobResource {

    private final TSDTVQueue queue;

    @Inject
    public JobResource(TSDTVQueue queue) {
        this.queue = queue;
    }

    @PUT
    @Path("/{jobId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateJobStatus(@PathParam("jobId") String jobId, JobUpdate jobUpdate) {
        switch (jobUpdate.getJobUpdateType()) {
            case tsdtv_started: {
                queue.confirmStarted(Integer.parseInt(jobUpdate.getData().get("mediaId")));
                break;
            }

            case tsdtv_play_error:
            case tsdtv_stopped: {
                queue.confirmStopped(Integer.parseInt(jobUpdate.getData().get("mediaId")));
                break;
            }
        }

        return Response.ok().build();
    }
}
