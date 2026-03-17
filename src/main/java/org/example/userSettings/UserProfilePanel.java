package org.example.userSettings;

import org.example.movieSettings.MovieDAO;
import org.example.movieSettings.Movie;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public class UserProfilePanel extends JPanel {
    private User targetUser;
    private User currentUser;
    private MovieDAO movieDAO;
    private UserDAO userDAO;
    private JTabbedPane profileTabbedPane;

    public UserProfilePanel(User targetUser, User currentUser, MovieDAO mDao, UserDAO uDao) {
        this.targetUser = targetUser;
        this.currentUser = currentUser;
        this.movieDAO = mDao;
        this.userDAO = uDao;

        setLayout(new BorderLayout());
        setBackground(new Color(25, 15, 30));

        JPanel topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);

        profileTabbedPane = new JTabbedPane();
        profileTabbedPane.setBackground(new Color(35, 25, 45));
        profileTabbedPane.setForeground(Color.WHITE);
        profileTabbedPane.setFont(new Font("Arial", Font.BOLD, 14));

        JPanel likedPanel = createUserMoviesPanel("liked");
        profileTabbedPane.addTab("Любимые", likedPanel);

        JPanel watchedPanel = createUserMoviesPanel("watched");
        profileTabbedPane.addTab("Просмотренные", watchedPanel);

        JPanel dislikedPanel = createUserMoviesPanel("disliked");
        profileTabbedPane.addTab("Не понравились", dislikedPanel);

        updateTabTitles();

        add(profileTabbedPane, BorderLayout.CENTER);
    }

    private void updateTabTitles() {
        new Thread(() -> {
            int likedCount = movieDAO.getTotalLikedCount(targetUser.getId());
            int watchedCount = movieDAO.getTotalWatchedCount(targetUser.getId());
            int dislikedCount = movieDAO.getTotalDislikedCount(targetUser.getId());

            SwingUtilities.invokeLater(() -> {
                profileTabbedPane.setTitleAt(0, "Любимые (" + likedCount + ")");
                profileTabbedPane.setTitleAt(1, "Просмотренные (" + watchedCount + ")");
                profileTabbedPane.setTitleAt(2, "Не понравились (" + dislikedCount + ")");
            });
        }).start();
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(35, 25, 45));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel avatarLabel = new JLabel(" ");
        avatarLabel.setFont(new Font("Arial", Font.PLAIN, 48));
        avatarLabel.setForeground(new Color(180, 150, 200));

        JPanel infoPanel = new JPanel(new GridLayout(3, 1));
        infoPanel.setBackground(new Color(35, 25, 45));
        infoPanel.setBorder(new EmptyBorder(0, 20, 0, 0));

        JLabel nameLabel = new JLabel(targetUser.getUsername());
        nameLabel.setFont(new Font("Arial", Font.BOLD, 24));
        nameLabel.setForeground(Color.WHITE);

        JLabel statusLabel = new JLabel(getFriendStatusText());
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        statusLabel.setForeground(new Color(180, 150, 200));

        JLabel statsLabel = new JLabel(getUserStats());
        statsLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        statsLabel.setForeground(new Color(150, 150, 150));

        infoPanel.add(nameLabel);
        infoPanel.add(statusLabel);
        infoPanel.add(statsLabel);

        JButton actionBtn = createActionButton();

        panel.add(avatarLabel, BorderLayout.WEST);
        panel.add(infoPanel, BorderLayout.CENTER);

        if (actionBtn != null) {
            panel.add(actionBtn, BorderLayout.EAST);
        }

        return panel;
    }

    private String getFriendStatusText() {
        String status = userDAO.getFriendStatus(currentUser.getId(), targetUser.getId());
        switch (status) {
            case "accepted":
                return "У вас в друзьях";
            case "pending":
                return "Ожидает подтверждения";
            case "incoming":
                return "Хочет добавить вас в друзья";
            default:
                return "";
        }
    }

    private String getUserStats() {
        int likedCount = movieDAO.getTotalLikedCount(targetUser.getId());
        int watchedCount = movieDAO.getTotalWatchedCount(targetUser.getId());
        int dislikedCount = movieDAO.getTotalDislikedCount(targetUser.getId());
        return String.format("%d лайков • %d просмотрено • %d не понравилось",
                likedCount, watchedCount, dislikedCount);
    }

    private JButton createActionButton() {
        String status = userDAO.getFriendStatus(currentUser.getId(), targetUser.getId());
        JButton button = null;

        if (targetUser.getId() == currentUser.getId()) {
            return null;
        }

        switch (status) {
            case "none":
                button = new JButton("Добавить в друзья");
                button.setBackground(new Color(110, 70, 140));
                button.addActionListener(e -> sendFriendRequest());
                break;
            case "accepted":
                button = new JButton("Удалить из друзей");
                button.setBackground(new Color(140, 70, 70));
                button.addActionListener(e -> removeFriend());
                break;
            case "pending":
                button = new JButton("Заявка отправлена");
                button.setBackground(new Color(80, 80, 80));
                button.setEnabled(false);
                break;
            case "incoming":
                button = new JButton("Принять заявку");
                button.setBackground(new Color(200, 100, 0));
                button.addActionListener(e -> acceptFriendRequest());
                break;
        }

        if (button != null) {
            button.setFont(new Font("Arial", Font.BOLD, 14));
            button.setForeground(Color.WHITE);
            button.setFocusPainted(false);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.WHITE, 1),
                    BorderFactory.createEmptyBorder(10, 20, 10, 20)
            ));
            button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        return button;
    }

    private void sendFriendRequest() {
        boolean success = userDAO.sendFriendRequest(currentUser.getId(), targetUser.getId());
        if (success) {
            JOptionPane.showMessageDialog(this,
                    "Заявка отправлена пользователю " + targetUser.getUsername(),
                    "Успех",
                    JOptionPane.INFORMATION_MESSAGE);
            refreshProfile();
        }
    }

    private void removeFriend() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Удалить " + targetUser.getUsername() + " из друзей?",
                "Подтверждение",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            boolean success = userDAO.removeFriend(currentUser.getId(), targetUser.getId());
            if (success) {
                JOptionPane.showMessageDialog(this,
                        "Пользователь " + targetUser.getUsername() + " удален из друзей",
                        "Успех",
                        JOptionPane.INFORMATION_MESSAGE);
                refreshProfile();
            }
        }
    }

    private void acceptFriendRequest() {
        boolean success = userDAO.acceptFriendRequest(currentUser.getId(), targetUser.getId());
        if (success) {
            JOptionPane.showMessageDialog(this,
                    "Вы приняли заявку в друзья от " + targetUser.getUsername(),
                    "Успех",
                    JOptionPane.INFORMATION_MESSAGE);
            refreshProfile();
        }
    }

    private void refreshProfile() {
        removeAll();
        JPanel topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);

        updateTabTitles();

        profileTabbedPane.removeAll();

        JPanel likedPanel = createUserMoviesPanel("liked");
        profileTabbedPane.addTab("Любимые", likedPanel);

        JPanel watchedPanel = createUserMoviesPanel("watched");
        profileTabbedPane.addTab("Просмотренные", watchedPanel);

        JPanel dislikedPanel = createUserMoviesPanel("disliked");
        profileTabbedPane.addTab("Не понравились", dislikedPanel);

        add(profileTabbedPane, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    private JPanel createUserMoviesPanel(String mode) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(25, 15, 30));

        JPanel moviesContainer = new JPanel();
        moviesContainer.setLayout(new BoxLayout(moviesContainer, BoxLayout.Y_AXIS));
        moviesContainer.setBackground(new Color(25, 15, 30));

        JScrollPane scrollPane = new JScrollPane(moviesContainer);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(new Color(25, 15, 30));

        panel.add(scrollPane, BorderLayout.CENTER);

        loadUserMovies(mode, moviesContainer);

        return panel;
    }

    private void loadUserMovies(String mode, JPanel container) {
        new Thread(() -> {
            List<Movie> movies;

            switch (mode) {
                case "liked":
                    movies = movieDAO.getLikedMovies(targetUser.getId(), 1, 50);
                    break;
                case "watched":
                    movies = movieDAO.getWatchedMovies(targetUser.getId(), 1, 50);
                    break;
                case "disliked":
                    movies = movieDAO.getDislikedMovies(targetUser.getId(), 1, 50);
                    break;
                default:
                    movies = new java.util.ArrayList<>();
            }

            SwingUtilities.invokeLater(() -> {
                container.removeAll();

                if (movies.isEmpty()) {
                    String message;
                    switch (mode) {
                        case "liked":
                            message = "У пользователя пока нет любимых фильмов";
                            break;
                        case "watched":
                            message = "У пользователя пока нет просмотренных фильмов";
                            break;
                        case "disliked":
                            message = "У пользователя пока нет фильмов, которые ему не понравились";
                            break;
                        default:
                            message = "Нет фильмов для отображения";
                    }

                    JLabel emptyLabel = new JLabel(message, SwingConstants.CENTER);
                    emptyLabel.setFont(new Font("Arial", Font.PLAIN, 16));
                    emptyLabel.setForeground(new Color(150, 150, 150));
                    emptyLabel.setBorder(new EmptyBorder(50, 0, 50, 0));
                    container.add(emptyLabel);
                } else {
                    for (Movie movie : movies) {
                        JPanel movieCard = createReadOnlyMovieCard(movie, mode);
                        container.add(movieCard);
                        container.add(Box.createVerticalStrut(10));
                    }
                }

                container.revalidate();
                container.repaint();
            });
        }).start();
    }

    private JPanel createReadOnlyMovieCard(Movie movie, String mode) {
        JPanel card = new JPanel(new BorderLayout(15, 15));
        card.setBackground(new Color(45, 30, 55));

        Color borderColor;
        switch (mode) {
            case "liked":
                borderColor = new Color(140, 70, 90);
                break;
            case "watched":
                borderColor = new Color(60, 100, 80);
                break;
            case "disliked":
                borderColor = new Color(140, 70, 70);
                break;
            default:
                borderColor = new Color(100, 70, 120);
        }

        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor, 1),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));

        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setBackground(new Color(45, 30, 55));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1.0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 5, 0);

        JLabel titleLabel = new JLabel(movie.getTitle());
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        infoPanel.add(titleLabel, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 10, 0);
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        statsPanel.setBackground(new Color(45, 30, 55));

        JLabel ratingLabel = new JLabel("* " + String.format("%.1f", movie.getRating()));
        ratingLabel.setFont(new Font("Arial", Font.BOLD, 14));
        ratingLabel.setForeground(new Color(255, 215, 0));
        statsPanel.add(ratingLabel);

        if (movie.getYear() > 0) {
            JLabel yearLabel = new JLabel("(" + movie.getYear() + ")");
            yearLabel.setFont(new Font("Arial", Font.PLAIN, 14));
            yearLabel.setForeground(new Color(200, 200, 200));
            statsPanel.add(yearLabel);
        }

        infoPanel.add(statsPanel, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 5, 0);
        String displayGenres = parseGenres(movie.getGenres());
        if (!displayGenres.isEmpty()) {
            JLabel genreLabel = new JLabel(displayGenres);
            genreLabel.setFont(new Font("Arial", Font.ITALIC, 14));
            genreLabel.setForeground(new Color(180, 150, 200));
            infoPanel.add(genreLabel, gbc);
        }

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
}