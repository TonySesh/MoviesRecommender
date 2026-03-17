package org.example.movieSettings;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class AllMoviesPanel extends MoviesPanel {

    public AllMoviesPanel(MovieDAO dao, int userId, List<MoviesPanel> panels) {
        super(dao, userId, panels);
        loadFirstPage();
    }

    @Override
    protected String getPanelTitle() {
        return "Все фильмы";
    }

    @Override
    protected List<Movie> loadMovies(int page, int pageSize) {
        String genre = genreField.getText().trim();
        double rating = 0;
        try {
            rating = Double.parseDouble(ratingField.getText());
        } catch (NumberFormatException e) {
            rating = 0;
        }

        return movieDAO.getMoviesPage(
                genre.isEmpty() ? null : genre,
                rating,
                page,
                pageSize,
                userId
        );
    }

    @Override
    protected int getTotalCount() {
        String genre = genreField.getText().trim();
        double rating = 0;
        try {
            rating = Double.parseDouble(ratingField.getText());
        } catch (NumberFormatException e) {
            rating = 0;
        }

        return movieDAO.getFilteredMoviesCount(
                genre.isEmpty() ? null : genre,
                rating
        );
    }

    @Override
    protected String getEmptyMessage() {
        return "Нет фильмов для отображения";
    }

    @Override
    protected Color getAccentColor() {
        return new Color(110, 70, 140);
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
        movieDAO.toggleWatched(userId, movie.getId(), newState);
        movie.setWatched(newState);
        model.handleNewWatch(userId, movie.getId());

        watchedBtn.setBackground(newState ? watchedColor : defaultColor);

        if (parentProgram != null) {
            parentProgram.refreshAllPanelsForMovie(movie.getId());
        }
    }
}