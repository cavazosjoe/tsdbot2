package org.tsd.rest.v1.tsdtv;

import java.util.LinkedList;
import java.util.List;

public class Series {

    private String name;
    private List<Season> seasons = new LinkedList<>();
    private List<Episode> episodes = new LinkedList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Season> getSeasons() {
        return seasons;
    }

    public void setSeasons(List<Season> seasons) {
        this.seasons = seasons;
    }

    public List<Episode> getEpisodes() {
        return episodes;
    }

    public void setEpisodes(List<Episode> episodes) {
        this.episodes = episodes;
    }
}
