package org.tsd.tsdtv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.client.TSDBotClient;
import org.tsd.rest.v1.tsdtv.job.*;

import javax.inject.Inject;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class JobPollingThread implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(JobPollingThread.class);

    private static final long PERIOD_MILLIS = TimeUnit.SECONDS.toMillis(1);

    private final TSDBotClient tsdBotClient;
    private final TSDTVPlayer player;

    private boolean shutdown = false;

    @Inject
    public JobPollingThread(TSDBotClient tsdBotClient,
                            TSDTVPlayer player) {
        this.tsdBotClient = tsdBotClient;
        this.player = player;
    }

    @Override
    public void run() {
        while (!shutdown) {
            log.debug("Polling for jobs...");

            try {
                Job job = tsdBotClient.pollForJob();
                if (job != null) {
                    handleJob(job);
                }
            } catch (Exception e) {
                log.error("Error polling for jobs", e);
            }

            try {
                log.debug("Sleeping for {} ms", PERIOD_MILLIS);
                Thread.sleep(PERIOD_MILLIS);
            } catch (InterruptedException e) {
                log.error("Interrupted", e);
                shutdown = true;
            }
        }
    }

    private void handleJob(Job job) {
        if (job instanceof TSDTVPlayJob) {
            int mediaId = ((TSDTVPlayJob) job).getMediaId();
            TSDTVPlayJobResult result = new TSDTVPlayJobResult();
            result.setJobId(job.getId());
            try {
                player.play(mediaId);
                result.setSuccess(true);
                result.setTimeStarted(Instant.now().toEpochMilli());
            } catch (Exception e) {
                log.error("Error playing media: " + mediaId, e);
                result.setSuccess(false);
            }
            tsdBotClient.sendJobResult(result);
        } else if (job instanceof TSDTVStopJob) {
            TSDTVStopJobResult result = new TSDTVStopJobResult();
            result.setJobId(job.getId());
            try {
                player.stop();
            } catch (Exception e) {
                log.error("Error stopping stream", e);
            }
            tsdBotClient.sendJobResult(result);
        }
    }
}
