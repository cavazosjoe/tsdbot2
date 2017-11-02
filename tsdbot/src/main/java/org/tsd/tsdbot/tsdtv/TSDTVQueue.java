package org.tsd.tsdbot.tsdtv;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.rest.v1.tsdtv.Episode;
import org.tsd.rest.v1.tsdtv.Media;
import org.tsd.rest.v1.tsdtv.Season;
import org.tsd.rest.v1.tsdtv.Series;
import org.tsd.rest.v1.tsdtv.job.TSDTVPlayJob;
import org.tsd.rest.v1.tsdtv.job.TSDTVPlayJobResult;
import org.tsd.rest.v1.tsdtv.job.TSDTVStopJob;
import org.tsd.rest.v1.tsdtv.queue.QueuedItem;
import org.tsd.rest.v1.tsdtv.queue.QueuedItemType;
import org.tsd.rest.v1.tsdtv.schedule.ScheduledBlock;
import org.tsd.rest.v1.tsdtv.schedule.ScheduledItem;
import org.tsd.tsdbot.Constants;
import org.tsd.tsdbot.tsdtv.job.JobQueue;
import org.tsd.tsdbot.tsdtv.job.JobTimeoutException;
import org.tsd.tsdbot.tsdtv.library.AgentMedia;
import org.tsd.tsdbot.tsdtv.library.TSDTVLibrary;
import org.tsd.tsdbot.tsdtv.library.TSDTVListing;
import org.tsd.tsdbot.util.TSDTVUtils;

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
    private final TSDTVEpisodicItemDao episodicItemDao;

    @Inject
    public TSDTVQueue(ExecutorService executorService,
                      TSDTVLibrary library,
                      TSDTVEpisodicItemDao episodicItemDao,
                      JobQueue jobQueue) {
        this.library = library;
        this.jobQueue = jobQueue;
        this.episodicItemDao = episodicItemDao;
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

    public synchronized boolean add(String agentId, int mediaId) throws TSDTVException {
        return add(agentId, mediaId, null);
    }

    private synchronized boolean add(String agentId, int mediaId, Integer episodeNumber) throws TSDTVException {
        log.info("Adding media to queue, agentId={}, mediaId={}", agentId, mediaId);
        Media media = library.findMediaById(agentId, mediaId);
        log.info("Found media: {}", media);

        if (nowPlaying == null && CollectionUtils.isEmpty(queue)) {
            // play immediately
            nowPlaying(media);
            return true;
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
            if (episodeNumber != null) {
                addingItem.setEffectiveEpisodeNumber(episodeNumber);
            }

            if (CollectionUtils.isNotEmpty(queue)) {
                QueuedItem lastItem = queue.get(queue.size() - 1);
                addingItem.setStartTime(lastItem.getEndTime() + Constants.TSDTV.SCHEDULING_FUDGE_FACTOR_MILLIS);
            } else {
                addingItem.setStartTime(nowPlaying.getEndTime() + Constants.TSDTV.SCHEDULING_FUDGE_FACTOR_MILLIS);
            }

            addingItem.updateEndTime();
            queue.add(addingItem);
            return false;
        }
    }

    private boolean doesQueueContainMedia(Media media) {
        return queue.stream().map(QueuedItem::getMedia).anyMatch(m -> m.equals(media));
    }

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

    public void startScheduledBlock(ScheduledBlock block) {
        log.warn("Starting scheduled block: {}", block);
        this.queue.clear();
        stopNowPlaying();

        TSDTVListing listing = library.getListings();
        List<Episode> toPlay = new LinkedList<>();

        // Used to keep track of episode numbers, since blocks can contain duplicate series/seasons
        Map<ScheduledItem, Integer> episodeNumberMap = new HashMap<>();

        for (ScheduledItem scheduledItem : block.getScheduledItems()) {
            AgentMedia<Series> matchingSeries = listing.getAllSeries()
                    .stream()
                    .filter(series -> StringUtils.equalsIgnoreCase(series.getMedia().getName(), scheduledItem.getSeries()))
                    .findAny().orElse(null);

            if (matchingSeries == null) {
                log.error("Could not find series matching \"{}\" in listings", scheduledItem.getSeries());
            } else {
                Series series = matchingSeries.getMedia();
                if (StringUtils.isNotBlank(scheduledItem.getSeason())) {
                    // this scheduled item specifies a season, find it
                    Season season = series.getSeasons()
                            .stream()
                            .filter(s -> StringUtils.equalsIgnoreCase(s.getName(), scheduledItem.getSeason()))
                            .findAny().orElse(null);
                    if (season == null) {
                        log.error("Could not find season matching \"{}\" for series {}",
                                scheduledItem.getSeason(), series.getName());
                    } else {
                        Episode episodeToPlay = findEpisodeToPlay(scheduledItem,
                                episodeNumberMap, series.getName(), season.getName(), season.getEpisodes());
                        toPlay.add(episodeToPlay);
                    }
                } else {
                    // this scheduled item only specifies a series
                    if (CollectionUtils.isNotEmpty(series.getSeasons())) {
                        // this series has seasons -- bundle up all episodes, use their index as episode number
                        List<Episode> effectiveEpisodes = TSDTVUtils.getEffectiveEpisodes(series);

                        int maxEpisodeNumber = effectiveEpisodes.size();
                        int currentEpisode = getEffectiveCurrentEpisodeNumber(scheduledItem,
                                episodeNumberMap, series.getName(), null);

                        Episode episodeToPlay;
                        if (currentEpisode > maxEpisodeNumber) {
                            log.info("currentEpisode {} is larger than maxEpisode {}, using first...",
                                    currentEpisode, maxEpisodeNumber);
                            episodeToPlay = effectiveEpisodes.get(0);
                            currentEpisode = 1;
                        } else {
                            episodeToPlay = effectiveEpisodes.get(currentEpisode-1);
                        }

                        toPlay.add(episodeToPlay);
                        log.info("Added episode to queue: {}", episodeToPlay);
                        episodeNumberMap.put(scheduledItem, currentEpisode+1);
                    } else {
                        // this series has no seasons -- use episode number from media
                        Episode episodeToPlay = findEpisodeToPlay(scheduledItem,
                                episodeNumberMap, series.getName(), null, series.getEpisodes());
                        toPlay.add(episodeToPlay);
                    }
                }
            }
        }

        for (Episode episode : toPlay) {
            int effectiveEpisodeNumber = episode.getOverriddenEpisodeNumber() != null ?
                    episode.getOverriddenEpisodeNumber() : episode.getEpisodeNumber();
            try {
                log.info("Adding episode to queue, series={}, season={}, effectiveEpisodeNumber = {}",
                        episode.getSeriesName(), episode.getSeasonName(), effectiveEpisodeNumber);
                add(episode.getAgentId(), episode.getId(), effectiveEpisodeNumber);
            } catch (Exception e) {
                log.error("Error adding episode to queue: {}", episode);
            }
        }
    }

    private Episode findEpisodeToPlay(ScheduledItem scheduledItem,
                                      Map<ScheduledItem, Integer> episodeNumberMap,
                                      String seriesName, String seasonName,
                                      List<Episode> episodes) {
        int episodeNumber = getEffectiveCurrentEpisodeNumber(scheduledItem, episodeNumberMap, seriesName, seasonName);
        Episode episode = findEpisodeMatchingNumber(episodes, episodeNumber);
        episodeNumberMap.put(scheduledItem, episode.getEpisodeNumber()+1);
        return episode;
    }

    private int getEffectiveCurrentEpisodeNumber(ScheduledItem scheduledItem,
                                                 Map<ScheduledItem, Integer> episodeNumberMap,
                                                 String seriesName, String seasonName) {
        if (episodeNumberMap.containsKey(scheduledItem)) {
            return episodeNumberMap.get(scheduledItem);
        } else {
            TSDTVEpisodicItem episodicItem = episodicItemDao.getCurrentEpisode(seriesName, seasonName);
            return episodicItem.getCurrentEpisode();
        }
    }

    private Episode findEpisodeMatchingNumber(List<Episode> allEpisodes, int number) {
        if (CollectionUtils.isEmpty(allEpisodes)) {
            throw new IllegalArgumentException("Episode list is null");
        }

        TreeMap<Integer, Episode> episodesOrderedByNumber = new TreeMap<>();
        for (Episode episode : allEpisodes) {
            log.info("Ordering episode by number: {} -> {}", episode.getEpisodeNumber(), episode);
            episodesOrderedByNumber.put(episode.getEpisodeNumber(), episode);
        }

        Episode matchingEpisode = null;
        int maxEpisodeNumber = episodesOrderedByNumber.lastKey();
        int searchingNumber = number;
        log.info("maxEpisodeNumber: {}", maxEpisodeNumber);
        while (matchingEpisode == null && searchingNumber <= maxEpisodeNumber) {
            Episode episode = episodesOrderedByNumber.get(searchingNumber);
            if (episode != null) {
                log.info("Found episode for number: {} -> {}", searchingNumber, episode);
                matchingEpisode = episode;
            } else {
                log.warn("Could not find episode for number {}", searchingNumber);
            }
            searchingNumber++;
        }

        if (matchingEpisode == null) {
            log.warn("Could not find any episodes matching number {}, using lowest available...", number);
            matchingEpisode = episodesOrderedByNumber.firstEntry().getValue();
        }

        log.info("Matching episode: {}", matchingEpisode);
        return matchingEpisode;
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
