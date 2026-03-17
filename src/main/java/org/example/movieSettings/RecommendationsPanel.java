package org.example.movieSettings;

import org.example.models.MahoutIncrementalModel;
import org.example.models.ModelInitializer;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RecommendationsPanel extends JPanel {
    private JPanel moviesContainer;
    private JScrollPane scrollPane;
    private MovieDAO movieDAO;
    private int userId;
    private String username;

    // Пагинация
    private int currentPage = 1;
    private int totalPages = 1;
    private int pageSize = 20;
    private JLabel pageLabel;
    private JButton prevButton;
    private JButton nextButton;
    private JPanel bottomPanel;
    private JTextField pageInput;

    // Фильтры
    private JTextField searchField;
    private JComboBox<String> searchTypeCombo;
    private JTextField genreField;
    private JTextField ratingField;

    // Для поиска и фильтрации
    private List<Movie> currentSearchResults;
    private boolean isSearchMode = false;

    // Модель
    private MahoutIncrementalModel model;
    private List<Integer> recommendedIds;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    // Цветовая схема
    private final Color darkBg = new Color(25, 15, 30);
    private final Color panelBg = new Color(35, 25, 45);
    private final Color cardBg = new Color(45, 30, 55);
    private final Color accentColor = new Color(110, 70, 140);
    private final Color dislikeColor = new Color(140, 70, 70);
    private final Color watchedColor = new Color(60, 100, 80);
    private final Color textColor = Color.WHITE;
    private final Color secondaryText = new Color(180, 150, 200);

    public RecommendationsPanel(int userId, String username) {
        this.userId = userId;
        this.username = username;
        this.movieDAO = new MovieDAO();

        // Получаем модель из инициализатора
        Object obj = ModelInitializer.getModel();
        if (obj instanceof MahoutIncrementalModel) {
            this.model = (MahoutIncrementalModel) obj;
        } else {
            throw new RuntimeException("Ошибка: ModelInitializer вернул неправильный тип");
        }

        setLayout(new BorderLayout());
        setBackground(darkBg);

        // Контейнер для фильмов
        moviesContainer = new JPanel();
        moviesContainer.setLayout(new BoxLayout(moviesContainer, BoxLayout.Y_AXIS));
        moviesContainer.setBackground(darkBg);

        scrollPane = new JScrollPane(moviesContainer);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(darkBg);

        // Улучшаем скролл
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // Верхняя панель с фильтрами
        JPanel topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);

        // Центральная панель с фильмами
        add(scrollPane, BorderLayout.CENTER);

        // Нижняя панель с пагинацией
        bottomPanel = createBottomPanel();
        add(bottomPanel, BorderLayout.SOUTH);

        // Загружаем рекомендации
        loadRecommendations();
    }

    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBackground(panelBg);
        topPanel.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 25));

        Font labelFont = new Font("Arial", Font.BOLD, 14);
        Font fieldFont = new Font("Arial", Font.PLAIN, 14);

        // ПАНЕЛЬ ПОИСКА
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        searchPanel.setBackground(panelBg);
        searchPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        searchField = new JTextField(25);
        searchField.setFont(fieldFont);
        searchField.setBackground(new Color(60, 45, 75));
        searchField.setForeground(textColor);
        searchField.setCaretColor(textColor);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accentColor),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        searchField.addActionListener(e -> performSearch());
        searchPanel.add(searchField);

        String[] searchTypes = {"Название", "Актер"};
        searchTypeCombo = new JComboBox<>(searchTypes);
        searchTypeCombo.setFont(fieldFont);
        searchTypeCombo.setBackground(new Color(60, 45, 75));
        searchTypeCombo.setForeground(textColor);
        searchTypeCombo.setBorder(BorderFactory.createLineBorder(accentColor));
        searchPanel.add(searchTypeCombo);

        JButton searchBtn = createStyledButton("Найти", accentColor);
        searchBtn.addActionListener(e -> performSearch());
        searchPanel.add(searchBtn);

        JButton resetBtn = createStyledButton("Сброс", new Color(80, 60, 90));
        resetBtn.addActionListener(e -> resetSearch());
        searchPanel.add(resetBtn);

        // ПАНЕЛЬ ФИЛЬТРОВ
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        filterPanel.setBackground(panelBg);
        filterPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JLabel genreLabelUI = new JLabel("Жанр:");
        genreLabelUI.setFont(labelFont);
        genreLabelUI.setForeground(textColor);
        filterPanel.add(genreLabelUI);

        genreField = new JTextField(12);
        genreField.setFont(fieldFont);
        genreField.setBackground(new Color(60, 45, 75));
        genreField.setForeground(textColor);
        genreField.setCaretColor(textColor);
        genreField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accentColor),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        filterPanel.add(genreField);

        JLabel ratingLabelUI = new JLabel("Рейтинг от:");
        ratingLabelUI.setFont(labelFont);
        ratingLabelUI.setForeground(textColor);
        filterPanel.add(ratingLabelUI);

        ratingField = new JTextField(4);
        ratingField.setFont(fieldFont);
        ratingField.setBackground(new Color(60, 45, 75));
        ratingField.setForeground(textColor);
        ratingField.setCaretColor(textColor);
        ratingField.setText("0");
        ratingField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accentColor),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        filterPanel.add(ratingField);

        JButton applyBtn = createStyledButton("Применить", new Color(80, 60, 90));
        applyBtn.addActionListener(e -> applyFilters());
        filterPanel.add(applyBtn);

        topPanel.add(searchPanel);
        topPanel.add(Box.createVerticalStrut(5));
        topPanel.add(filterPanel);

        return topPanel;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        panel.setBackground(panelBg);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        prevButton = createStyledButton("<-", new Color(80, 60, 90));
        prevButton.addActionListener(e -> {
            if (currentPage > 1) {
                currentPage--;
                loadCurrentPage();
            }
        });
        panel.add(prevButton);

        pageInput = new JTextField(3);
        pageInput.setFont(new Font("Arial", Font.PLAIN, 14));
        pageInput.setBackground(new Color(60, 45, 75));
        pageInput.setForeground(textColor);
        pageInput.setCaretColor(textColor);
        pageInput.setText(String.valueOf(currentPage));
        pageInput.setHorizontalAlignment(JTextField.CENTER);
        pageInput.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accentColor),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        pageInput.addActionListener(e -> {
            try {
                int newPage = Integer.parseInt(pageInput.getText());
                if (newPage >= 1 && newPage <= totalPages) {
                    currentPage = newPage;
                    loadCurrentPage();
                } else {
                    pageInput.setText(String.valueOf(currentPage));
                    JOptionPane.showMessageDialog(this,
                            "Страница должна быть от 1 до " + totalPages,
                            "Неверный номер страницы",
                            JOptionPane.WARNING_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                pageInput.setText(String.valueOf(currentPage));
                JOptionPane.showMessageDialog(this,
                        "Введите корректное число",
                        "Ошибка",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        panel.add(pageInput);

        pageLabel = new JLabel("из " + totalPages);
        pageLabel.setFont(new Font("Arial", Font.BOLD, 16));
        pageLabel.setForeground(textColor);
        panel.add(pageLabel);

        nextButton = createStyledButton("->", new Color(80, 60, 90));
        nextButton.addActionListener(e -> {
            if (currentPage < totalPages) {
                currentPage++;
                loadCurrentPage();
            }
        });
        panel.add(nextButton);

        return panel;
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.PLAIN, 14));
        button.setBackground(bgColor);
        button.setForeground(textColor);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor.brighter());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });

        return button;
    }

    private void loadRecommendations() {
        // Проверяем, есть ли данные для рекомендаций
        if (!model.hasEnoughDataForRecommendations()) {
            moviesContainer.removeAll();

            // Создаем панель с сообщением о накоплении данных
            JPanel messagePanel = new JPanel();
            messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
            messagePanel.setBackground(darkBg);
            messagePanel.setBorder(BorderFactory.createEmptyBorder(100, 50, 100, 50));

            JLabel statusLabel = new JLabel(
                    "<html><div style='text-align: center;'>" +
                            " " + model.getCurrentStats() + "<br><br>" +
                            "Добавляйте лайки, просмотры и дизлайки,<br>" +
                            "чтобы появились персональные рекомендации" +
                            "</div></html>",
                    SwingConstants.CENTER
            );
            statusLabel.setFont(new Font("Arial", Font.BOLD, 18));
            statusLabel.setForeground(secondaryText);
            statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel progressLabel = new JLabel(
                    "<html><div style='text-align: center; color: #888888;'>" +
                            "Идет накопление данных для обучения модели...<br>" +
                            "Чем больше действий, тем точнее рекомендации" +
                            "</div></html>",
                    SwingConstants.CENTER
            );
            progressLabel.setFont(new Font("Arial", Font.PLAIN, 14));
            progressLabel.setForeground(new Color(150, 150, 150));
            progressLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            progressLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

            messagePanel.add(statusLabel);
            messagePanel.add(progressLabel);

            moviesContainer.add(messagePanel);
            moviesContainer.revalidate();
            moviesContainer.repaint();

            // Отключаем пагинацию
            pageLabel.setText("из 1");
            pageInput.setText("1");
            prevButton.setEnabled(false);
            nextButton.setEnabled(false);

            return;
        }

        // Показываем статус загрузки
        moviesContainer.removeAll();
        JLabel loadingLabel = new JLabel("Загрузка рекомендаций...", SwingConstants.CENTER);
        loadingLabel.setFont(new Font("Arial", Font.BOLD, 18));
        loadingLabel.setForeground(secondaryText);
        moviesContainer.add(loadingLabel);
        moviesContainer.revalidate();
        moviesContainer.repaint();

        executor.submit(() -> {
            try {
                // Получаем рекомендации от модели
                MahoutIncrementalModel.PersonalizedModel personalModel = model.getPersonalizedModel(userId);

                // Если модель вернула null (нет данных)
                if (personalModel == null) {
                    SwingUtilities.invokeLater(() -> {
                        moviesContainer.removeAll();
                        JLabel errorLabel = new JLabel(
                                "<html>Недостаточно данных для персонализации<br>" +
                                        "Продолжайте добавлять действия</html>",
                                SwingConstants.CENTER
                        );
                        errorLabel.setFont(new Font("Arial", Font.BOLD, 16));
                        errorLabel.setForeground(new Color(200, 100, 100));
                        moviesContainer.add(errorLabel);
                        moviesContainer.revalidate();
                        moviesContainer.repaint();
                    });
                    return;
                }

                recommendedIds = personalModel.recommend(1000);

                SwingUtilities.invokeLater(() -> {
                    isSearchMode = false;
                    currentSearchResults = null;

                    if (recommendedIds == null || recommendedIds.isEmpty()) {
                        moviesContainer.removeAll();
                        JLabel emptyLabel = new JLabel("Нет рекомендаций", SwingConstants.CENTER);
                        emptyLabel.setFont(new Font("Arial", Font.PLAIN, 16));
                        emptyLabel.setForeground(new Color(150, 150, 150));
                        emptyLabel.setBorder(BorderFactory.createEmptyBorder(50, 0, 50, 0));
                        moviesContainer.add(emptyLabel);
                        moviesContainer.revalidate();
                        moviesContainer.repaint();

                        pageLabel.setText("из 1");
                        pageInput.setText("1");
                        prevButton.setEnabled(false);
                        nextButton.setEnabled(false);
                    } else {
                        updateTotalPages();
                        currentPage = 1;
                        loadCurrentPage();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    moviesContainer.removeAll();
                    JLabel errorLabel = new JLabel("Ошибка загрузки", SwingConstants.CENTER);
                    errorLabel.setFont(new Font("Arial", Font.BOLD, 16));
                    errorLabel.setForeground(new Color(200, 100, 100));
                    moviesContainer.add(errorLabel);
                    moviesContainer.revalidate();
                    moviesContainer.repaint();
                });
            }
        });
    }

    private void updateTotalPages() {
        int total;
        String genre = genreField != null ? genreField.getText().trim() : null;
        double rating = 0;
        if (ratingField != null) {
            try {
                rating = Double.parseDouble(ratingField.getText());
            } catch (NumberFormatException e) {
                rating = 0;
            }
        }

        if (isSearchMode && currentSearchResults != null) {
            total = currentSearchResults.size();
        } else if (recommendedIds != null) {
            total = recommendedIds.size();
        } else {
            total = 0;
        }

        totalPages = (int) Math.ceil((double) total / pageSize);
        if (totalPages < 1) totalPages = 1;
    }

    private void loadCurrentPage() {
        new Thread(() -> {
            List<Movie> movies = new ArrayList<>();

            String genre = genreField != null ? genreField.getText().trim() : null;
            double rating = 0;
            if (ratingField != null) {
                try {
                    rating = Double.parseDouble(ratingField.getText());
                } catch (NumberFormatException e) {
                    rating = 0;
                }
            }

            if (isSearchMode && currentSearchResults != null) {
                int startIndex = (currentPage - 1) * pageSize;
                int endIndex = Math.min(startIndex + pageSize, currentSearchResults.size());

                if (startIndex < currentSearchResults.size()) {
                    movies = currentSearchResults.subList(startIndex, endIndex);
                }
            } else if (recommendedIds != null && !recommendedIds.isEmpty()) {
                int startIndex = (currentPage - 1) * pageSize;
                int endIndex = Math.min(startIndex + pageSize, recommendedIds.size());

                for (int i = startIndex; i < endIndex; i++) {
                    int movieId = recommendedIds.get(i);
                    Movie movie = movieDAO.getMovieById(movieId);
                    if (movie != null) {
                        // Проверяем статусы фильма для текущего пользователя
                        checkMovieStatus(movie);
                        movies.add(movie);
                    }
                }
            }

            if ((genre != null && !genre.isEmpty()) || rating > 0) {
                movies = applyFilters(movies, genre, rating);
            }

            List<Movie> finalMovies = movies;

            SwingUtilities.invokeLater(() -> {
                displayMovies(finalMovies);
                pageLabel.setText("из " + totalPages);
                prevButton.setEnabled(currentPage > 1);
                nextButton.setEnabled(currentPage < totalPages);
                pageInput.setText(String.valueOf(currentPage));
            });
        }).start();
    }

    private void checkMovieStatus(Movie movie) {
        // Здесь нужно проверить статусы фильма для текущего пользователя
        // В идеале это должно делаться в одном запросе, но для простоты:
        List<Movie> liked = movieDAO.getLikedMovies(userId, 1, 10000);
        List<Movie> watched = movieDAO.getWatchedMovies(userId, 1, 10000);
        List<Movie> disliked = movieDAO.getDislikedMovies(userId, 1, 10000);

        movie.setLiked(liked.stream().anyMatch(m -> m.getId() == movie.getId()));
        movie.setWatched(watched.stream().anyMatch(m -> m.getId() == movie.getId()));
        movie.setDisliked(disliked.stream().anyMatch(m -> m.getId() == movie.getId()));
    }

    private List<Movie> applyFilters(List<Movie> movies, String genre, double minRating) {
        List<Movie> filtered = new ArrayList<>();

        for (Movie movie : movies) {
            boolean passesGenre = true;
            boolean passesRating = true;

            if (genre != null && !genre.isEmpty()) {
                String movieGenres = movie.getGenres() != null ? movie.getGenres() : "";
                passesGenre = movieGenres.toLowerCase().contains(genre.toLowerCase());
            }

            if (minRating > 0) {
                passesRating = movie.getRating() >= minRating;
            }

            if (passesGenre && passesRating) {
                filtered.add(movie);
            }
        }

        return filtered;
    }

    private void displayMovies(List<Movie> movies) {
        moviesContainer.removeAll();

        if (movies == null || movies.isEmpty()) {
            JLabel emptyLabel = new JLabel("Нет фильмов для отображения", SwingConstants.CENTER);
            emptyLabel.setFont(new Font("Arial", Font.PLAIN, 16));
            emptyLabel.setForeground(new Color(150, 150, 150));
            emptyLabel.setBorder(BorderFactory.createEmptyBorder(50, 0, 50, 0));
            moviesContainer.add(emptyLabel);
        } else {
            for (Movie movie : movies) {
                JPanel movieCard = createMovieCard(movie);
                moviesContainer.add(movieCard);
                moviesContainer.add(Box.createVerticalStrut(10));
            }
        }

        moviesContainer.add(Box.createVerticalStrut(50));
        moviesContainer.revalidate();
        moviesContainer.repaint();

        SwingUtilities.invokeLater(() -> {
            scrollPane.getVerticalScrollBar().setValue(0);
        });
    }

    private JPanel createMovieCard(Movie movie) {
        JPanel card = new JPanel(new BorderLayout(15, 15));
        card.setBackground(cardBg);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accentColor, 1),
                BorderFactory.createEmptyBorder(20, 25, 20, 25)
        ));

        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setBackground(cardBg);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1.0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 5, 0);

        // Название фильма
        JLabel titleLabel = new JLabel(movie.getTitle());
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(textColor);
        infoPanel.add(titleLabel, gbc);

        // Статистика (рейтинг, год, жанры)
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 10, 0);
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        statsPanel.setBackground(cardBg);

        JLabel ratingLabel = new JLabel("* " + String.format("%.1f", movie.getRating()));
        ratingLabel.setFont(new Font("Arial", Font.BOLD, 18));
        ratingLabel.setForeground(new Color(255, 215, 0));
        statsPanel.add(ratingLabel);

        if (movie.getYear() > 0) {
            JLabel yearLabel = new JLabel("(" + movie.getYear() + ")");
            yearLabel.setFont(new Font("Arial", Font.PLAIN, 16));
            yearLabel.setForeground(new Color(200, 200, 200));
            statsPanel.add(yearLabel);
        }

        String displayGenres = parseGenres(movie.getGenres());
        if (!displayGenres.isEmpty()) {
            JLabel genreLabel = new JLabel(displayGenres);
            genreLabel.setFont(new Font("Arial", Font.ITALIC, 16));
            genreLabel.setForeground(secondaryText);
            statsPanel.add(genreLabel);
        }

        infoPanel.add(statsPanel, gbc);

        // Панель с кнопками (ЛАЙК, ПРОСМОТР, ДИЗЛАЙК)
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 10, 0);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonPanel.setBackground(cardBg);

        // Кнопка лайка
        JButton likeBtn = new JButton(movie.isLiked() ? "Лайк" : "Лайк");
        likeBtn.setFont(new Font("Arial", Font.PLAIN, 14));
        likeBtn.setBackground(movie.isLiked() ? new Color(140, 70, 90) : new Color(80, 60, 90));
        likeBtn.setForeground(textColor);
        likeBtn.setFocusPainted(false);
        likeBtn.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        likeBtn.addActionListener(e -> {
            boolean newState = !movie.isLiked();

            // Если ставим лайк, убираем дизлайк
            if (newState && movie.isDisliked()) {
                movieDAO.toggleDislike(userId, movie.getId(), false);
                movie.setDisliked(false);
            }

            movieDAO.toggleLike(userId, movie.getId(), newState);
            movie.setLiked(newState);
            likeBtn.setText(newState ? "Лайк" : "Лайк");
            likeBtn.setBackground(newState ? new Color(140, 70, 90) : new Color(80, 60, 90));

            // Регистрируем изменение для переобучения
            model.handleNewLike(userId, movie.getId());
        });

        // Кнопка просмотра
        JButton watchedBtn = new JButton(movie.isWatched() ? "Просмотрено" : "Просмотрено");
        watchedBtn.setFont(new Font("Arial", Font.PLAIN, 14));
        watchedBtn.setBackground(movie.isWatched() ? watchedColor : new Color(80, 60, 90));
        watchedBtn.setForeground(textColor);
        watchedBtn.setFocusPainted(false);
        watchedBtn.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        watchedBtn.addActionListener(e -> {
            boolean newState = !movie.isWatched();
            movieDAO.toggleWatched(userId, movie.getId(), newState);
            movie.setWatched(newState);
            watchedBtn.setText(newState ? "Просмотрено" : "Просмотрено");
            watchedBtn.setBackground(newState ? watchedColor : new Color(80, 60, 90));

            // Регистрируем изменение для переобучения
            model.handleNewWatch(userId, movie.getId());
        });

        // Кнопка дизлайка
        JButton dislikeBtn = new JButton(movie.isDisliked() ? "Дизлайк" : "Дизлайк");
        dislikeBtn.setFont(new Font("Arial", Font.PLAIN, 14));
        dislikeBtn.setBackground(movie.isDisliked() ? dislikeColor : new Color(80, 60, 90));
        dislikeBtn.setForeground(textColor);
        dislikeBtn.setFocusPainted(false);
        dislikeBtn.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        dislikeBtn.addActionListener(e -> {
            boolean newState = !movie.isDisliked();

            // Если ставим дизлайк, убираем лайк
            if (newState && movie.isLiked()) {
                movieDAO.toggleLike(userId, movie.getId(), false);
                movie.setLiked(false);
                likeBtn.setText("Лайк");
                likeBtn.setBackground(new Color(80, 60, 90));
            }

            movieDAO.toggleDislike(userId, movie.getId(), newState);
            movie.setDisliked(newState);
            dislikeBtn.setText(newState ? "Дизлайк" : "Дизлайк");
            dislikeBtn.setBackground(newState ? dislikeColor : new Color(80, 60, 90));

            // Регистрируем изменение для переобучения
            model.handleNewDislike(userId, movie.getId());
        });

        buttonPanel.add(likeBtn);
        buttonPanel.add(watchedBtn);
        buttonPanel.add(dislikeBtn);
        infoPanel.add(buttonPanel, gbc);

        // Описание
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0;
        gbc.insets = new Insets(0, 0, 10, 0);
        String description = movie.getOverview();
        if (description == null || description.isEmpty() || description.equals("null")) {
            description = "Описание отсутствует";
        }
        JTextArea descArea = new JTextArea(description);
        descArea.setFont(new Font("Arial", Font.PLAIN, 14));
        descArea.setForeground(new Color(220, 220, 220));
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setEditable(false);
        descArea.setBackground(cardBg);
        descArea.setRows(3);
        infoPanel.add(descArea, gbc);

        // Актеры
        gbc.gridy = 4;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 0, 0, 0);

        JPanel actorsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        actorsPanel.setBackground(cardBg);

        String[] actors = movie.getActors();
        if (actors != null && actors.length > 0) {
            for (String actor : actors) {
                if (actor != null && !actor.isEmpty()) {
                    JLabel actorLabel = new JLabel(actor);
                    actorLabel.setFont(new Font("Arial", Font.PLAIN, 14));
                    actorLabel.setForeground(new Color(180, 180, 210));
                    actorsPanel.add(actorLabel);
                }
            }
        } else {
            JLabel noActorsLabel = new JLabel("Информация об актерах отсутствует");
            noActorsLabel.setFont(new Font("Arial", Font.ITALIC, 14));
            noActorsLabel.setForeground(new Color(120, 120, 150));
            actorsPanel.add(noActorsLabel);
        }

        infoPanel.add(actorsPanel, gbc);

        card.add(infoPanel, BorderLayout.CENTER);

        return card;
    }

    private String parseGenres(String genreJson) {
        if (genreJson == null || genreJson.isEmpty() || genreJson.equals("null") || genreJson.equals("[]")) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"name\":\\s*\"([^\"]+)\"");
        java.util.regex.Matcher matcher = pattern.matcher(genreJson);

        while (matcher.find()) {
            if (result.length() > 0) {
                result.append(" • ");
            }
            result.append(matcher.group(1));
        }

        return result.toString();
    }

    private void performSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty() || recommendedIds == null || recommendedIds.isEmpty()) {
            return;
        }

        int searchType = searchTypeCombo.getSelectedIndex();
        currentPage = 1;
        isSearchMode = true;

        new Thread(() -> {
            List<Movie> allMovies = new ArrayList<>();
            for (int id : recommendedIds) {
                Movie m = movieDAO.getMovieById(id);
                if (m != null) {
                    checkMovieStatus(m);
                    allMovies.add(m);
                }
            }

            List<Movie> searchResults = new ArrayList<>();
            String lowerQuery = query.toLowerCase();

            for (Movie movie : allMovies) {
                if (searchType == 0) {
                    if (movie.getTitle().toLowerCase().contains(lowerQuery)) {
                        searchResults.add(movie);
                    }
                } else {
                    String[] actors = movie.getActors();
                    if (actors != null) {
                        for (String actor : actors) {
                            if (actor.toLowerCase().contains(lowerQuery)) {
                                searchResults.add(movie);
                                break;
                            }
                        }
                    }
                }
            }

            currentSearchResults = searchResults;
            updateTotalPages();

            SwingUtilities.invokeLater(() -> {
                currentPage = 1;
                loadCurrentPage();
            });
        }).start();
    }

    private void applyFilters() {
        isSearchMode = false;
        currentSearchResults = null;
        updateTotalPages();
        currentPage = 1;
        loadCurrentPage();
    }

    private void resetSearch() {
        searchField.setText("");
        genreField.setText("");
        ratingField.setText("0");
        isSearchMode = false;
        currentSearchResults = null;
        updateTotalPages();
        currentPage = 1;
        loadCurrentPage();
    }

    public void cleanup() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        cleanup();
    }
}