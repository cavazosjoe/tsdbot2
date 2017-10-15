package org.tsd.rest.v1.tsdtv;

import java.util.LinkedList;
import java.util.List;

public class Inventory {

    private List<Movie> movies = new LinkedList<>();
    private List<Series> series = new LinkedList<>();

    public List<Movie> getMovies() {
        return movies;
    }

    public void setMovies(List<Movie> movies) {
        this.movies = movies;
    }

    public List<Series> getSeries() {
        return series;
    }

    public void setSeries(List<Series> series) {
        this.series = series;
    }
}
