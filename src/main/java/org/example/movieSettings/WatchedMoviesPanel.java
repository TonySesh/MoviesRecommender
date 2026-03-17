package org.example.movieSettings;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class WatchedMoviesPanel extends MoviesPanel {

    public WatchedMoviesPanel(MovieDAO dao, int userId, List<MoviesPanel> panels) {
        super(dao, userId, panels);
        loadFirstPage();
    }

    @Override
    protected String getPanelTitle() {
        return "Просмотренные фильмы";
    }

    @Override
    protected List<Movie> loadMovies(int page, int pageSize) {
        return movieDAO.getWatchedMovies(userId, page, pageSize);
    }

    @Override
    protected int getTotalCount() {
        return movieDAO.getTotalWatchedCount(userId);
    }

    @Override
    protected String getEmptyMessage() {
        return "У вас пока нет просмотренных фильмов";
    }

    @Override
    protected Color getAccentColor() {
        return watchedColor;
    }

    @Override
    protected void handleLike(Movie movie, JButton likeBtn, JButton dislikeBtn) {
        boolean newLikeState = !movie.isLiked();

        if (newLikeState && movie.isDisliked()) {
            movieDAO.toggleDislike(userId, movie.getId(), false);
            movie.setDisliked(false);
            model.handleNewDislike(userId, movie.getId());
        }

        movieDAO.toggleLike(userId, movie.getId(), newLikeState);
        movie.setLiked(newLikeState);
        model.handleNewLike(userId, movie.getId());

        likeBtn.setBackground(newLikeState ? likeColor : defaultColor);
        dislikeBtn.setBackground(movie.isDisliked() ? dislikeColor : defaultColor);

        if (parentProgram != null) {
            parentProgram.refreshAllPanelsForMovie(movie.getId());
        }
    }

    @Override
    protected void handleDislike(Movie movie, JButton likeBtn, JButton dislikeBtn) {
        boolean newDislikeState = !movie.isDisliked();

        if (newDislikeState && movie.isLiked()) {
            movieDAO.toggleLike(userId, movie.getId(), false);
            movie.setLiked(false);
            likeBtn.setBackground(defaultColor);
            model.handleNewLike(userId, movie.getId());
        }

        movieDAO.toggleDislike(userId, movie.getId(), newDislikeState);
        movie.setDisliked(newDislikeState);
        model.handleNewDislike(userId, movie.getId());

        dislikeBtn.setBackground(newDislikeState ? dislikeColor : defaultColor);

        if (parentProgram != null) {
            parentProgram.refreshAllPanelsForMovie(movie.getId());
        }

        if (dislikedPanel != null) {
            dislikedPanel.loadFirstPage();
        }
    }

    @Override
    protected void handleWatch(Movie movie, JButton watchedBtn) {
        boolean newState = !movie.isWatched();

        if (!newState) {
            movieDAO.toggleWatched(userId, movie.getId(), false);
            movie.setWatched(false);
            model.handleNewWatch(userId, movie.getId());
            loadFirstPage();
        } else {
            movieDAO.toggleWatched(userId, movie.getId(), true);
            movie.setWatched(true);
            model.handleNewWatch(userId, movie.getId());
            watchedBtn.setBackground(watchedColor);
        }

        if (parentProgram != null) {
            parentProgram.refreshAllPanelsForMovie(movie.getId());
        }
    }
}