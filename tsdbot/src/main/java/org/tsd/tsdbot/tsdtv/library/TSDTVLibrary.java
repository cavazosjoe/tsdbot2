package org.tsd.tsdbot.tsdtv.library;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.tsd.rest.v1.tsdtv.Movie;
import org.tsd.rest.v1.tsdtv.Series;
import org.tsd.tsdbot.tsdtv.AgentRegistry;
import org.tsd.tsdbot.tsdtv.OnlineAgent;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class TSDTVLibrary {

    private final AgentRegistry agentRegistry;

    @Inject
    public TSDTVLibrary(AgentRegistry agentRegistry) {
        this.agentRegistry = agentRegistry;
    }

    public TSDTVListing getListings() {
        TSDTVListing listing = new TSDTVListing();

        for (OnlineAgent onlineAgent : agentRegistry.getOnlineAgents()) {
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

        return listing;
    }
}
