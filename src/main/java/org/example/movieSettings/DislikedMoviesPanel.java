package org.example.movieSettings;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class DislikedMoviesPanel extends MoviesPanel {

    public DislikedMoviesPanel(MovieDAO dao, int userId, List<MoviesPanel> panels) {
        super(dao, userId, panels);
        loadFirstPage();
    }

    @Override
    protected String getPanelTitle() {
        return "Не понравившиеся фильмы";
    }

    @Override
    protected List<Movie> loadMovies(int page, int pageSize) {
        return movieDAO.getDislikedMovies(userId, page, pageSize);
    }

    @Override
    protected int getTotalCount() {
        return movieDAO.getTotalDislikedCount(userId);
    }

    @Override
    protected String getEmptyMessage() {
        return "У вас пока нет фильмов, которые вам не понравились";
    }

    @Override
    protected Color getAccentColor() {
        return dislikeColor;
    }

    @Override
    protected void handleLike(Movie movie, JButton likeBtn, JButton dislikeBtn) {
        boolean newLikeState = !movie.isLiked();

        if (newLikeState && movie.isDisliked()) {
            movieDAO.toggleDislike(userId, movie.getId(), false);
            movie.setDisliked(false);
            model.handleNewDislike(userId, movie.getId());
            loadFirstPage();
        }

        movieDAO.toggleLike(userId, movie.getId(), newLikeState);
        movie.setLiked(newLikeState);
        model.handleNewLike(userId, movie.getId());

        if (parentProgram != null) {
            parentProgram.refreshAllPanelsForMovie(movie.getId());
        }
    }

    @Override
    protected void handleDislike(Movie movie, JButton likeBtn, JButton dislikeBtn) {
        boolean newDislikeState = !movie.isDisliked();

        if (!newDislikeState) {
            movieDAO.toggleDislike(userId, movie.getId(), false);
            movie.setDisliked(false);
            model.handleNewDislike(userId, movie.getId());
            loadFirstPage();
        }

        if (parentProgram != null) {
            parentProgram.refreshAllPanelsForMovie(movie.getId());
        }
    }

    @Override
    protected void handleWatch(Movie movie, JButton watchedBtn) {
        boolean newState = !movie.isWatched();
        movieDAO.toggleWatched(userId, movie.getId(), newState);
        movie.setWatched(newState);
        model.handleNewWatch(userId, movie.getId());

        watchedBtn.setBackground(newState ? watchedColor : defaultColor);

        if (parentProgram != null) {
            parentProgram.refreshAllPanelsForMovie(movie.getId());
        }
    }
}