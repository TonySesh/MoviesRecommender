package org.example.movieSettings;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class LikedMoviesPanel extends MoviesPanel {

    public LikedMoviesPanel(MovieDAO dao, int userId, List<MoviesPanel> panels) {
        super(dao, userId, panels);
        loadFirstPage();
    }

    @Override
    protected String getPanelTitle() {
        return "Любимые фильмы";
    }

    @Override
    protected List<Movie> loadMovies(int page, int pageSize) {
        return movieDAO.getLikedMovies(userId, page, pageSize);
    }

    @Override
    protected int getTotalCount() {
        return movieDAO.getTotalLikedCount(userId);
    }

    @Override
    protected String getEmptyMessage() {
        return "У вас пока нет любимых фильмов";
    }

    @Override
    protected Color getAccentColor() {
        return likeColor;
    }

    @Override
    protected void handleLike(Movie movie, JButton likeBtn, JButton dislikeBtn) {
        boolean newLikeState = !movie.isLiked();

        if (!newLikeState) {
            movieDAO.toggleLike(userId, movie.getId(), false);
            movie.setLiked(false);
            model.handleNewLike(userId, movie.getId());
            loadFirstPage();
        }

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
            model.handleNewLike(userId, movie.getId());
            loadFirstPage();
        }

        movieDAO.toggleDislike(userId, movie.getId(), newDislikeState);
        movie.setDisliked(newDislikeState);
        model.handleNewDislike(userId, movie.getId());

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
        movieDAO.toggleWatched(userId, movie.getId(), newState);
        movie.setWatched(newState);
        model.handleNewWatch(userId, movie.getId());

        watchedBtn.setBackground(newState ? watchedColor : defaultColor);

        if (parentProgram != null) {
            parentProgram.refreshAllPanelsForMovie(movie.getId());
        }
    }
}