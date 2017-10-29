package org.tsd.tsdbot.tsdtv.library;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.rest.v1.tsdtv.Episode;
import org.tsd.rest.v1.tsdtv.Media;
import org.tsd.rest.v1.tsdtv.Movie;
import org.tsd.rest.v1.tsdtv.Series;
import org.tsd.tsdbot.Constants;
import org.tsd.tsdbot.tsdtv.AgentRegistry;
import org.tsd.tsdbot.tsdtv.MediaNotFoundException;
import org.tsd.tsdbot.tsdtv.OnlineAgent;

import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class TSDTVLibrary {

    private static final Logger log = LoggerFactory.getLogger(TSDTVLibrary.class);

    private final AgentRegistry agentRegistry;
    private final String streamUrl;
    private final String tsdtvImagesBucket;
    private final AmazonS3 s3Client;

    // mediaId -> image data
    private final LoadingCache<Integer, byte[]> queueImageCache;

    @Inject
    public TSDTVLibrary(AgentRegistry agentRegistry,
                        AmazonS3 s3Client,
                        @Named(Constants.Annotations.S3_TSDTV_IMAGES_BUCKET) String tsdtvImagesBucket,
                        @Named(Constants.Annotations.TSDTV_STREAM_URL) String streamUrl) {
        this.agentRegistry = agentRegistry;
        this.streamUrl = streamUrl;
        this.tsdtvImagesBucket = tsdtvImagesBucket;
        this.s3Client = s3Client;

        this.queueImageCache = CacheBuilder.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .build(new CacheLoader<Integer, byte[]>() {
                    @Override
                    public byte[] load(Integer mediaId) throws Exception {
                        log.info("Loading TSDTV queue image: {} (bucket = {})", mediaId, tsdtvImagesBucket);

                        Media media = findMediaById(mediaId);
                        if (media == null) {
                            throw new MediaNotFoundException(mediaId);
                        }

                        String effectiveName;
                        if (media instanceof Movie) {
                            effectiveName = ((Movie)media).getName();
                        } else if (media instanceof Episode) {
                            effectiveName = ((Episode)media).getSeriesName();
                        } else {
                            throw new RuntimeException("Unknown media type: " + media);
                        }

                        log.info("Using effective media name: {}", effectiveName);

                        ListObjectsV2Result result = s3Client.listObjectsV2(tsdtvImagesBucket);
                        S3ObjectSummary match = result.getObjectSummaries().stream()
                                .filter(s3ObjectSummary -> {
                                    String filenameNoExtension = StringUtils.substringBeforeLast(s3ObjectSummary.getKey(), ".");
                                    log.debug("Filename in S3: {} -> {}", s3ObjectSummary.getKey(), filenameNoExtension);
                                    return StringUtils.equalsIgnoreCase(filenameNoExtension, effectiveName);
                                })
                                .findAny().orElse(null);

                        if (match == null) {
                            log.warn("Found no matching file");
                            return null;
                        }

                        log.info("Found matching file in S3: {}", match.getKey());
                        S3Object object = s3Client.getObject(tsdtvImagesBucket, match.getKey());
                        log.info("Retrieved file from S3: {}", object);
                        return IOUtils.toByteArray(object.getObjectContent());
                    }
                });
    }

    public TSDTVListing getListings() {
        TSDTVListing listing = new TSDTVListing();

        for (OnlineAgent onlineAgent : agentRegistry.getOnlineAgents()) {
            log.debug("Retrieving listings for agent: {}", onlineAgent);
            List<AgentMedia<Movie>> moviesForAgent
                    = onlineAgent.getInventory().getMovies()
                    .stream()
                    .map(movie -> new AgentMedia<>(movie, onlineAgent))
                    .collect(Collectors.toList());
            listing.getAllMovies().addAll(moviesForAgent);
            listing.getAllMovies().sort(Comparator.comparing(movie -> movie.getMedia().getName()));

            List<AgentMedia<Series>> seriesForAgent
                    = onlineAgent.getInventory().getSeries()
                    .stream()
                    .map(series -> new AgentMedia<>(series, onlineAgent))
                    .collect(Collectors.toList());
            listing.getAllSeries().addAll(seriesForAgent);
            listing.getAllSeries().sort(Comparator.comparing(series -> series.getMedia().getName()));
        }

        log.debug("Retrieved all listings: {}", listing);
        return listing;
    }

    public byte[] getQueueImage(int mediaId) throws IOException {
        try {
            return queueImageCache.get(mediaId);
        } catch (Exception e) {
            log.error("Error getting queue image for mediaId {}, using default");
            return IOUtils.toByteArray(getClass().getResourceAsStream("/tarehart.jpg"));
        }
    }

    public Media findMediaById(int mediaId) throws MediaNotFoundException {
        return findMediaById(null, mediaId);
    }

    public Media findMediaById(String agentId, int mediaId) throws MediaNotFoundException {
        log.info("Seaching for media, agentId={}, mediaId={}", agentId, mediaId);
        List<Media> allMedia = new LinkedList<>();
        TSDTVListing listing = getListings();
        allMedia.addAll(listing.getAllMovies().stream().map(AgentMedia::getMedia)
                .collect(Collectors.toList()));
        allMedia.addAll(listing.getAllSeries().stream().flatMap(series -> series.getMedia().getEpisodes().stream())
                .collect(Collectors.toList()));
        allMedia.addAll(listing.getAllSeries().stream().flatMap(series -> series.getMedia().getSeasons().stream().flatMap(season -> season.getEpisodes().stream()))
                .collect(Collectors.toList()));

        Stream<Media> mediaSearch = allMedia.stream()
                .filter(m -> m.getId() == mediaId);

        if (StringUtils.isNotBlank(agentId)) {
            mediaSearch = mediaSearch.filter(m -> m.getAgentId().equals(agentId));
        }

        Optional<Media> media = mediaSearch.findAny();

        if (!media.isPresent()) {
            throw new MediaNotFoundException(agentId, mediaId);
        }

        log.info("Found media, agentId={}, mediaId={} -> {}", agentId, mediaId, media.get());
        return media.get();
    }

    public String getStreamUrl() {
        return streamUrl;
    }
}
