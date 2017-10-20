package org.tsd.tsdbot.tsdtv;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.rest.v1.tsdtv.Media;
import org.tsd.rest.v1.tsdtv.job.Job;
import org.tsd.rest.v1.tsdtv.job.JobType;
import org.tsd.rest.v1.tsdtv.queue.NowPlaying;
import org.tsd.rest.v1.tsdtv.queue.NowPlayingStatus;
import org.tsd.tsdbot.tsdtv.library.TSDTVLibrary;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Singleton
public class TSDTVQueue {

    private static final Logger log = LoggerFactory.getLogger(TSDTVQueue.class);

    private NowPlaying nowPlaying;
    private final List<Media> queue = new LinkedList<>();

    private final AgentRegistry agentRegistry;
    private final TSDTVLibrary library;

    @Inject
    public TSDTVQueue(ExecutorService executorService,
                      AgentRegistry agentRegistry,
                      TSDTVLibrary library) {
        this.library = library;
        this.agentRegistry = agentRegistry;
        executorService.submit(new QueueManagerThread());
    }

    public Map<String, Object> getFullQueue() {
        Map<String, Object> result = new HashMap<>();
        if (nowPlaying != null) {
            result.put("nowPlaying", nowPlaying);
        }
        if (queue != null) {
            result.put("queue", queue);
        }
        return result;
    }

    public synchronized void add(String agentId, int mediaId)
            throws MediaNotFoundException, DuplicateMediaQueuedException {
        log.info("Adding media to queue, agentId={}, mediaId={}", agentId, mediaId);
        Media media = library.findMediaById(agentId, mediaId);
        log.info("Found media: {}", media);
        if ( (nowPlaying != null && Objects.equals(media, nowPlaying.getMedia())) ||
                queue.contains(media) ) {
            log.error("Duplicate media: nowPlaying={}, queue={}", nowPlaying, queue);
            throw new DuplicateMediaQueuedException(agentId, mediaId);
        }
        queue.add(media);
    }

    public synchronized void confirmStarted(int mediaId) {
        log.info("Received start confirmation: mediaId={}, nowPlaying={}",
                mediaId, nowPlaying);
        if (nowPlaying != null && nowPlaying.getMedia().getId() == mediaId) {
            nowPlaying.setNowPlayingStatus(NowPlayingStatus.live);
        }
    }

    public synchronized void confirmStopped(int mediaId) {
        log.info("Received stop confirmation: mediaId={}, nowPlaying={}",
                mediaId, nowPlaying);
        if (nowPlaying != null && nowPlaying.getMedia().getId() == mediaId) {
            nowPlaying = null;
        }
    }

    // user action via API
    public synchronized void stopNowPlaying() {
        if (nowPlaying != null) {
            log.info("Stopping nowPlaying: {}", nowPlaying);
            nowPlaying.setNowPlayingStatus(NowPlayingStatus.stopping);
            Job stopJob = new Job();
            stopJob.setType(JobType.tsdtv_stop);
            agentRegistry.submitJob(nowPlaying.getMedia().getAgentId(), stopJob);
        }
    }

    private void setNowPlaying(Media media) {
        log.info("Setting nowPlaying: {}", media);
        this.nowPlaying = new NowPlaying();
        this.nowPlaying.setNowPlayingStatus(NowPlayingStatus.starting);
        this.nowPlaying.setMedia(media);
        Job playJob = new Job();
        playJob.setType(JobType.tsdtv_play);
        playJob.getParameters().put("mediaId", Integer.toString(nowPlaying.getMedia().getId()));
        agentRegistry.submitJob(nowPlaying.getMedia().getAgentId(), playJob);
    }

    class QueueManagerThread implements Runnable {
        private boolean shutdown = false;

        @Override
        public void run() {
            while (!shutdown) {
                if (nowPlaying == null && CollectionUtils.isNotEmpty(queue)) {
                    synchronized (queue) {
                        Media media = queue.remove(0);
                        log.info("Moving queued item to nowPlaying: {}", media);
                        setNowPlaying(media);
                    }
                }
                try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(2));
                } catch (Exception e) {
                    log.error("Interrupted");
                    shutdown = true;
                }
            }
        }
    }
}
