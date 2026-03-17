package org.example.movieSettings;

import org.example.Program;
import org.example.models.ModelInitializer;
import org.example.models.MahoutIncrementalModel;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public abstract class MoviesPanel extends JPanel {
    protected MovieDAO movieDAO;
    protected int userId;
    protected MahoutIncrementalModel model;
    protected Program parentProgram;

    // Компоненты UI
    protected JPanel moviesContainer;
    protected JScrollPane scrollPane;
    protected JPanel bottomPanel;
    protected JLabel countLabel; // Счётчик фильмов

    // Пагинация
    protected int currentPage = 1;
    protected int totalPages = 1;
    protected int pageSize = 20;
    protected JLabel pageLabel;
    protected JButton prevButton;
    protected JButton nextButton;
    protected JTextField pageInput;

    // Фильтры и поиск
    protected JTextField searchField;
    protected JComboBox<String> searchTypeCombo;
    protected JTextField genreField;
    protected JTextField ratingField;
    protected List<Movie> currentSearchResults;
    protected boolean isSearchMode = false;

    // Для синхронизации между панелями
    protected List<MoviesPanel> allPanels;
    protected DislikedMoviesPanel dislikedPanel;

    // Цветовая схема
    protected final Color darkBg = new Color(25, 15, 30);
    protected final Color panelBg = new Color(35, 25, 45);
    protected final Color cardBg = new Color(45, 30, 55);
    protected final Color likeColor = new Color(140, 70, 90);
    protected final Color dislikeColor = new Color(140, 70, 70);
    protected final Color watchedColor = new Color(60, 100, 80);
    protected final Color defaultColor = new Color(80, 60, 90);
    protected final Color accentColor = new Color(110, 70, 140);
    protected final Color textColor = Color.WHITE;
    protected final Color secondaryText = new Color(180, 150, 200);

    public MoviesPanel(MovieDAO dao, int userId, List<MoviesPanel> panels) {
        this.movieDAO = dao;
        this.userId = userId;
        this.model = ModelInitializer.getModel();
        this.allPanels = panels;

        setLayout(new BorderLayout());
        setBackground(darkBg);

        moviesContainer = new JPanel();
        moviesContainer.setLayout(new BoxLayout(moviesContainer, BoxLayout.Y_AXIS));
        moviesContainer.setBackground(darkBg);

        scrollPane = new JScrollPane(moviesContainer);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(darkBg);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        JPanel topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);

        bottomPanel = createBottomPanel();
        add(bottomPanel, BorderLayout.SOUTH);

        add(scrollPane, BorderLayout.CENTER);
    }

    protected abstract List<Movie> loadMovies(int page, int pageSize);
    protected abstract int getTotalCount();
    protected abstract String getEmptyMessage();
    protected abstract Color getAccentColor();
    protected abstract void handleLike(Movie movie, JButton likeBtn, JButton dislikeBtn);
    protected abstract void handleDislike(Movie movie, JButton likeBtn, JButton dislikeBtn);
    protected abstract void handleWatch(Movie movie, JButton watchedBtn);

    protected JPanel createTopPanel() {
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBackground(panelBg);
        topPanel.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 25));

        Font labelFont = new Font("Arial", Font.BOLD, 14);
        Font fieldFont = new Font("Arial", Font.PLAIN, 14);

        // Верхняя строка с заголовком и счётчиком
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(panelBg);
        titlePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        titlePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JLabel titleLabel = new JLabel(getPanelTitle());
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(textColor);
        titlePanel.add(titleLabel, BorderLayout.WEST);

        countLabel = new JLabel("0");
        countLabel.setFont(new Font("Arial", Font.BOLD, 18));
        countLabel.setForeground(accentColor);
        countLabel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
        titlePanel.add(countLabel, BorderLayout.CENTER);

        topPanel.add(titlePanel);

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
                BorderFactory.createLineBorder(getAccentColor()),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        searchField.addActionListener(e -> performSearch());
        searchPanel.add(searchField);

        String[] searchTypes = {"Название", "Актер"};
        searchTypeCombo = new JComboBox<>(searchTypes);
        searchTypeCombo.setFont(fieldFont);
        searchTypeCombo.setBackground(new Color(60, 45, 75));
        searchTypeCombo.setForeground(textColor);
        searchTypeCombo.setBorder(BorderFactory.createLineBorder(getAccentColor()));
        searchPanel.add(searchTypeCombo);

        JButton searchBtn = createStyledButton("Найти", getAccentColor());
        searchBtn.addActionListener(e -> performSearch());
        searchPanel.add(searchBtn);

        JButton resetBtn = createStyledButton("Сброс", defaultColor);
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
                BorderFactory.createLineBorder(getAccentColor()),
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
                BorderFactory.createLineBorder(getAccentColor()),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        filterPanel.add(ratingField);

        JButton applyBtn = createStyledButton("Применить", defaultColor);
        applyBtn.addActionListener(e -> applyFilters());
        filterPanel.add(applyBtn);

        topPanel.add(searchPanel);
        topPanel.add(Box.createVerticalStrut(5));
        topPanel.add(filterPanel);

        return topPanel;
    }

    // Новый метод - название панели
    protected abstract String getPanelTitle();

    // Новый метод - обновление счётчика
    public void updateCount() {
        new Thread(() -> {
            int count = getTotalCount();
            SwingUtilities.invokeLater(() -> {
                countLabel.setText(String.valueOf(count));
            });
        }).start();
    }

    protected JPanel createBottomPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        panel.setBackground(panelBg);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        prevButton = createStyledButton("<-", defaultColor);
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
                BorderFactory.createLineBorder(getAccentColor()),
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

        nextButton = createStyledButton("->", defaultColor);
        nextButton.addActionListener(e -> {
            if (currentPage < totalPages) {
                currentPage++;
                loadCurrentPage();
            }
        });
        panel.add(nextButton);

        return panel;
    }

    protected JButton createStyledButton(String text, Color bgColor) {
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

    protected void loadFirstPage() {
        currentPage = 1;
        isSearchMode = false;
        currentSearchResults = null;
        updateTotalPages();
        loadCurrentPage();
        updateCount(); // Обновляем счётчик
    }

    protected void updateTotalPages() {
        int total = isSearchMode && currentSearchResults != null ?
                currentSearchResults.size() : getTotalCount();
        totalPages = (int) Math.ceil((double) total / pageSize);
        if (totalPages < 1) totalPages = 1;
    }

    protected void loadCurrentPage() {
        new Thread(() -> {
            List<Movie> movies;

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
                } else {
                    movies = new ArrayList<>();
                }
            } else {
                movies = loadMovies(currentPage, pageSize);
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

    protected List<Movie> applyFilters(List<Movie> movies, String genre, double minRating) {
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

    protected void displayMovies(List<Movie> movies) {
        moviesContainer.removeAll();

        if (movies.isEmpty()) {
            JLabel emptyLabel = new JLabel(getEmptyMessage(), SwingConstants.CENTER);
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

    protected JPanel createMovieCard(Movie movie) {
        JPanel card = new JPanel(new BorderLayout(15, 15));
        card.setBackground(cardBg);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(getAccentColor(), 1),
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

        JLabel titleLabel = new JLabel(movie.getTitle());
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(textColor);
        infoPanel.add(titleLabel, gbc);

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

        JPanel buttonPanel = createButtonPanel(movie);
        if (buttonPanel != null) {
            gbc.gridy = 2;
            gbc.insets = new Insets(0, 0, 10, 0);
            infoPanel.add(buttonPanel, gbc);
        }

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

    protected JPanel createButtonPanel(Movie movie) {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonPanel.setBackground(cardBg);

        JButton likeBtn = new JButton("Лайк");
        likeBtn.setFont(new Font("Arial", Font.PLAIN, 14));
        likeBtn.setBackground(movie.isLiked() ? likeColor : defaultColor);
        likeBtn.setForeground(textColor);
        likeBtn.setFocusPainted(false);
        likeBtn.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));

        JButton watchedBtn = new JButton("Просмотрено");
        watchedBtn.setFont(new Font("Arial", Font.PLAIN, 14));
        watchedBtn.setBackground(movie.isWatched() ? watchedColor : defaultColor);
        watchedBtn.setForeground(textColor);
        watchedBtn.setFocusPainted(false);
        watchedBtn.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));

        JButton dislikeBtn = new JButton("Дизлайк");
        dislikeBtn.setFont(new Font("Arial", Font.PLAIN, 14));
        dislikeBtn.setBackground(movie.isDisliked() ? dislikeColor : defaultColor);
        dislikeBtn.setForeground(textColor);
        dislikeBtn.setFocusPainted(false);
        dislikeBtn.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));

        likeBtn.addActionListener(e -> {
            handleLike(movie, likeBtn, dislikeBtn);
            updateCount(); // Обновляем счётчик после действия
        });

        watchedBtn.addActionListener(e -> {
            handleWatch(movie, watchedBtn);
            updateCount(); // Обновляем счётчик после действия
        });

        dislikeBtn.addActionListener(e -> {
            handleDislike(movie, likeBtn, dislikeBtn);
            updateCount(); // Обновляем счётчик после действия
        });

        buttonPanel.add(likeBtn);
        buttonPanel.add(watchedBtn);
        buttonPanel.add(dislikeBtn);

        return buttonPanel;
    }

    protected String parseGenres(String genreJson) {
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

    protected void performSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            return;
        }

        int searchType = searchTypeCombo.getSelectedIndex();
        currentPage = 1;
        isSearchMode = true;

        new Thread(() -> {
            List<Movie> allMovies = loadMovies(1, 1000);
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
                loadCurrentPage();
            });
        }).start();
    }

    protected void applyFilters() {
        isSearchMode = false;
        currentSearchResults = null;
        currentPage = 1;
        updateTotalPages();
        loadCurrentPage();
    }

    protected void resetSearch() {
        searchField.setText("");
        genreField.setText("");
        ratingField.setText("0");
        isSearchMode = false;
        currentSearchResults = null;
        currentPage = 1;
        updateTotalPages();
        loadCurrentPage();
        updateCount(); // Обновляем счётчик
    }

    protected void refreshAllPanels() {
        if (allPanels != null) {
            for (MoviesPanel panel : allPanels) {
                if (panel != this) {
                    panel.refreshIfNeeded();
                }
            }
        }
    }

    public void refreshIfNeeded() {
        updateTotalPages();
        if (currentPage > totalPages) {
            currentPage = 1;
        }
        loadCurrentPage();
        updateCount(); // Обновляем счётчик
    }

    public void setParentProgram(Program program) {
        this.parentProgram = program;
    }

    public void setDislikedPanel(DislikedMoviesPanel panel) {
        this.dislikedPanel = panel;
    }

    public void refreshMovieState(int movieId) {
        loadCurrentPage();
        updateCount(); // Обновляем счётчик
    }
}