package org.tsd.tsdbot.tsdtv;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.rest.v1.tsdtv.Media;
import org.tsd.rest.v1.tsdtv.job.TSDTVPlayJob;
import org.tsd.rest.v1.tsdtv.job.TSDTVPlayJobResult;
import org.tsd.rest.v1.tsdtv.job.TSDTVStopJob;
import org.tsd.rest.v1.tsdtv.queue.QueuedItem;
import org.tsd.rest.v1.tsdtv.queue.QueuedItemType;
import org.tsd.tsdbot.Constants;
import org.tsd.tsdbot.tsdtv.job.JobQueue;
import org.tsd.tsdbot.tsdtv.job.JobTimeoutException;
import org.tsd.tsdbot.tsdtv.library.TSDTVLibrary;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Singleton
public class TSDTVQueue {

    private static final Logger log = LoggerFactory.getLogger(TSDTVQueue.class);

    private QueuedItem nowPlaying;
    private final List<QueuedItem> queue = new LinkedList<>();

    private final TSDTVLibrary library;
    private final JobQueue jobQueue;

    @Inject
    public TSDTVQueue(ExecutorService executorService,
                      TSDTVLibrary library,
                      JobQueue jobQueue) {
        this.library = library;
        this.jobQueue = jobQueue;
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
        log.debug("Built full queue response: {}", result);
        return result;
    }

    public synchronized void add(String agentId, int mediaId) throws TSDTVException {
        log.info("Adding media to queue, agentId={}, mediaId={}", agentId, mediaId);
        Media media = library.findMediaById(agentId, mediaId);
        log.info("Found media: {}", media);

        if (nowPlaying == null && CollectionUtils.isEmpty(queue)) {
            // play immediately
            nowPlaying(media);
        } else {
            // try to enqueue
            if ( (nowPlaying != null && Objects.equals(media, nowPlaying.getMedia())) ||
                    doesQueueContainMedia(media) ) {
                log.error("Duplicate media: nowPlaying={}, queue={}", nowPlaying, queue);
                throw new DuplicateMediaQueuedException(agentId, mediaId);
            }

            QueuedItem addingItem = new QueuedItem();
            addingItem.setMedia(media);
            addingItem.setType(QueuedItemType.fromClass(media.getClass()));

            if (CollectionUtils.isNotEmpty(queue)) {
                QueuedItem lastItem = queue.get(queue.size() - 1);
                addingItem.setStartTime(lastItem.getEndTime() + Constants.TSDTV.SCHEDULING_FUDGE_FACTOR_MILLIS);
            } else {
                addingItem.setStartTime(nowPlaying.getEndTime() + Constants.TSDTV.SCHEDULING_FUDGE_FACTOR_MILLIS);
            }

            addingItem.updateEndTime();
            queue.add(addingItem);
        }
    }

    private boolean doesQueueContainMedia(Media media) {
        return queue.stream().map(QueuedItem::getMedia).anyMatch(m -> m.equals(media));
    }

    // user action via API
    public synchronized void stopNowPlaying() {
        if (nowPlaying != null) {
            log.info("Stopping nowPlaying: {}", nowPlaying);
            TSDTVStopJob stopJob = new TSDTVStopJob();
            try {
                jobQueue.submitJob(nowPlaying.getMedia().getAgentId(), stopJob, TimeUnit.SECONDS.toMillis(10));
            } catch (Exception e) {
                log.error("Error stopping media " + nowPlaying, e);
            }
            this.nowPlaying = null;
        }
    }

    public synchronized void reportStopped(int mediaId) {
        log.info("Handling stopped notification, mediaId={}, nowPlaying={}", mediaId, nowPlaying);
        if (nowPlaying != null && nowPlaying.getMedia().getId() == mediaId) {
            this.nowPlaying = null;
        }
    }

    private void nowPlaying(Media media) throws TSDTVException {
        log.info("Setting nowPlaying: {}", media);
        TSDTVPlayJob playJob = new TSDTVPlayJob();
        playJob.setMediaId(media.getId());
        try {
            TSDTVPlayJobResult result
                    = jobQueue.submitJob(media.getAgentId(), playJob, TimeUnit.SECONDS.toMillis(20));

            if (!result.isSuccess()) {
                throw new TSDTVException("Error playing media");
            }

            long startedTimeUTC = result.getTimeStarted();

            QueuedItem queuedItem = new QueuedItem();
            queuedItem.setMedia(media);
            queuedItem.setType(QueuedItemType.fromClass(media.getClass()));
            queuedItem.setStartTime(startedTimeUTC);
            queuedItem.updateEndTime();
            log.info("Set nowPlaying start/end times, {} -> {}", queuedItem.getStartTime(), queuedItem.getEndTime());
            this.nowPlaying = queuedItem;

            long lastItemEndTime = queuedItem.getEndTime();
            for (QueuedItem inQueue : queue) {
                inQueue.setStartTime(lastItemEndTime + Constants.TSDTV.SCHEDULING_FUDGE_FACTOR_MILLIS);
                inQueue.updateEndTime();
                log.info("Set queued media start/end times, mediaId={}, {} -> {}",
                        inQueue.getMedia().getId(), queuedItem.getStartTime(), queuedItem.getEndTime());
                lastItemEndTime = inQueue.getEndTime();
            }

        } catch (JobTimeoutException e) {
            log.error("Timed out waiting for response to play job");
        }
    }

    class QueueManagerThread implements Runnable {
        private boolean shutdown = false;

        @Override
        public void run() {
            while (!shutdown) {
                if (nowPlaying == null && CollectionUtils.isNotEmpty(queue)) {
                    synchronized (queue) {
                        QueuedItem queuedItem = queue.remove(0);
                        log.info("Moving queued item to nowPlaying: {}", queuedItem.getMedia());
                        try {
                            nowPlaying(queuedItem.getMedia());
                        } catch (Exception e) {
                            log.error("Error playing media: " + queuedItem.getMedia(), e);
                        }
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
