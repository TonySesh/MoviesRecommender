package org.example.models;

import org.apache.mahout.classifier.sgd.CrossFoldLearner;
import org.apache.mahout.classifier.sgd.L2;
import org.apache.mahout.classifier.sgd.ModelSerializer;
import org.apache.mahout.math.Vector;
import org.example.movieSettings.Movie;
import org.example.movieSettings.MovieDAO;
import org.example.userSettings.User;
import org.example.userSettings.UserDAO;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class MahoutIncrementalModel implements Serializable {

    private static final long serialVersionUID = 9L;

    private CrossFoldLearner globalLearner;
    private int numFeatures = 1000; // Должно совпадать с MovieFeatureEncoder.VECTOR_SIZE
    private int numCategories = 2;
    private boolean isInitialized = false;

    private transient MovieDAO movieDAO;
    private transient UserDAO userDAO;

    // Кэш персональных моделей с блокировками
    private transient Map<Integer, PersonalizedModel> personalModelCache;
    private transient Map<Integer, ReentrantReadWriteLock> modelLocks;

    // Счетчики для автоматического переобучения
    private transient int totalActionsSinceLastTrain;
    private transient long lastTrainTime;
    private static final int RETRAIN_THRESHOLD = 50;           // через 50 действий
    private static final long RETRAIN_TIME_THRESHOLD = 24 * 60 * 60 * 1000; // 24 часа

    // Фоновый поток для переобучения
    private transient ScheduledExecutorService retrainScheduler;
    private transient boolean isRetraining = false;

    private static final String MODEL_FILE = "global_model.bin";
    private static final String PERSONAL_DIR = "personal_models/";

    private static final int MIN_LIKES_FOR_RECOMMENDATIONS = 5;

    private static final int LIKE_BOOST = 3;
    private static final int WATCH_BOOST = 2;
    private static final int DISLIKE_BOOST = 4;

    // Веса для финального скоринга
    private static final double MODEL_WEIGHT = 0.6;
    private static final double GENRE_WEIGHT = 0.25;
    private static final double RATING_WEIGHT = 0.1;
    private static final double YEAR_WEIGHT = 0.05;
    private static final double SOCIAL_WEIGHT = 0.15; // Добавлен социальный вес

    // Конструктор
    public MahoutIncrementalModel() {
        initTransient();
        createPersonalDir();
        loadModelFromFile();
        startRetrainScheduler();
    }
    // Инициализация transient полей
    private void initTransient() {
        if (movieDAO == null) movieDAO = new MovieDAO();
        if (userDAO == null) userDAO = new UserDAO();
        if (personalModelCache == null) personalModelCache = new ConcurrentHashMap<>();
        if (modelLocks == null) modelLocks = new ConcurrentHashMap<>();

        totalActionsSinceLastTrain = 0;
        lastTrainTime = System.currentTimeMillis();
    }

    public void shutdown() {
        if (retrainScheduler != null && !retrainScheduler.isShutdown()) {
            retrainScheduler.shutdown();
            try {
                if (!retrainScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    retrainScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                retrainScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    // Создание папки для персональных моделей
    private void createPersonalDir() {
        File dir = new File(PERSONAL_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    // Запуск планировщика для периодической проверки переобучения
    private void startRetrainScheduler() {
        retrainScheduler = Executors.newSingleThreadScheduledExecutor();
        retrainScheduler.scheduleAtFixedRate(() -> {
            checkAndRetrainGlobal();
        }, 1, 1, TimeUnit.HOURS);
    }

    // Загрузить глобальную модель из файла
    private void loadModelFromFile() {
        try {
            if (Files.exists(Paths.get(MODEL_FILE))) {
                globalLearner = ModelSerializer.readBinary(
                        new FileInputStream(MODEL_FILE),
                        CrossFoldLearner.class
                );
                isInitialized = true;
                System.out.println("Загружена сохраненная глобальная модель из файла");
            } else {
                System.out.println("Сохраненная глобальная модель не найдена");
            }
        } catch (IOException e) {
            System.out.println("Не удалось загрузить глобальную модель: " + e.getMessage());
        }
    }

    // Сохранить глобальную модель
    private void saveModelToFile() {
        try {
            ModelSerializer.writeBinary(MODEL_FILE, globalLearner);
            System.out.println("Глобальная модель сохранена в файл");
        } catch (IOException e) {
            System.err.println("Ошибка сохранения глобальной модели: " + e.getMessage());
        }
    }

    // Проверка и переобучение глобальной модели при необходимости
    private synchronized void checkAndRetrainGlobal() {
        if (isRetraining) return;

        boolean shouldRetrain = false;
        String reason = "";

        if (totalActionsSinceLastTrain >= RETRAIN_THRESHOLD) {
            shouldRetrain = true;
            reason = "накоплено " + totalActionsSinceLastTrain + " новых действий";
        }

        long timeSinceLastTrain = System.currentTimeMillis() - lastTrainTime;
        if (!shouldRetrain && timeSinceLastTrain > RETRAIN_TIME_THRESHOLD && totalActionsSinceLastTrain > 0) {
            shouldRetrain = true;
            reason = "прошло " + (timeSinceLastTrain / (60*60*1000)) + " часов";
        }

        if (shouldRetrain && isInitialized) {
            isRetraining = true;
            System.out.println("\nФОНОВОЕ ПЕРЕОБУЧЕНИЕ ГЛОБАЛЬНОЙ МОДЕЛИ (" + reason + ")");

            new Thread(() -> {
                try {
                    trainGlobal();
                    totalActionsSinceLastTrain = 0;
                    lastTrainTime = System.currentTimeMillis();
                    personalModelCache.clear();
                    System.out.println("Кэш персональных моделей очищен");
                } catch (Exception e) {
                    System.err.println("Ошибка фонового переобучения: " + e.getMessage());
                } finally {
                    isRetraining = false;
                }
            }).start();
        }
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public boolean hasEnoughDataForRecommendations() {
        try {
            initTransient();

            List<User> allUsers = userDAO.getAllUsers(0);
            int totalLikes = 0;

            for (User user : allUsers) {
                totalLikes += movieDAO.getLikedMovies(user.getId(), 1, 10000).size();
                if (totalLikes >= MIN_LIKES_FOR_RECOMMENDATIONS) {
                    return true;
                }
            }
            return totalLikes >= MIN_LIKES_FOR_RECOMMENDATIONS;
        } catch (Exception e) {
            System.err.println("Ошибка проверки данных: " + e.getMessage());
            return false;
        }
    }

    public String getCurrentStats() {
        try {
            initTransient();

            List<User> allUsers = userDAO.getAllUsers(0);
            int totalLikes = 0;
            int totalWatches = 0;
            int totalDislikes = 0;
            Set<Integer> activeUsers = new HashSet<>();

            for (User user : allUsers) {
                List<Movie> liked = movieDAO.getLikedMovies(user.getId(), 1, 10000);
                List<Movie> watched = movieDAO.getWatchedMovies(user.getId(), 1, 10000);
                List<Movie> disliked = movieDAO.getDislikedMovies(user.getId(), 1, 10000);

                if (!liked.isEmpty() || !watched.isEmpty() || !disliked.isEmpty()) {
                    activeUsers.add(user.getId());
                }

                totalLikes += liked.size();
                totalWatches += watched.size();
                totalDislikes += disliked.size();
            }

            int totalActions = totalLikes + totalWatches + totalDislikes;
            String modelStatus = isInitialized ? "обучена" : "не обучена";

            return String.format(
                    "СТАТИСТИКА ИЗ БД:\n" +
                            "Всего действий: %d\n" +
                            "Пользователей с активностью: %d\n" +
                            "Лайков: %d\n" +
                            "Просмотров: %d\n" +
                            "Дизлайков: %d\n" +
                            "Глобальная модель: %s\n" +
                            "Новых действий после обучения: %d\n" +
                            "Порог для рекомендаций: %d лайков",
                    totalActions, activeUsers.size(), totalLikes, totalWatches, totalDislikes,
                    modelStatus,
                    totalActionsSinceLastTrain,
                    MIN_LIKES_FOR_RECOMMENDATIONS
            );
        } catch (Exception e) {
            return "Ошибка получения статистики: " + e.getMessage();
        }
    }
    public void trainGlobal() throws Exception {
        initTransient();

        System.out.println("ОБУЧЕНИЕ ГЛОБАЛЬНОЙ МОДЕЛИ НА ВСЕХ ДАННЫХ ИЗ БД");

        long startTime = System.currentTimeMillis();
        System.out.println(getCurrentStats());

        globalLearner = new CrossFoldLearner(5, numCategories, numFeatures, new L2())
                .stepOffset(1000)
                .decayExponent(0.5)
                .learningRate(0.5);

        List<User> allUsers = userDAO.getAllUsers(0);
        int exampleId = 0;
        int totalLikes = 0;
        int totalWatches = 0;
        int totalDislikes = 0;

        System.out.println("\n Загрузка пользователей: " + allUsers.size());

        for (User user : allUsers) {
            List<Movie> liked = movieDAO.getLikedMovies(user.getId(), 1, 10000);
            List<Movie> watched = movieDAO.getWatchedMovies(user.getId(), 1, 10000);
            List<Movie> disliked = movieDAO.getDislikedMovies(user.getId(), 1, 10000);

            totalLikes += liked.size();
            totalWatches += watched.size();
            totalDislikes += disliked.size();

            System.out.printf("%-10s | лайков: %3d | просмотров: %3d | дизлайков: %3d%n",
                    user.getUsername(), liked.size(), watched.size(), disliked.size());

            for (Movie movie : liked) {
                Vector features = MovieFeatureEncoder.encodeMovie(movie);
                globalLearner.train(exampleId++, 1, features);
            }

            for (Movie movie : watched) {
                boolean isLiked = liked.stream().anyMatch(m -> m.getId() == movie.getId());
                if (!isLiked) {
                    Vector features = MovieFeatureEncoder.encodeMovie(movie);
                    globalLearner.train(exampleId++, 1, features);
                }
            }

            for (Movie movie : disliked) {
                Vector features = MovieFeatureEncoder.encodeMovie(movie);
                globalLearner.train(exampleId++, 0, features);
            }
        }

        isInitialized = true;
        saveModelToFile();

        long time = System.currentTimeMillis() - startTime;

        System.out.println("ГЛОБАЛЬНОЕ ОБУЧЕНИЕ ЗАВЕРШЕНО");
        System.out.println("Статистика обучения:");
        System.out.println("Всего примеров: " + exampleId);
        System.out.println("Время обучения: " + time + " мс");
        if (globalLearner != null) {
            System.out.println("Финальная ошибка: " + String.format("%.4f", globalLearner.logLikelihood()));
        }
    }
    // Расчет корреляции Пирсона между двумя списками чисел
    private double calculatePearsonCorrelation(List<Double> x, List<Double> y) {
        if (x.size() != y.size() || x.size() < 2) return 0;

        int n = x.size();
        double sumX = 0, sumY = 0;
        for (int i = 0; i < n; i++) {
            sumX += x.get(i);
            sumY += y.get(i);
        }
        double meanX = sumX / n;
        double meanY = sumY / n;

        double cov = 0, stdX = 0, stdY = 0;
        for (int i = 0; i < n; i++) {
            double diffX = x.get(i) - meanX;
            double diffY = y.get(i) - meanY;
            cov += diffX * diffY;
            stdX += diffX * diffX;
            stdY += diffY * diffY;
        }

        if (stdX == 0 || stdY == 0) return 0;
        return cov / Math.sqrt(stdX * stdY);
    }

    // НОВЫЙ МЕТОД: Расчет социального бонуса для фильма
    private double calculateSocialBonus(int movieId, UserProfile profile) {
        if (profile.friendIds.isEmpty() || profile.friendLikes.isEmpty()) {
            return 0.0;
        }

        // Сколько друзей лайкнули этот фильм
        int likedByFriends = profile.friendLikes.getOrDefault(movieId, 0);
        if (likedByFriends == 0) {
            return 0.0;
        }

        // Нормализуем: максимум 0.5 (если фильм лайкнули все друзья)
        double maxPossible = profile.friendIds.size() * LIKE_BOOST; // максимум очков от друзей
        return Math.min(0.5, likedByFriends * LIKE_BOOST / maxPossible);
    }
    // Анализ профиля пользователя
    private UserProfile analyzeUserProfile(int userId) throws Exception {
        List<User> friends = userDAO.getFriends(userId);
        List<Movie> liked = movieDAO.getLikedMovies(userId, 1, 100);
        List<Movie> watched = movieDAO.getWatchedMovies(userId, 1, 100);
        List<Movie> disliked = movieDAO.getDislikedMovies(userId, 1, 100);

        List<Integer> friendIds = new ArrayList<>();
        Map<Integer, Integer> friendLikesMap = new HashMap<>();

        for (User friend : friends) {
            friendIds.add(friend.getId());
            List<Movie> friendLikes = movieDAO.getLikedMovies(friend.getId(), 1, 100);
            for (Movie m : friendLikes) {
                friendLikesMap.put(m.getId(), friendLikesMap.getOrDefault(m.getId(), 0) + 1);
            }
        }

        return new UserProfile(
                userId,
                liked.size(),
                watched.size(),
                disliked.size(),
                friends.size(),
                friendIds,
                friendLikesMap,
                liked,          // Добавим сами списки
                disliked,
                movieDAO
        );
    }

    // Анализ корреляции с друзьями
    private void analyzeFriendCorrelation(int userId, UserProfile profile) throws Exception {
        if (profile.friendIds.isEmpty()) {
            System.out.println("\n У пользователя нет друзей");
            return;
        }

        System.out.println("\n" + "┌" + "─".repeat(118) + "┐");
        System.out.println("│ СОЦИАЛЬНАЯ КОРРЕЛЯЦИЯ С ДРУЗЬЯМИ " + " ".repeat(80) + "│");
        System.out.println("├" + "─".repeat(118) + "┤");
        System.out.printf("│ %-12s │ %-22s │ %-18s │ %-12s │ %-12s │ %-12s │ %-10s │%n",
                "Друг", "Любимые жанры", "Общие фильмы", "Жаккар", "Пирсон", "Комбинир.", "Влияние");
        System.out.println("├" + "─".repeat(12) + "┼" + "─".repeat(24) + "┼" + "─".repeat(20) + "┼" +
                "─".repeat(14) + "┼" + "─".repeat(14) + "┼" + "─".repeat(14) + "┼" + "─".repeat(12) + "┤");

        List<Movie> userLikes = profile.userLikes;
        Map<Integer, Double> userRatings = new HashMap<>();
        Set<Integer> userLikedIds = new HashSet<>();

        for (Movie m : userLikes) {
            userLikedIds.add(m.getId());
            userRatings.put(m.getId(), m.getRating());
        }

        double userAvgRating = userLikes.stream()
                .mapToDouble(Movie::getRating)
                .average().orElse(0);

        for (int friendId : profile.friendIds) {
            User friend = userDAO.getUserById(friendId);
            List<Movie> friendLikes = movieDAO.getLikedMovies(friendId, 1, 100);

            Set<Integer> friendLikedIds = friendLikes.stream()
                    .map(Movie::getId)
                    .collect(Collectors.toSet());

            Set<Integer> union = new HashSet<>(userLikedIds);
            union.addAll(friendLikedIds);

            Set<Integer> intersection = new HashSet<>(userLikedIds);
            intersection.retainAll(friendLikedIds);

            double jaccard = union.isEmpty() ? 0 : (double) intersection.size() / union.size();

            double pearson = 0;
            String pearsonStr = "—";
            String combinedStr = "—";

            if (intersection.size() >= 3) {
                Map<Integer, Double> friendRatings = friendLikes.stream()
                        .collect(Collectors.toMap(Movie::getId, Movie::getRating));

                List<Double> userRates = new ArrayList<>();
                List<Double> friendRates = new ArrayList<>();

                for (Integer movieId : intersection) {
                    userRates.add(userRatings.get(movieId));
                    friendRates.add(friendRatings.get(movieId));
                }

                pearson = calculatePearsonCorrelation(userRates, friendRates);
                double combined = 0.4 * jaccard + 0.6 * (pearson + 1) / 2;

                String influence;
                if (combined >= 0.6) influence = "ВЫСОКОЕ";
                else if (combined >= 0.3) influence = "СРЕДНЕЕ";
                else if (combined >= 0.1) influence = "НИЗКОЕ";
                else influence = "МИНИМАЛЬНОЕ";

                pearsonStr = String.format("%+.3f", pearson);
                combinedStr = String.format("%.1f%%", combined * 100);

                Map<String, Integer> friendGenres = new HashMap<>();
                for (Movie m : friendLikes) {
                    List<String> genres = MovieFeatureEncoder.parseGenresList(m.getGenres());
                    for (String g : genres) {
                        friendGenres.put(g, friendGenres.getOrDefault(g, 0) + 1);
                    }
                }

                String topGenres = friendGenres.entrySet().stream()
                        .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                        .limit(3)
                        .map(e -> e.getKey() + " (" + e.getValue() + ")")
                        .collect(Collectors.joining(", "));
                if (topGenres.isEmpty()) topGenres = "—";

                String commonExamples = "";
                if (!intersection.isEmpty()) {
                    List<String> examples = new ArrayList<>();
                    for (Integer id : intersection.stream().limit(2).collect(Collectors.toList())) {
                        Movie m = movieDAO.getMovieById(id);
                        if (m != null) examples.add(m.getTitle());
                    }
                    commonExamples = String.join(", ", examples);
                    if (intersection.size() > 2) {
                        commonExamples += " и еще " + (intersection.size() - 2);
                    }
                } else {
                    commonExamples = "—";
                }

                System.out.printf("│ %-12s │ %-22s │ %-18s │ %5.1f%%     │ %-11s │ %-11s │ %-12s │%n",
                        friend.getUsername(),
                        topGenres.length() > 22 ? topGenres.substring(0, 19) + "..." : topGenres,
                        commonExamples.length() > 18 ? commonExamples.substring(0, 15) + "..." : commonExamples,
                        jaccard * 100,
                        pearsonStr,
                        combinedStr,
                        influence);
            } else {
                String influence;
                if (jaccard >= 0.3) influence = "СРЕДНЕЕ";
                else if (jaccard >= 0.1) influence = "НИЗКОЕ";
                else influence = "МИНИМАЛЬНОЕ";

                Map<String, Integer> friendGenres = new HashMap<>();
                for (Movie m : friendLikes) {
                    List<String> genres = MovieFeatureEncoder.parseGenresList(m.getGenres());
                    for (String g : genres) {
                        friendGenres.put(g, friendGenres.getOrDefault(g, 0) + 1);
                    }
                }

                String topGenres = friendGenres.entrySet().stream()
                        .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                        .limit(3)
                        .map(e -> e.getKey() + " (" + e.getValue() + ")")
                        .collect(Collectors.joining(", "));
                if (topGenres.isEmpty()) topGenres = "—";

                String commonExamples = intersection.isEmpty() ? "—" :
                        String.valueOf(intersection.size()) + " фильмов";

                System.out.printf("│ %-12s │ %-22s │ %-18s │ %5.1f%%     │ %-11s │ %-11s │ %-12s │%n",
                        friend.getUsername(),
                        topGenres.length() > 22 ? topGenres.substring(0, 19) + "..." : topGenres,
                        commonExamples.length() > 18 ? commonExamples.substring(0, 15) + "..." : commonExamples,
                        jaccard * 100,
                        "—",
                        String.format("%.1f%%", jaccard * 100),
                        influence);
            }
        }
        System.out.println("└" + "─".repeat(12) + "┴" + "─".repeat(24) + "┴" + "─".repeat(20) + "┴" +
                "─".repeat(14) + "┴" + "─".repeat(14) + "┴" + "─".repeat(14) + "┴" + "─".repeat(12) + "┘");
    }
    // Получить персональную модель (с кэшированием)
    public PersonalizedModel getPersonalizedModel(int userId) throws Exception {
        initTransient();

        if (!isInitialized) {
            loadModelFromFile();
            if (!isInitialized) {
                throw new RuntimeException("Глобальная модель не обучена");
            }
        }

        ReentrantReadWriteLock lock = getLockForUser(userId);
        lock.readLock().lock();
        try {
            if (personalModelCache.containsKey(userId)) {
                System.out.println(" Использована кэшированная модель для пользователя " + userId);
                return personalModelCache.get(userId);
            }
        } finally {
            lock.readLock().unlock();
        }

        lock.writeLock().lock();
        try {
            if (personalModelCache.containsKey(userId)) {
                return personalModelCache.get(userId);
            }

            PersonalizedModel loaded = loadPersonalizedModel(userId);
            if (loaded != null) {
                System.out.println("Загружена сохраненная модель для пользователя " + userId);
                personalModelCache.put(userId, loaded);
                return loaded;
            }

            System.out.println("\n СОЗДАНИЕ НОВОЙ ПЕРСОНАЛЬНОЙ МОДЕЛИ ДЛЯ ПОЛЬЗОВАТЕЛЯ " + userId);
            PersonalizedModel newModel = createPersonalizedModel(userId);
            personalModelCache.put(userId, newModel);
            savePersonalizedModel(userId, newModel);
            return newModel;

        } finally {
            lock.writeLock().unlock();
        }
    }

    // Загрузить персональную модель из файла
    private PersonalizedModel loadPersonalizedModel(int userId) {
        String filename = PERSONAL_DIR + "personal_" + userId + ".bin";
        try {
            if (Files.exists(Paths.get(filename))) {
                CrossFoldLearner learner = ModelSerializer.readBinary(
                        new FileInputStream(filename),
                        CrossFoldLearner.class
                );
                return new PersonalizedModel(userId, learner, this);
            }
        } catch (IOException e) {
            System.out.println("Не удалось загрузить персональную модель: " + e.getMessage());
        }
        return null;
    }

    // Сохранить персональную модель
    private void savePersonalizedModel(int userId, PersonalizedModel model) {
        String filename = PERSONAL_DIR + "personal_" + userId + ".bin";
        try {
            ModelSerializer.writeBinary(filename, model.getLearner());
        } catch (IOException e) {
            System.err.println("Ошибка сохранения персональной модели: " + e.getMessage());
        }
    }

    // СОЗДАТЬ ПЕРСОНАЛЬНУЮ МОДЕЛЬ с нуля
    private PersonalizedModel createPersonalizedModel(int userId) throws Exception {
        System.out.println("ПЕРСОНАЛИЗАЦИЯ ДЛЯ ПОЛЬЗОВАТЕЛЯ " + userId);

        UserProfile profile = analyzeUserProfile(userId);
        System.out.println(profile);

        CrossFoldLearner personalized = globalLearner.copy();
        int personalExamples = 0;

        List<Movie> likes = movieDAO.getLikedMovies(userId, 1, 10000);
        List<Movie> watched = movieDAO.getWatchedMovies(userId, 1, 10000);
        List<Movie> dislikes = movieDAO.getDislikedMovies(userId, 1, 10000);

        System.out.println("\n Статистика пользователя:");
        System.out.println("Лайков: " + likes.size() + " (x" + LIKE_BOOST + ")");
        System.out.println("Просмотров: " + watched.size() + " (x" + WATCH_BOOST + ")");
        System.out.println("Дизлайков: " + dislikes.size() + " (x" + DISLIKE_BOOST + ")");

        for (Movie movie : likes) {
            Vector features = MovieFeatureEncoder.encodeMovie(movie);
            for (int i = 0; i < LIKE_BOOST; i++) {
                personalized.train(personalExamples++, 1, features);
            }
        }

        for (Movie movie : watched) {
            boolean isLiked = likes.stream().anyMatch(l -> l.getId() == movie.getId());
            if (!isLiked) {
                Vector features = MovieFeatureEncoder.encodeMovie(movie);
                for (int i = 0; i < WATCH_BOOST; i++) {
                    personalized.train(personalExamples++, 1, features);
                }
            }
        }

        for (Movie movie : dislikes) {
            Vector features = MovieFeatureEncoder.encodeMovie(movie);
            for (int i = 0; i < DISLIKE_BOOST; i++) {
                personalized.train(personalExamples++, 0, features);
            }
        }

        System.out.println("\n Персональная модель создана, добавлено " + personalExamples + " примеров");

        analyzeFriendCorrelation(userId, profile);
        analyzeGenrePreferences(userId, likes, watched, dislikes);

        return new PersonalizedModel(userId, personalized, this);
    }

    private ReentrantReadWriteLock getLockForUser(int userId) {
        return modelLocks.computeIfAbsent(userId, k -> new ReentrantReadWriteLock());
    }

    public void handleNewLike(int userId, int movieId) {
        System.out.println("\n Новый лайк: пользователь " + userId + " → фильм " + movieId);
        handleUserAction(userId, movieId, true, false, false);
    }

    public void handleNewWatch(int userId, int movieId) {
        System.out.println("\n Новый просмотр: пользователь " + userId + " → фильм " + movieId);
        handleUserAction(userId, movieId, false, true, false);
    }

    public void handleNewDislike(int userId, int movieId) {
        System.out.println("\n Новый дизлайк: пользователь " + userId + " → фильм " + movieId);
        handleUserAction(userId, movieId, false, false, true);
    }

    private void handleUserAction(int userId, int movieId, boolean isLike, boolean isWatch, boolean isDislike) {
        try {
            initTransient();

            ReentrantReadWriteLock lock = getLockForUser(userId);
            lock.writeLock().lock();

            try {
                PersonalizedModel personalModel = getPersonalizedModel(userId);
                Movie movie = movieDAO.getMovieById(movieId);
                if (movie == null) {
                    System.err.println("Фильм не найден: " + movieId);
                    return;
                }

                Vector features = MovieFeatureEncoder.encodeMovie(movie);
                int boost = 0;

                if (isLike) {
                    boost = LIKE_BOOST;
                    for (int i = 0; i < boost; i++) {
                        personalModel.getLearner().train(0, 1, features);
                    }
                } else if (isDislike) {
                    boost = DISLIKE_BOOST;
                    for (int i = 0; i < boost; i++) {
                        personalModel.getLearner().train(0, 0, features);
                    }
                } else if (isWatch) {
                    List<Movie> likes = movieDAO.getLikedMovies(userId, 1, 10000);
                    boolean alreadyLiked = likes.stream().anyMatch(m -> m.getId() == movieId);
                    if (!alreadyLiked) {
                        boost = WATCH_BOOST;
                        for (int i = 0; i < boost; i++) {
                            personalModel.getLearner().train(0, 1, features);
                        }
                    } else {
                        System.out.println("Фильм уже лайкнут, просмотр не добавлен");
                        return;  // без unlock, он сработает в finally
                    }
                }

                savePersonalizedModel(userId, personalModel);
                System.out.println("Персональная модель дообучена (x" + boost + ")");

            } finally {
                lock.writeLock().unlock();
            }

            totalActionsSinceLastTrain++;
            checkAndRetrainGlobal();

        } catch (Exception e) {
            System.err.println(" Ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
    // ВНУТРЕННИЙ КЛАСС: Фильм с оценкой
    private static class ScoredMovie implements Serializable {
        private static final long serialVersionUID = 3L;

        int movieId;
        double finalScore;
        String title;
        double modelScore;
        double genreBonus;
        double ratingBonus;
        double yearBonus;
        double socialBonus;

        ScoredMovie(int movieId, double finalScore, String title,
                    double modelScore, double genreBonus,
                    double ratingBonus, double yearBonus,
                    double socialBonus) {
            this.movieId = movieId;
            this.finalScore = finalScore;
            this.title = title;
            this.modelScore = modelScore;
            this.genreBonus = genreBonus;
            this.ratingBonus = ratingBonus;
            this.yearBonus = yearBonus;
            this.socialBonus = socialBonus;
        }
    }

    // Внутренний класс для профиля
    private static class UserProfile implements Serializable {
        private static final long serialVersionUID = 2L;

        int userId;
        int likesCount;
        int watchesCount;
        int dislikesCount;
        int friendsCount;
        List<Integer> friendIds;
        Map<Integer, Integer> friendLikes;
        List<Movie> userLikes;
        List<Movie> userDislikes;
        transient MovieDAO movieDAO;

        UserProfile(int userId, int likesCount, int watchesCount,
                    int dislikesCount, int friendsCount,
                    List<Integer> friendIds,
                    Map<Integer, Integer> friendLikes,
                    List<Movie> userLikes,
                    List<Movie> userDislikes,
                    MovieDAO movieDAO) {
            this.userId = userId;
            this.likesCount = likesCount;
            this.watchesCount = watchesCount;
            this.dislikesCount = dislikesCount;
            this.friendsCount = friendsCount;
            this.friendIds = friendIds;
            this.friendLikes = friendLikes;
            this.userLikes = userLikes;
            this.userDislikes = userDislikes;
            this.movieDAO = movieDAO;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("\n ПРОФИЛЬ ПОЛЬЗОВАТЕЛЯ ").append(userId).append(":\n");
            sb.append("   • Личных лайков: ").append(likesCount).append("\n");
            sb.append("   • Личных просмотров: ").append(watchesCount).append("\n");
            sb.append("   • Личных дизлайков: ").append(dislikesCount).append("\n");
            sb.append("   • Друзей: ").append(friendsCount).append("\n");

            if (!friendLikes.isEmpty()) {
                sb.append("   • Топ фильмов у друзей:\n");
                friendLikes.entrySet().stream()
                        .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                        .limit(5)
                        .forEach(e -> {
                            try {
                                Movie m = movieDAO.getMovieById(e.getKey());
                                String title = (m != null) ? m.getTitle() : "ID " + e.getKey();
                                sb.append("      - ").append(title)
                                        .append(": ").append(e.getValue()).append(" друзей\n");
                            } catch (Exception ex) {
                                sb.append("      - Фильм ID ").append(e.getKey())
                                        .append(": ").append(e.getValue()).append(" друзей\n");
                            }
                        });
            }
            return sb.toString();
        }
    }

    // ВНУТРЕННИЙ КЛАСС: Персонализированная модель
    public class PersonalizedModel implements Serializable {
        private static final long serialVersionUID = 1L;

        private int userId;
        private transient CrossFoldLearner learner;
        private MahoutIncrementalModel parent;

        public PersonalizedModel(int userId, CrossFoldLearner learner, MahoutIncrementalModel parent) {
            this.userId = userId;
            this.learner = learner;
            this.parent = parent;
        }

        public CrossFoldLearner getLearner() {
            return learner;
        }

        // ПОЛУЧИТЬ РЕКОМЕНДАЦИИ для пользователя
        public List<Integer> recommend(int limit) throws Exception {
            parent.initTransient();

            if (learner == null) {
                return new ArrayList<>();
            }

            System.out.println("ГЕНЕРАЦИЯ РЕКОМЕНДАЦИЙ ДЛЯ ПОЛЬЗОВАТЕЛЯ " + userId);

            UserProfile profile = parent.analyzeUserProfile(userId);
            System.out.println(profile);

            if (profile.friendIds.isEmpty()) {
                System.out.println("\n У пользователя нет друзей");
            } else {
                parent.analyzeFriendCorrelation(userId, profile);
            }

            List<Movie> likedMovies = parent.movieDAO.getLikedMovies(userId, 1, 100);
            List<Movie> dislikedMovies = parent.movieDAO.getDislikedMovies(userId, 1, 100);
            List<Movie> watchedMovies = parent.movieDAO.getWatchedMovies(userId, 1, 100);

            parent.analyzeGenrePreferences(userId, likedMovies, watchedMovies, dislikedMovies);

            Map<String, Double> genrePreferences = new HashMap<>();

            double avgLikedRating = likedMovies.stream()
                    .mapToDouble(Movie::getRating)
                    .average().orElse(7.0);

            double avgLikedYear = likedMovies.stream()
                    .filter(m -> m.getYear() > 0)
                    .mapToInt(Movie::getYear)
                    .average().orElse(2010);

            for (Movie m : likedMovies) {
                List<String> genres = MovieFeatureEncoder.parseGenresList(m.getGenres());
                for (String g : genres) {
                    genrePreferences.put(g, genrePreferences.getOrDefault(g, 0.0) + 3.0);
                }
            }

            for (Movie m : watchedMovies) {
                boolean isLiked = likedMovies.stream().anyMatch(l -> l.getId() == m.getId());
                if (!isLiked) {
                    List<String> genres = MovieFeatureEncoder.parseGenresList(m.getGenres());
                    for (String g : genres) {
                        genrePreferences.put(g, genrePreferences.getOrDefault(g, 0.0) + 1.0);
                    }
                }
            }

            for (Movie m : dislikedMovies) {
                List<String> genres = MovieFeatureEncoder.parseGenresList(m.getGenres());
                for (String g : genres) {
                    genrePreferences.put(g, genrePreferences.getOrDefault(g, 0.0) - 4.0);
                }
            }

            for (Map.Entry<String, Double> entry : genrePreferences.entrySet()) {
                genrePreferences.put(entry.getKey(), entry.getValue() / 10.0);
            }

            List<Movie> allMovies = parent.movieDAO.getAllMoviesPage(1, 5000, userId);
            System.out.println("\n Всего фильмов для оценки: " + allMovies.size());

            List<ScoredMovie> scored = new ArrayList<>();
            int skipped = 0;

            for (Movie movie : allMovies) {
                if (movie.isLiked() || movie.isDisliked()) {
                    skipped++;
                    continue;
                }

                Vector features = MovieFeatureEncoder.encodeMovie(movie);
                double modelScore = learner.classifyFull(features).get(1);

                double genreBonus = 0;
                int genreMatchCount = 0;
                List<String> movieGenres = MovieFeatureEncoder.parseGenresList(movie.getGenres());

                for (String genre : movieGenres) {
                    Double pref = genrePreferences.get(genre);
                    if (pref != null) {
                        genreBonus += pref;
                        genreMatchCount++;
                    }
                }

                if (genreMatchCount > 0) {
                    genreBonus = genreBonus / genreMatchCount;
                }

                double ratingBonus = (movie.getRating() - avgLikedRating) / 5.0;

                double yearBonus = 0;
                if (movie.getYear() > 0) {
                    double yearDiff = Math.abs(movie.getYear() - avgLikedYear);
                    yearBonus = Math.max(0, 1.0 - yearDiff / 50.0);
                }

                double popularityBonus = Math.min(movie.getPopularity(), 100) / 100.0;

                // НОВОЕ: социальный бонус
                double socialBonus = parent.calculateSocialBonus(movie.getId(), profile);

                double finalScore =
                        MODEL_WEIGHT * modelScore +
                                GENRE_WEIGHT * (0.5 + genreBonus) +
                                RATING_WEIGHT * (0.5 + ratingBonus) +
                                YEAR_WEIGHT * yearBonus +
                                0.01 * popularityBonus +
                                SOCIAL_WEIGHT * socialBonus;  // социальный вес добавлен

                scored.add(new ScoredMovie(
                        movie.getId(), finalScore, movie.getTitle(),
                        modelScore, genreBonus, ratingBonus, yearBonus, socialBonus
                ));
            }

            System.out.println("Пропущено оцененных фильмов: " + skipped);
            System.out.println("Оценено фильмов: " + scored.size());

            scored.sort((a, b) -> Double.compare(b.finalScore, a.finalScore));

            analyzeRecommendationFactors(scored, genrePreferences, profile);

            List<Integer> recommendations = new ArrayList<>();
            for (int i = 0; i < Math.min(limit, scored.size()); i++) {
                recommendations.add(scored.get(i).movieId);
            }

            System.out.println("\n Получено рекомендаций: " + recommendations.size());
            if (!recommendations.isEmpty() && scored.size() >= 3) {
                System.out.println("\n ТОП-5 РЕКОМЕНДАЦИИ:");
                for (int i = 0; i < 5; i++) {
                    ScoredMovie sm = scored.get(i);
                    System.out.printf("   %d. %s (%.1f%%)\n", i+1, sm.title, sm.finalScore * 100);
                }
            }
            return recommendations;
        }
    }
    // Анализ жанровых предпочтений
    private void analyzeGenrePreferences(int userId, List<Movie> liked, List<Movie> watched, List<Movie> disliked) throws Exception {
        Map<String, GenreStats> genreStats = new HashMap<>();

        for (Movie m : liked) {
            List<String> genres = MovieFeatureEncoder.parseGenresList(m.getGenres());
            for (String g : genres) {
                GenreStats stats = genreStats.computeIfAbsent(g, k -> new GenreStats());
                stats.likes++;
                stats.score += 3;
                stats.avgRating += m.getRating();
                stats.movies.add(m.getTitle());
            }
        }

        for (Movie m : watched) {
            List<String> genres = MovieFeatureEncoder.parseGenresList(m.getGenres());
            for (String g : genres) {
                GenreStats stats = genreStats.computeIfAbsent(g, k -> new GenreStats());
                stats.watches++;
                if (!stats.movies.contains(m.getTitle())) {
                    stats.score += 2;
                    stats.avgRating += m.getRating();
                    stats.movies.add(m.getTitle());
                }
            }
        }

        for (Movie m : disliked) {
            List<String> genres = MovieFeatureEncoder.parseGenresList(m.getGenres());
            for (String g : genres) {
                GenreStats stats = genreStats.computeIfAbsent(g, k -> new GenreStats());
                stats.dislikes++;
                stats.score -= 4;
                stats.movies.add(m.getTitle());
            }
        }

        for (GenreStats stats : genreStats.values()) {
            int totalMovies = stats.likes + stats.watches;
            if (totalMovies > 0) {
                stats.avgRating /= totalMovies;
            }
        }

        System.out.println("\n ДЕТАЛЬНЫЙ АНАЛИЗ ЖАНРОВ:");
        System.out.println("┌─────────────┬──────────┬──────────┬──────────┬──────────┬──────────┬────────────────────┐");
        System.out.println("│ Жанр        │  Лайки   │ Просмотры│ Дизлайки │ Ср.рейтинг│ Общий    │ Примеры фильмов   │");
        System.out.println("│             │          │          │          │          │ балл     │                    │");
        System.out.println("├─────────────┼──────────┼──────────┼──────────┼──────────┼──────────┼────────────────────┤");

        genreStats.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().score, a.getValue().score))
                .limit(10)
                .forEach(e -> {
                    GenreStats stats = e.getValue();
                    String examples = stats.movies.stream()
                            .limit(2)
                            .collect(Collectors.joining(", "));
                    if (stats.movies.size() > 2) {
                        examples += " и еще " + (stats.movies.size() - 2);
                    }

                    System.out.printf("│ %-11s │ %8d │ %8d │ %8d │    %5.2f   │ %8d │ %-18s │%n",
                            e.getKey(),
                            stats.likes,
                            stats.watches,
                            stats.dislikes,
                            stats.avgRating,
                            stats.score,
                            examples.length() > 18 ? examples.substring(0, 15) + "..." : examples);
                });
        System.out.println("└─────────────┴──────────┴──────────┴──────────┴──────────┴──────────┴────────────────────┘");
    }

    // Внутренний класс для статистики по жанрам
    private static class GenreStats {
        int likes = 0;
        int watches = 0;
        int dislikes = 0;
        double avgRating = 0;
        int score = 0;
        Set<String> movies = new HashSet<>();
    }

    // Анализ факторов для топ-рекомендаций
    private void analyzeRecommendationFactors(List<ScoredMovie> scored,
                                              Map<String, Double> genrePreferences,
                                              UserProfile profile) throws Exception {
        System.out.println("\n АНАЛИЗ ФАКТОРОВ ДЛЯ ТОП-10 РЕКОМЕНДАЦИЙ:");
        System.out.println("┌─────┬──────────────────────────────┬─────────────────────────────────────────────┐");
        System.out.println("│ #   │ Название                     │ Факторы влияния                             │");
        System.out.println("├─────┼──────────────────────────────┼─────────────────────────────────────────────┤");

        for (int i = 0; i < Math.min(10, scored.size()); i++) {
            ScoredMovie sm = scored.get(i);
            Movie m = movieDAO.getMovieById(sm.movieId);
            if (m == null) continue;

            List<String> movieGenres = MovieFeatureEncoder.parseGenresList(m.getGenres());
            List<String> factors = new ArrayList<>();

            for (String g : movieGenres) {
                Double pref = genrePreferences.get(g);
                if (pref != null && pref > 0) {
                    factors.add(String.format("%s (%.1f)", g, pref));
                }
            }

            if (m.getRating() > 8.0) {
                factors.add(String.format("рейтинг %.1f", m.getRating()));
            }

            if (m.getYear() >= 2020) {
                factors.add("новинка " + m.getYear());
            }

            if (m.getPopularity() > 50) {
                factors.add("популярный");
            }

            // НОВОЕ: отображаем социальный фактор
            if (sm.socialBonus > 0.1) {
                factors.add(String.format("совет друзей (%.0f%%)", sm.socialBonus * 100));
            }

            if (m.getRuntime() > 0) {
                if (m.getRuntime() < 90) {
                    factors.add("короткий");
                } else if (m.getRuntime() > 150) {
                    factors.add("длинный");
                }
            }

            String factorsStr = factors.isEmpty() ? "нет явных факторов" :
                    String.join(", ", factors);

            System.out.printf("│ %-3d │ %-28s │ %-43s │%n",
                    i+1,
                    m.getTitle().length() > 25 ? m.getTitle().substring(0, 22) + "..." : m.getTitle(),
                    factorsStr.length() > 43 ? factorsStr.substring(0, 40) + "..." : factorsStr);
        }
        System.out.println("└─────┴──────────────────────────────┴─────────────────────────────────────────────┘");
    }
}