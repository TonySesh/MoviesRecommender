package org.example.movieSettings;

import org.example.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.*;

public class MovieDAO {

     // Получить фильм по ID

    public Movie getMovieById(int movieId) {
        String sql = "SELECT movie_id, title, genres, overview, rating, " +
                "actors_json, keywords_json, original_language, runtime, popularity, " +
                "year FROM movies_final WHERE CAST(movie_id AS TEXT) = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, String.valueOf(movieId));
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return extractMovieFromResultSet(rs);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

     //Получить все фильмы с пагинацией (для вкладки "Все фильмы" без фильтров)

    public List<Movie> getAllMoviesPage(int page, int pageSize, int userId) {
        List<Movie> movies = new ArrayList<>();
        int offset = (page - 1) * pageSize;

        String sql = "SELECT m.movie_id, m.title, m.genres, CAST(m.rating AS FLOAT) as rating, " +
                "m.year, m.overview, m.actors_json, " +
                "CAST(m.popularity AS FLOAT) as popularity, " +
                "m.original_language, m.runtime, " +
                "CASE WHEN l.user_id IS NOT NULL THEN true ELSE false END as is_liked, " +
                "CASE WHEN w.user_id IS NOT NULL THEN true ELSE false END as is_watched, " +
                "CASE WHEN d.user_id IS NOT NULL THEN true ELSE false END as is_disliked " +
                "FROM movies_final m " +
                "LEFT JOIN liked l ON CAST(m.movie_id AS TEXT) = CAST(l.movie_id AS TEXT) AND l.user_id = ? " +
                "LEFT JOIN watched w ON CAST(m.movie_id AS TEXT) = CAST(w.movie_id AS TEXT) AND w.user_id = ? " +
                "LEFT JOIN disliked d ON CAST(m.movie_id AS TEXT) = CAST(d.movie_id AS TEXT) AND d.user_id = ? " +
                "WHERE m.movie_id ~ '^[0-9]+$' " +  // Только числовые ID
                "ORDER BY popularity DESC NULLS LAST LIMIT ? OFFSET ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setInt(3, userId);
            stmt.setInt(4, pageSize);
            stmt.setInt(5, offset);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                try {
                    String movieIdStr = rs.getString("movie_id");
                    int movieId;
                    try {
                        movieId = Integer.parseInt(movieIdStr);
                    } catch (NumberFormatException e) {
                        continue; // Пропускаем некорректные ID без вывода ошибки
                    }

                    Movie movie = new Movie(
                            movieId,
                            rs.getString("title"),
                            rs.getString("genres"),
                            rs.getDouble("rating"),
                            rs.getInt("year"),
                            rs.getString("overview")
                    );

                    movie.setLiked(rs.getBoolean("is_liked"));
                    movie.setWatched(rs.getBoolean("is_watched"));
                    movie.setDisliked(rs.getBoolean("is_disliked"));

                    // Добавляем дополнительные поля
                    movie.setPopularity(rs.getDouble("popularity"));
                    movie.setRuntime(rs.getInt("runtime"));

                    String actorsJson = rs.getString("actors_json");
                    if (actorsJson != null && !actorsJson.isEmpty()) {
                        movie.setActors(parseActorsFromJson(actorsJson));
                    }

                    movies.add(movie);
                } catch (SQLException e) {
                    // Игнорируем ошибки конкретного фильма
                }
            }

        } catch (SQLException e) {
            System.err.println("Ошибка в getAllMoviesPage: " + e.getMessage());
        }

