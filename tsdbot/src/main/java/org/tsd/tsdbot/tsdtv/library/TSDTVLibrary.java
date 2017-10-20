package org.tsd.tsdbot.tsdtv.library;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.rest.v1.tsdtv.Media;
import org.tsd.rest.v1.tsdtv.Movie;
import org.tsd.rest.v1.tsdtv.Series;
import org.tsd.tsdbot.Constants;
import org.tsd.tsdbot.tsdtv.AgentRegistry;
import org.tsd.tsdbot.tsdtv.MediaNotFoundException;
import org.tsd.tsdbot.tsdtv.OnlineAgent;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
public class TSDTVLibrary {

    private static final Logger log = LoggerFactory.getLogger(TSDTVLibrary.class);

    private final AgentRegistry agentRegistry;
    private final String streamUrl;

    @Inject
    public TSDTVLibrary(AgentRegistry agentRegistry,
                        @Named(Constants.Annotations.TSDTV_STREAM_URL) String streamUrl) {
        this.agentRegistry = agentRegistry;
        this.streamUrl = streamUrl;
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

    public Media findMediaById(String agentId, int mediaId) throws MediaNotFoundException {
        List<Media> allMedia = new LinkedList<>();
        TSDTVListing listing = getListings();
        allMedia.addAll(listing.getAllMovies().stream().map(AgentMedia::getMedia)
                .collect(Collectors.toList()));
        allMedia.addAll(listing.getAllSeries().stream().flatMap(series -> series.getMedia().getEpisodes().stream())
                .collect(Collectors.toList()));
        allMedia.addAll(listing.getAllSeries().stream().flatMap(series -> series.getMedia().getSeasons().stream().flatMap(season -> season.getEpisodes().stream()))
                .collect(Collectors.toList()));
        Optional<Media> media = allMedia.stream()
                .filter(m -> m.getAgentId().equals(agentId))
                .filter(m -> m.getId() == mediaId)
                .findAny();
        if (!media.isPresent()) {
            throw new MediaNotFoundException(agentId, mediaId);
        }
        return media.get();
    }

    public String getStreamUrl() {
        return streamUrl;
    }
}
