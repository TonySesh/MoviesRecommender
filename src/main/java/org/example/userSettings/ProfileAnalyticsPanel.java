package org.example.userSettings;

import org.example.movieSettings.Movie;
import org.example.movieSettings.MovieDAO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;

public class ProfileAnalyticsPanel extends JPanel {
    private final User currentUser;
    private final MovieDAO movieDAO;
    private final UserDAO userDAO;

    private final JPanel statsPanel;
    private final JPanel genresPanel;

    // Цветовая схема как в остальных вкладках
    private final Color darkBg = new Color(25, 15, 30);
    private final Color panelBg = new Color(35, 25, 45);
    private final Color cardBg = new Color(45, 30, 55);
    private final Color accentColor = new Color(110, 70, 140);
    private final Color textColor = Color.WHITE;
    private final Color secondaryText = new Color(180, 150, 200);

    public ProfileAnalyticsPanel(User user, MovieDAO mDao, UserDAO uDao) {
        this.currentUser = user;
        this.movieDAO = mDao;
        this.userDAO = uDao;

        setLayout(new BorderLayout(0, 20));
        setBackground(darkBg);
        setBorder(new EmptyBorder(25, 25, 25, 25));

        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        JPanel mainPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        mainPanel.setBackground(darkBg);

        statsPanel = createStatsPanel();
        mainPanel.add(statsPanel);

        genresPanel = createGenresPanel();
        mainPanel.add(genresPanel);

        add(mainPanel, BorderLayout.CENTER);

        loadAnalytics();
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(panelBg);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, accentColor),
                new EmptyBorder(15, 20, 15, 20)
        ));

        JLabel titleLabel = new JLabel(currentUser.getUsername() + ", твоя статистика");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(textColor);

        panel.add(titleLabel, BorderLayout.WEST);

        return panel;
    }

    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(cardBg);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accentColor, 1),
                new EmptyBorder(20, 20, 20, 20)
        ));

        JLabel title = new JLabel("Общая статистика");
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setForeground(accentColor);
        title.setBorder(new EmptyBorder(0, 0, 15, 0));
        panel.add(title, BorderLayout.NORTH);

        JPanel content = new JPanel(new GridLayout(4, 1, 0, 15));
        content.setBackground(cardBg);

        content.add(createStatCard("Всего оценено", "0"));
        content.add(createStatCard("Любимые", "0"));
        content.add(createStatCard("Просмотренные", "0"));
        content.add(createStatCard("Не понравились", "0"));

        panel.add(content, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createGenresPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(cardBg);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accentColor, 1),
                new EmptyBorder(20, 20, 20, 20)
        ));

        JLabel title = new JLabel("Любимые жанры");
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setForeground(accentColor);
        title.setBorder(new EmptyBorder(0, 0, 15, 0));
        panel.add(title, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(cardBg);

        for (int i = 0; i < 5; i++) {
            content.add(createGenreCard("Загрузка...", 0));
            content.add(Box.createVerticalStrut(8));
        }

        panel.add(content, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createStatCard(String label, String value) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(cardBg);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        JLabel labelComp = new JLabel(label);
        labelComp.setFont(new Font("Arial", Font.PLAIN, 16));
        labelComp.setForeground(secondaryText);

        JLabel valueComp = new JLabel(value);
        valueComp.setFont(new Font("Arial", Font.BOLD, 24));
        valueComp.setForeground(accentColor);
        valueComp.setHorizontalAlignment(SwingConstants.RIGHT);

        card.add(labelComp, BorderLayout.WEST);
        card.add(valueComp, BorderLayout.EAST);

        return card;
    }

    private JPanel createGenreCard(String genre, int count) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(cardBg);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

        JLabel genreLabel = new JLabel(genre);
        genreLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        genreLabel.setForeground(textColor);

        JLabel countLabel = new JLabel(String.valueOf(count));
        countLabel.setFont(new Font("Arial", Font.BOLD, 18));
        countLabel.setForeground(accentColor);
        countLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        card.add(genreLabel, BorderLayout.WEST);
        card.add(countLabel, BorderLayout.EAST);

        return card;
    }

    private void loadAnalytics() {
        new Thread(() -> {
            try {
                List<Movie> liked = movieDAO.getLikedMovies(currentUser.getId(), 1, 1000);
                List<Movie> watched = movieDAO.getWatchedMovies(currentUser.getId(), 1, 1000);
                List<Movie> disliked = movieDAO.getDislikedMovies(currentUser.getId(), 1, 1000);

                int total = liked.size() + watched.size() + disliked.size();

                // Анализ жанров
                Map<String, Integer> genreCount = new HashMap<>();

                for (Movie m : liked) {
                    List<String> genres = parseGenresList(m.getGenres());
                    for (String g : genres) {
                        genreCount.put(g, genreCount.getOrDefault(g, 0) + 1);
                    }
                }

                List<Map.Entry<String, Integer>> sortedGenres = new ArrayList<>(genreCount.entrySet());
                sortedGenres.sort((a, b) -> b.getValue().compareTo(a.getValue()));

                SwingUtilities.invokeLater(() -> {
                    updateStatsPanel(total, liked.size(), watched.size(), disliked.size());
                    updateGenresPanel(sortedGenres);
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void updateStatsPanel(int total, int liked, int watched, int disliked) {
        JPanel content = (JPanel) statsPanel.getComponent(1);
        content.removeAll();

        content.add(createStatCard("Всего оценено", String.valueOf(total)));
        content.add(createStatCard("Любимые", String.valueOf(liked)));
        content.add(createStatCard("Просмотренные", String.valueOf(watched)));
        content.add(createStatCard("Не понравились", String.valueOf(disliked)));

        content.revalidate();
        content.repaint();
    }

    private void updateGenresPanel(List<Map.Entry<String, Integer>> sortedGenres) {
        JPanel content = (JPanel) genresPanel.getComponent(1);
        content.removeAll();

        int count = 0;
        for (Map.Entry<String, Integer> entry : sortedGenres) {
            if (count >= 5) break;
            content.add(createGenreCard(entry.getKey(), entry.getValue()));
            content.add(Box.createVerticalStrut(8));
            count++;
        }

        if (count == 0) {
            JLabel emptyLabel = new JLabel("Нет данных о жанрах");
            emptyLabel.setFont(new Font("Arial", Font.ITALIC, 16));
            emptyLabel.setForeground(secondaryText);
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            emptyLabel.setBorder(new EmptyBorder(20, 0, 20, 0));
            content.add(emptyLabel);
        }

        content.revalidate();
        content.repaint();
    }

    private List<String> parseGenresList(String genresJson) {
        List<String> genres = new ArrayList<>();
        if (genresJson == null || genresJson.isEmpty() || genresJson.equals("null") || genresJson.equals("[]")) {
            return genres;
        }

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"name\":\\s*\"([^\"]+)\"");
        java.util.regex.Matcher matcher = pattern.matcher(genresJson);
        while (matcher.find()) {
            genres.add(matcher.group(1));
        }
        return genres;
    }
}