package org.example.models;

import org.apache.mahout.math.RandomAccessSparseVector;
import org.apache.mahout.math.Vector;
import org.example.movieSettings.Movie;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

 // Преобразует фильмы в векторы признаков для обучения

public class MovieFeatureEncoder {

    private static final int VECTOR_SIZE = 1000; // размерность вектора

    // Маппинг жанров (one-hot encoding)
    private static final String[] GENRES = {
            "Action", "Adventure", "Animation", "Comedy", "Crime",
            "Documentary", "Drama", "Family", "Fantasy", "History",
            "Horror", "Music", "Mystery", "Romance", "Science Fiction",
            "TV Movie", "Thriller", "War", "Western"
    };

     // Превращает фильм в вектор признаков

    public static Vector encodeMovie(Movie movie) {
        Vector vector = new RandomAccessSparseVector(VECTOR_SIZE);

        // Кодируем жанры (one-hot)
        List<String> movieGenres = parseGenresList(movie.getGenres());
        for (String genre : movieGenres) {
            int index = getGenreIndex(genre);
            if (index >= 0) {
                vector.set(index, 1.0);
            }
        }

        // Кодируем год выпуска (нормализованный)
        if (movie.getYear() > 0) {
            double normalizedYear = (movie.getYear() - 1900) / 200.0;
            vector.set(100, normalizedYear);
        }

        // Кодируем рейтинг (нормализованный)
        double normalizedRating = movie.getRating() / 10.0;
        vector.set(101, normalizedRating);

        // Кодируем популярность
        vector.set(102, movie.getPopularity() / 100.0);

        return vector;
    }

     //Получить индекс жанра (для one-hot encoding)
    private static int getGenreIndex(String genre) {
        for (int i = 0; i < GENRES.length; i++) {
            if (GENRES[i].equalsIgnoreCase(genre)) {
                return i;
            }
        }
        return -1;
    }

     //Парсинг жанров из JSON в список строк
    public static List<String> parseGenresList(String genresJson) {
        List<String> genres = new ArrayList<>();
        if (genresJson == null || genresJson.isEmpty() ||
                genresJson.equals("null") || genresJson.equals("[]")) {
            return genres;
        }

        Pattern pattern = Pattern.compile("\"name\":\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(genresJson);
        while (matcher.find()) {
            genres.add(matcher.group(1));
        }
        return genres;
    }
}