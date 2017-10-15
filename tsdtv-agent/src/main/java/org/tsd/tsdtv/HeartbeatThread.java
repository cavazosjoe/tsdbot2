package org.tsd.tsdtv;

import com.google.inject.name.Named;
import fr.bmartel.speedtest.SpeedTestReport;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.probe.FFmpegFormat;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.client.TSDBotClient;
import org.tsd.rest.v1.tsdtv.*;
import org.tsd.rest.v1.tsdtv.stream.AudioStream;
import org.tsd.rest.v1.tsdtv.stream.SubtitleStream;
import org.tsd.rest.v1.tsdtv.stream.VideoStream;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class HeartbeatThread implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatThread.class);

    private static final String LANGUAGE_TAG = "language";
    private static final long PERIOD_MILLIS = TimeUnit.SECONDS.toMillis(10);

    private final String agentId;
    private final File inventoryDirectory;
    private final FFprobe fFprobe;
    private final NetworkMonitor networkMonitor;
    private final TSDBotClient tsdBotClient;

    private boolean shutdown = false;

    @Inject
    public HeartbeatThread(TSDBotClient tsdBotClient,
                           FFprobe fFprobe,
                           NetworkMonitor networkMonitor,
                           @Named("agentId") String agentId,
                           @Named("inventory") File inventoryDirectory) {
        this.agentId = agentId;
        this.inventoryDirectory = inventoryDirectory;
        this.fFprobe = fFprobe;
        this.tsdBotClient = tsdBotClient;
        this.networkMonitor = networkMonitor;
    }

    public void run() {
        while (!shutdown) {
            log.info("Building heartbeat...");

            Heartbeat heartbeat = new Heartbeat();
            heartbeat.setAgentId(agentId);
            heartbeat.setHealthy(true);

            Inventory inventory = buildTsdtvInventory();
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

            try {
                tsdBotClient.sendTsdtvAgentHeartbeat(heartbeat);
            } catch (Exception e ) {
                log.error("Error sending heartbeat", e);
            }

            try {
                Thread.sleep(PERIOD_MILLIS);
            } catch (InterruptedException e) {
                log.error("Interrupted", e);
                shutdown = true;
            }
        }
    }

    private Inventory buildTsdtvInventory() {
        Inventory inventory = new Inventory();
        for (File file : listFilesAlphabetically(inventoryDirectory)) {
            log.info("Evaluating file: {}", file);
            if (file.isDirectory()) {
                try {
                    Series series = compileSeries(file);
                    inventory.getSeries().add(series);
                } catch (Exception e) {
                    log.error("Error building series from directory: "+file.getAbsolutePath(), e);
                }
            } else {
                try {
                    Movie movie = buildMovie(file);
                    inventory.getMovies().add(movie);
                } catch (Exception e) {
                    log.error("Error building movie from file: "+file.getAbsolutePath(), e);
                }
            }
        }

        log.info("Built TSDTV inventory: {}", inventory);
        return inventory;
    }

    private Series compileSeries(File seriesDirectory) {
        log.info("Compiling series: {}", seriesDirectory.getAbsolutePath());
        Series series = new Series();
        series.setName(seriesDirectory.getName());

        List<File> files = listFilesAlphabetically(seriesDirectory);
        int episodeNumber = 0;
        for (File file : files) {
            log.info("Evaluating file: {}", file);
            if (file.isDirectory()) {
                Season season = compileSeason(file);
                series.getSeasons().add(season);
            } else {
                episodeNumber++;
                try {
                    Episode episode = buildEpisode(file, episodeNumber);
                    series.getEpisodes().add(episode);
                } catch (Exception e) {
                    log.error("Error building episode for series: "+file.getAbsolutePath(), e);
                }
            }
        }

        return series;
    }

    private Season compileSeason(File seasonDirectory) {
        log.info("Compiling season: {}", seasonDirectory.getAbsolutePath());
        Season season = new Season();
        season.setName(seasonDirectory.getName());

        List<File> files = listFilesAlphabetically(seasonDirectory);
        int episodeNumber = 0;
        for (File file : files) {
            if (!file.isDirectory()) {
                episodeNumber++;
                try {
                    Episode episode = buildEpisode(file, episodeNumber);
                    season.getEpisodes().add(episode);
                } catch (Exception e) {
                    log.error("Error building episode for season: "+file.getAbsolutePath(), e);
                }
            }
        }

        return season;
    }

    private Episode buildEpisode(File file, int episodeNumber) throws IOException {
        Episode episode = new Episode();
        episode.setName(file.getName());
        episode.setEpisodeNumber(episodeNumber);
        episode.setMediaInfo(getMediaInfo(file));
        log.info("Built episode, file={}, episodeNumber={}: {}",
                file.getAbsolutePath(), episodeNumber, episode);
        return episode;
    }

    private Movie buildMovie(File file) throws IOException {
        Movie movie = new Movie();
        movie.setName(file.getName());
        movie.setMediaInfo(getMediaInfo(file));
        log.info("Built movie, file={}: {}", file.getAbsolutePath(), movie);
        return movie;
    }

    private MediaInfo getMediaInfo(File file) throws IOException {
        log.info("Getting media info for file: {}", file);
        FFmpegProbeResult probeResult = fFprobe.probe(file.getAbsolutePath());
        FFmpegFormat format = probeResult.getFormat();

        MediaInfo mediaInfo = new MediaInfo();
        mediaInfo.setFilePath(file.getAbsolutePath());
        mediaInfo.setFileSize(format.size);
        mediaInfo.setDurationSeconds((int)format.duration);
        mediaInfo.setBitRate(format.bit_rate);

        for (FFmpegStream fFmpegStream : probeResult.getStreams()) {
            if (fFmpegStream.codec_type != null) {
                switch (fFmpegStream.codec_type) {
                    case VIDEO: {
                        VideoStream videoStream = new VideoStream();
                        populateStreamInfo(videoStream, fFmpegStream);
                        videoStream.setWidth(fFmpegStream.width);
                        videoStream.setHeight(fFmpegStream.height);
                        videoStream.setSampleAspectRatio(fFmpegStream.sample_aspect_ratio);
                        videoStream.setDisplayAspectRatio(fFmpegStream.display_aspect_ratio);
                        videoStream.setPixFmt(fFmpegStream.pix_fmt);
                        videoStream.setAvc(Boolean.parseBoolean(fFmpegStream.is_avc));
                        videoStream.setrFrameRate(fFmpegStream.r_frame_rate.doubleValue());
                        videoStream.setAvgFrameRate(fFmpegStream.avg_frame_rate.doubleValue());
                        log.info("Parsed video stream: {}", videoStream);
                        mediaInfo.getVideoStreams().add(videoStream);
                        break;
                    }
                    case AUDIO: {
                        AudioStream audioStream = new AudioStream();
                        populateStreamInfo(audioStream, fFmpegStream);
                        audioStream.setChannelLayout(fFmpegStream.channel_layout);
                        audioStream.setLanguage(detectLanguage(audioStream));
                        audioStream.setSampleRate(fFmpegStream.sample_rate);
                        log.info("Parsed audio stream: {}", audioStream);
                        mediaInfo.getAudioStreams().add(audioStream);
                        break;
                    }
                    case SUBTITLE: {
                        SubtitleStream subtitleStream = new SubtitleStream();
                        populateStreamInfo(subtitleStream, fFmpegStream);
                        subtitleStream.setLanguage(detectLanguage(subtitleStream));
                        log.info("Parsed subtitle stream: {}", subtitleStream);
                        mediaInfo.getSubtitleStreams().add(subtitleStream);
                        break;
                    }
                }
            }
        }

        log.info("Parsed media info: {}", mediaInfo);
        return mediaInfo;
    }

    private static void populateStreamInfo(org.tsd.rest.v1.tsdtv.stream.Stream tsdtvStream,
                                           FFmpegStream fFmpegStream) {
        tsdtvStream.setIndex(fFmpegStream.index);
        tsdtvStream.setCodecName(fFmpegStream.codec_name);
        if (fFmpegStream.tags != null) {
            tsdtvStream.setTags(new HashMap<>(fFmpegStream.tags));
        }
    }

    private static String detectLanguage(org.tsd.rest.v1.tsdtv.stream.Stream tsdtvStream) {
        return tsdtvStream.getTags().entrySet().stream()
                .filter(entry -> StringUtils.equalsIgnoreCase(entry.getKey(), LANGUAGE_TAG))
                .map(Map.Entry::getValue)
                .findAny().orElse(null);
    }

    private static List<File> listFilesAlphabetically(File directory) {
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException(directory.getAbsolutePath()+" is not a directory");
        }
        File[] files = directory.listFiles();
        if (files == null) {
            return new LinkedList<>();
        }
        return Arrays.stream(files)
                .sorted(Comparator.comparing(file -> file.getName().toLowerCase()))
                .collect(Collectors.toList());
    }
}
