package org.tsd.tsdtv;

import com.google.inject.name.Named;
import fr.bmartel.speedtest.SpeedTestReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.client.TSDBotClient;
import org.tsd.rest.v1.tsdtv.Heartbeat;
import org.tsd.rest.v1.tsdtv.HeartbeatResponse;
import org.tsd.rest.v1.tsdtv.Inventory;
import org.tsd.rest.v1.tsdtv.job.Job;
import org.tsd.rest.v1.tsdtv.job.JobUpdate;
import org.tsd.rest.v1.tsdtv.job.JobUpdateType;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

public class HeartbeatThread implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatThread.class);


    private static final long PERIOD_SECONDS = 5;
    private static final long INVENTORY_PERIOD_MINUTES = 5;

    private final String agentId;
    private final NetworkMonitor networkMonitor;
    private final TSDBotClient tsdBotClient;
    private final AgentInventory agentInventory;
    private final TSDTVPlayer player;

    private Inventory inventory = null;
    private LocalDateTime inventoryLastSent = LocalDateTime.MIN;
    private boolean shutdown = false;

    @Inject
    public HeartbeatThread(TSDBotClient tsdBotClient,
                           NetworkMonitor networkMonitor,
                           AgentInventory agentInventory,
                           TSDTVPlayer player,
                           @Named("agentId") String agentId) {
        this.agentId = agentId;
        this.tsdBotClient = tsdBotClient;
        this.networkMonitor = networkMonitor;
        this.agentInventory = agentInventory;
        this.player = player;
    }

    public void run() {
        while (!shutdown) {
            log.info("Building heartbeat...");

            if (shouldRefreshInventory()) {
                inventory = agentInventory.compileInventory();
                inventoryLastSent = LocalDateTime.now();
            }

            Heartbeat heartbeat = new Heartbeat();
            heartbeat.setAgentId(agentId);
            heartbeat.setHealthy(true);
            heartbeat.setInventory(inventory);

            SpeedTestReport report = networkMonitor.getReport();
            if (report != null) {
                heartbeat.setUploadBitrate(report.getTransferRateBit().doubleValue());
            } else if (networkMonitor.getError() != null) {
                heartbeat.setUploadBitrate(null);
                heartbeat.setHealthy(false);
                heartbeat.setUnhealthyReason("Network diagnostic in error: " + networkMonitor.getError());
            } else {
                heartbeat.setUploadBitrate(null);
            }

            long sleepSeconds;
            HeartbeatResponse response;
            try {
                response = tsdBotClient.sendTsdtvAgentHeartbeat(heartbeat);
                sleepSeconds = response.getSleepSeconds();
                for (Job job : response.getJobsToExecute()) {
                    try {
                        handleJob(job);
                    } catch (Exception e) {
                        log.error("Error handling job " + job, e);
                    }
                }
            } catch (Exception e ) {
                log.error("Error sending heartbeat", e);
                sleepSeconds = PERIOD_SECONDS;
            }

            try {
                log.debug("Sleeping for {} ms", sleepSeconds);
                Thread.sleep(TimeUnit.SECONDS.toMillis(sleepSeconds));
            } catch (InterruptedException e) {
                log.error("Interrupted", e);
                shutdown = true;
            }
        }
    }

    private void handleJob(Job job) throws Exception {
        switch (job.getType()) {
            case tsdtv_play: {
                int mediaId = Integer.parseInt(job.getParameters().get("mediaId"));
                player.play(mediaId);

                JobUpdate response = new JobUpdate();
                response.setJobId(job.getId());
                response.setJobUpdateType(JobUpdateType.tsdtv_started);
                response.setData(job.getParameters());
                tsdBotClient.sendJobUpdate(response);
                break;
            }
            case tsdtv_stop: {
                player.stop();

                JobUpdate response = new JobUpdate();
                response.setJobId(job.getId());
                response.setJobUpdateType(JobUpdateType.tsdtv_stopped);
                response.setData(job.getParameters());
                tsdBotClient.sendJobUpdate(response);
                break;
            }
        }
    }

    private boolean shouldRefreshInventory() {
        return inventory == null
                || inventoryLastSent.isBefore(LocalDateTime.now().minus(INVENTORY_PERIOD_MINUTES, ChronoUnit.MINUTES));
    }

}
