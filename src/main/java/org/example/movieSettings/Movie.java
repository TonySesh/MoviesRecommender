package org.example.movieSettings;

public class Movie {
    private final int id;
    private final String title;
    private final String genres;
    private final double rating;
    private final int year;
    private final String overview;
    private boolean isLiked = false;
    private boolean isWatched = false;
    private boolean isDisliked = false;
    private String[] actors;
    private int runtime;
    private double popularity;

    public Movie(int id, String title, String genres, double rating, int year,
                 String overview) {
        this.id = id;
        this.title = title;
        this.genres = genres;
        this.rating = rating;
        this.year = year;
        this.overview = overview;
    }

    // Getters
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getGenres() { return genres; }
    public double getRating() { return rating; }
    public int getYear() { return year; }
    public String getOverview() { return overview; }
    public boolean isLiked() { return isLiked; }
    public boolean isWatched() { return isWatched; }
    public boolean isDisliked() { return isDisliked; }
    public String[] getActors() { return actors; }
    public int getRuntime() { return runtime; }
    public double getPopularity() { return popularity; }

    // Setters
    public void setLiked(boolean liked) { isLiked = liked; }
    public void setWatched(boolean watched) { isWatched = watched; }
    public void setDisliked(boolean disliked) { isDisliked = disliked; }
    public void setActors(String[] actors) { this.actors = actors; }
    public void setRuntime(int runtime) { this.runtime = runtime; }
    public void setPopularity(double popularity) { this.popularity = popularity; }

    @Override
    public String toString() {
        return title + " (" + (year > 0 ? year : "unknown") + ") - " + rating;
    }
}