        return movies;
    }

     // Получить количество фильмов с фильтром

    public int getFilteredMoviesCount(String genreFilter, double minRating) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) as total FROM movies_final WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (genreFilter != null && !genreFilter.isEmpty()) {
            sql.append(" AND genres ILIKE ?");
            params.add("%" + genreFilter + "%");
        }
        if (minRating > 0) {
            sql.append(" AND CAST(rating AS FLOAT) >= ?");
            params.add(minRating);
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof Double) {
                    stmt.setDouble(i + 1, (Double) param);
                } else if (param instanceof String) {
                    stmt.setString(i + 1, (String) param);
                }
            }

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("total");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    // МЕТОДЫ С ФИЛЬТРАЦИЕЙ

     // Получить фильмы с фильтрацией по жанру и рейтингу

    public List<Movie> getMoviesPage(String genreFilter, double minRating, int page, int pageSize, int userId) {
        List<Movie> movies = new ArrayList<>();
        int offset = (page - 1) * pageSize;

        StringBuilder sql = new StringBuilder(
                "SELECT m.movie_id, m.title, m.genres, m.rating, " +
                        "m.year, m.overview, m.actors_json, " +
                        "CASE WHEN l.user_id IS NOT NULL THEN true ELSE false END as is_liked, " +
                        "CASE WHEN w.user_id IS NOT NULL THEN true ELSE false END as is_watched, " +
                        "CASE WHEN d.user_id IS NOT NULL THEN true ELSE false END as is_disliked " +
                        "FROM movies_final m " +
                        "LEFT JOIN liked l ON CAST(m.movie_id AS TEXT) = CAST(l.movie_id AS TEXT) AND l.user_id = ? " +
                        "LEFT JOIN watched w ON CAST(m.movie_id AS TEXT) = CAST(w.movie_id AS TEXT) AND w.user_id = ? " +
                        "LEFT JOIN disliked d ON CAST(m.movie_id AS TEXT) = CAST(d.movie_id AS TEXT) AND d.user_id = ? " +
                        "WHERE 1=1"
        );

        List<Object> params = new ArrayList<>();
        params.add(userId);
        params.add(userId);
        params.add(userId);

        if (genreFilter != null && !genreFilter.isEmpty()) {
            sql.append(" AND m.genres ILIKE ?");
            params.add("%" + genreFilter + "%");
        }
        if (minRating > 0) {
            sql.append(" AND CAST(m.rating AS FLOAT) >= ?");
            params.add(minRating);
        }

        sql.append(" ORDER BY CAST(m.popularity AS FLOAT) DESC NULLS LAST LIMIT ? OFFSET ?");
        params.add(pageSize);
        params.add(offset);

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof Integer) {
                    stmt.setInt(i + 1, (Integer) param);
                } else if (param instanceof Double) {
                    stmt.setDouble(i + 1, (Double) param);
                } else if (param instanceof String) {
                    stmt.setString(i + 1, (String) param);
                }
            }

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                try {
                    String movieIdStr = rs.getString("movie_id");
                    int movieId;
                    try {
                        movieId = Integer.parseInt(movieIdStr);
                    } catch (NumberFormatException e) {
                        continue;
                    }

                    Movie movie = new Movie(
                            movieId,
                            rs.getString("title"),
                            rs.getString("genres"),
                            rs.getDouble("rating"),
                            rs.getInt("year"),
                            rs.getString("overview")
                    );

                    movie.setLiked(rs.getBoolean("is_liked"));
                    movie.setWatched(rs.getBoolean("is_watched"));
                    movie.setDisliked(rs.getBoolean("is_disliked"));

                    String actorsJson = rs.getString("actors_json");
                    if (actorsJson != null && !actorsJson.isEmpty()) {
                        movie.setActors(parseActorsFromJson(actorsJson));
                    }

                    movies.add(movie);
                } catch (SQLException e) {
                    System.err.println("Ошибка обработки фильма: " + e.getMessage());
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return movies;
    }

    //ЛАЙКИ, ПРОСМОТРЫ И ДИЗЛАЙКИ

     // Поставить/убрать лайк

    public void toggleLike(int userId, int movieId, boolean like) {
        String sql = like ?
                "INSERT INTO liked (user_id, movie_id) VALUES (?, ?) ON CONFLICT (user_id, movie_id) DO NOTHING" :
                "DELETE FROM liked WHERE user_id = ? AND movie_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setInt(2, movieId);
            pstmt.executeUpdate();
            System.out.println("Лайк " + (like ? "добавлен" : "удален") +
                    " (user=" + userId + ", movie=" + movieId + ")");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

     // Поставить/убрать просмотр

    public void toggleWatched(int userId, int movieId, boolean watched) {
        String sql = watched ?
                "INSERT INTO watched (user_id, movie_id) VALUES (?, ?) ON CONFLICT (user_id, movie_id) DO NOTHING" :
                "DELETE FROM watched WHERE user_id = ? AND movie_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setInt(2, movieId);
            pstmt.executeUpdate();
            System.out.println("Просмотр " + (watched ? "добавлен" : "удален") +
                    " (user=" + userId + ", movie=" + movieId + ")");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

     // Поставить/убрать дизлайк

    public void toggleDislike(int userId, int movieId, boolean dislike) {
        String sql = dislike ?
                "INSERT INTO disliked (user_id, movie_id) VALUES (?, ?) ON CONFLICT (user_id, movie_id) DO NOTHING" :
                "DELETE FROM disliked WHERE user_id = ? AND movie_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setInt(2, movieId);
            pstmt.executeUpdate();
            System.out.println("Дизлайк " + (dislike ? "добавлен" : "удален") +
                    " (user=" + userId + ", movie=" + movieId + ")");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

     //Получить лайкнутые фильмы пользователя

    public List<Movie> getLikedMovies(int userId, int page, int pageSize) {
        List<Movie> movies = new ArrayList<>();
        int offset = (page - 1) * pageSize;

        String sql = "SELECT m.movie_id, m.title, m.genres, m.rating, " +
                "m.year, m.overview, m.actors_json, " +
                "true as is_liked, " +
                "CASE WHEN w.user_id IS NOT NULL THEN true ELSE false END as is_watched, " +
                "CASE WHEN d.user_id IS NOT NULL THEN true ELSE false END as is_disliked " +
                "FROM movies_final m " +
                "JOIN liked l ON CAST(m.movie_id AS TEXT) = CAST(l.movie_id AS TEXT) " +
                "LEFT JOIN watched w ON CAST(m.movie_id AS TEXT) = CAST(w.movie_id AS TEXT) AND w.user_id = ? " +
                "LEFT JOIN disliked d ON CAST(m.movie_id AS TEXT) = CAST(d.movie_id AS TEXT) AND d.user_id = ? " +
                "WHERE l.user_id = ? " +
                "ORDER BY l.liked_date DESC LIMIT ? OFFSET ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setInt(3, userId);
            stmt.setInt(4, pageSize);
            stmt.setInt(5, offset);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                try {
                    String movieIdStr = rs.getString("movie_id");
                    int movieId = Integer.parseInt(movieIdStr);

                    Movie movie = new Movie(
                            movieId,
                            rs.getString("title"),
                            rs.getString("genres"),
                            rs.getDouble("rating"),
                            rs.getInt("year"),
                            rs.getString("overview")
                    );

                    movie.setLiked(true);
                    movie.setWatched(rs.getBoolean("is_watched"));
                    movie.setDisliked(rs.getBoolean("is_disliked"));

                    String actorsJson = rs.getString("actors_json");
                    if (actorsJson != null && !actorsJson.isEmpty()) {
                        movie.setActors(parseActorsFromJson(actorsJson));
                    }

                    movies.add(movie);
                } catch (NumberFormatException e) {
                    System.err.println("Пропущен фильм с некорректным ID в лайках");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return movies;
    }

     // Получить просмотренные фильмы пользователя

    public List<Movie> getWatchedMovies(int userId, int page, int pageSize) {
        List<Movie> movies = new ArrayList<>();
        int offset = (page - 1) * pageSize;

        String sql = "SELECT m.movie_id, m.title, m.genres, m.rating, " +
                "m.year, m.overview, m.actors_json, " +
                "CASE WHEN l.user_id IS NOT NULL THEN true ELSE false END as is_liked, " +
                "true as is_watched, " +
                "CASE WHEN d.user_id IS NOT NULL THEN true ELSE false END as is_disliked " +
                "FROM movies_final m " +
                "JOIN watched w ON CAST(m.movie_id AS TEXT) = CAST(w.movie_id AS TEXT) " +
                "LEFT JOIN liked l ON CAST(m.movie_id AS TEXT) = CAST(l.movie_id AS TEXT) AND l.user_id = ? " +
                "LEFT JOIN disliked d ON CAST(m.movie_id AS TEXT) = CAST(d.movie_id AS TEXT) AND d.user_id = ? " +
                "WHERE w.user_id = ? " +
                "ORDER BY w.watched_date DESC LIMIT ? OFFSET ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setInt(3, userId);
            stmt.setInt(4, pageSize);
            stmt.setInt(5, offset);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                try {
                    String movieIdStr = rs.getString("movie_id");
                    int movieId = Integer.parseInt(movieIdStr);

                    Movie movie = new Movie(
                            movieId,
                            rs.getString("title"),
                            rs.getString("genres"),
                            rs.getDouble("rating"),
                            rs.getInt("year"),
                            rs.getString("overview")
                    );

                    movie.setLiked(rs.getBoolean("is_liked"));
                    movie.setWatched(true);
                    movie.setDisliked(rs.getBoolean("is_disliked"));

                    String actorsJson = rs.getString("actors_json");
                    if (actorsJson != null && !actorsJson.isEmpty()) {
                        movie.setActors(parseActorsFromJson(actorsJson));
                    }

                    movies.add(movie);
                } catch (NumberFormatException e) {
                    System.err.println("Пропущен фильм с некорректным ID в просмотренных");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return movies;
    }

     // Получить дизлайкнутые фильмы пользователя

    public List<Movie> getDislikedMovies(int userId, int page, int pageSize) {
        List<Movie> movies = new ArrayList<>();
        int offset = (page - 1) * pageSize;

        String sql = "SELECT m.movie_id, m.title, m.genres, m.rating, " +
                "m.year, m.overview, m.actors_json, " +
                "CASE WHEN l.user_id IS NOT NULL THEN true ELSE false END as is_liked, " +
                "CASE WHEN w.user_id IS NOT NULL THEN true ELSE false END as is_watched, " +
                "true as is_disliked " +
                "FROM movies_final m " +
                "JOIN disliked d ON CAST(m.movie_id AS TEXT) = CAST(d.movie_id AS TEXT) " +
                "LEFT JOIN liked l ON CAST(m.movie_id AS TEXT) = CAST(l.movie_id AS TEXT) AND l.user_id = ? " +
                "LEFT JOIN watched w ON CAST(m.movie_id AS TEXT) = CAST(w.movie_id AS TEXT) AND w.user_id = ? " +
                "WHERE d.user_id = ? " +
                "ORDER BY d.disliked_date DESC LIMIT ? OFFSET ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setInt(3, userId);
            stmt.setInt(4, pageSize);
            stmt.setInt(5, offset);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                try {
                    String movieIdStr = rs.getString("movie_id");
                    int movieId = Integer.parseInt(movieIdStr);

                    Movie movie = new Movie(
                            movieId,
                            rs.getString("title"),
                            rs.getString("genres"),
                            rs.getDouble("rating"),
                            rs.getInt("year"),
                            rs.getString("overview")
                    );

                    movie.setLiked(rs.getBoolean("is_liked"));
                    movie.setWatched(rs.getBoolean("is_watched"));
                    movie.setDisliked(true);

                    String actorsJson = rs.getString("actors_json");
                    if (actorsJson != null && !actorsJson.isEmpty()) {
                        movie.setActors(parseActorsFromJson(actorsJson));
                    }

                    movies.add(movie);
                } catch (NumberFormatException e) {
                    System.err.println("Пропущен фильм с некорректным ID в дизлайках");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return movies;
    }

//     Получить количество лайкнутых фильмов пользователя

    public int getTotalLikedCount(int userId) {
        String sql = "SELECT COUNT(*) as total FROM liked WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("total");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

//     Получить количество просмотренных фильмов пользователя

    public int getTotalWatchedCount(int userId) {
        String sql = "SELECT COUNT(*) as total FROM watched WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("total");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

//     Получить количество дизлайкнутых фильмов пользователя

    public int getTotalDislikedCount(int userId) {
        String sql = "SELECT COUNT(*) as total FROM disliked WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("total");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

//     Создать объект Movie из ResultSet

    private Movie extractMovieFromResultSet(ResultSet rs) throws SQLException {
        int year = rs.getInt("year");
        if (rs.wasNull()) year = 0;

        String movieIdStr = rs.getString("movie_id");
        int movieId;
        try {
            movieId = Integer.parseInt(movieIdStr);
        } catch (NumberFormatException e) {
            throw new SQLException("Некорректный movie_id: " + movieIdStr);
        }

        Movie movie = new Movie(
                movieId,
                rs.getString("title"),
                rs.getString("genres"),
                rs.getDouble("rating"),
                year,
                rs.getString("overview")
        );

        String actorsJson = rs.getString("actors_json");
        if (actorsJson != null && !actorsJson.isEmpty()) {
            movie.setActors(parseActorsFromJson(actorsJson));
        }

        try {
            movie.setRuntime(rs.getInt("runtime"));
            movie.setPopularity(rs.getDouble("popularity"));
        } catch (SQLException e) {
            // Игнорируем, если полей нет
        }

        return movie;
    }

     // Парсинг актеров из JSON

    private String[] parseActorsFromJson(String actorsJson) {
        if (actorsJson == null || actorsJson.isEmpty()) {
            return new String[0];
        }

        List<String> actors = new ArrayList<>();
        Pattern pattern = Pattern.compile("\"name\":\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(actorsJson);

        int count = 0;
        while (matcher.find() && count < 5) {
            actors.add(matcher.group(1));
            count++;
        }

        return actors.toArray(new String[0]);
    }
}