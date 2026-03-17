package org.example;

import org.example.firstWindow.LoginWindow;
import org.example.movieSettings.*;
import org.example.userSettings.FriendsPanel;
import org.example.userSettings.ProfileAnalyticsPanel;
import org.example.userSettings.User;
import org.example.userSettings.UserDAO;
import org.example.models.ModelInitializer;
import org.example.movieSettings.RecommendationsPanel;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.List;

public class Program extends JFrame {
    private JTabbedPane tabbedPane;
    private User currentUser;

    private AllMoviesPanel allMoviesPanel;
    private LikedMoviesPanel likedMoviesPanel;
    private WatchedMoviesPanel watchedMoviesPanel;
    private DislikedMoviesPanel dislikedMoviesPanel;
    private RecommendationsPanel recommendationsPanel;
    private FriendsPanel friendsPanel;

    private MovieDAO movieDAO;
    private UserDAO userDAO;
    private List<MoviesPanel> moviesPanels = new ArrayList<>();

    public Program(User user) {
        ModelInitializer.getModel();

        this.currentUser = user;
        this.movieDAO = new MovieDAO();
        this.userDAO = new UserDAO();

        setTitle("MyMovie - " + user.getUsername());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 800));
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        getContentPane().setBackground(new Color(25, 15, 30));
        setIcon();

        tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(new Color(35, 25, 45));
        tabbedPane.setForeground(Color.WHITE);
        tabbedPane.setFont(new Font("Arial", Font.BOLD, 16));

        allMoviesPanel = new AllMoviesPanel(movieDAO, currentUser.getId(), moviesPanels);
        likedMoviesPanel = new LikedMoviesPanel(movieDAO, currentUser.getId(), moviesPanels);
        watchedMoviesPanel = new WatchedMoviesPanel(movieDAO, currentUser.getId(), moviesPanels);
        dislikedMoviesPanel = new DislikedMoviesPanel(movieDAO, currentUser.getId(), moviesPanels);

        moviesPanels.add(allMoviesPanel);
        moviesPanels.add(likedMoviesPanel);
        moviesPanels.add(watchedMoviesPanel);
        moviesPanels.add(dislikedMoviesPanel);

        allMoviesPanel.setParentProgram(this);
        likedMoviesPanel.setParentProgram(this);
        watchedMoviesPanel.setParentProgram(this);
        dislikedMoviesPanel.setParentProgram(this);

        allMoviesPanel.setDislikedPanel(dislikedMoviesPanel);
        likedMoviesPanel.setDislikedPanel(dislikedMoviesPanel);
        watchedMoviesPanel.setDislikedPanel(dislikedMoviesPanel);
        dislikedMoviesPanel.setDislikedPanel(dislikedMoviesPanel);

        // Простые названия вкладок без счётчиков
        tabbedPane.addTab("Все фильмы", allMoviesPanel);
        tabbedPane.addTab("Любимые", likedMoviesPanel);
        tabbedPane.addTab("Просмотренные", watchedMoviesPanel);
        tabbedPane.addTab("Не понравились", dislikedMoviesPanel);

        recommendationsPanel = new RecommendationsPanel(currentUser.getId(), currentUser.getUsername());
        tabbedPane.addTab("Рекомендации", recommendationsPanel);

        friendsPanel = new FriendsPanel(currentUser, userDAO, movieDAO);
        tabbedPane.addTab("Друзья", friendsPanel);

        // АНАЛИТИКА//
        ProfileAnalyticsPanel analyticsPanel = new ProfileAnalyticsPanel(currentUser, movieDAO, userDAO);
        tabbedPane.addTab("Моя статистика", analyticsPanel);
        // АНАЛИТИКА //

        JPanel topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(35, 25, 45));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JLabel userLabel = new JLabel(currentUser.getUsername());
        userLabel.setFont(new Font("Arial", Font.BOLD, 16));
        userLabel.setForeground(Color.WHITE);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(new Color(35, 25, 45));

        JButton switchUserBtn = createStyledButton("Сменить пользователя", new Color(80, 60, 90));
        switchUserBtn.addActionListener(e -> switchUser());

        JButton logoutBtn = createStyledButton("Выйти", new Color(80, 60, 90));
        logoutBtn.addActionListener(e -> logout());

        buttonPanel.add(switchUserBtn);
        buttonPanel.add(logoutBtn);

        panel.add(userLabel, BorderLayout.WEST);
        panel.add(buttonPanel, BorderLayout.EAST);

        return panel;
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.PLAIN, 14));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
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

    private void switchUser() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Выйти из текущего профиля?",
                "Смена пользователя",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            new LoginWindow().setVisible(true);
            dispose();
        }
    }

    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Вы действительно хотите выйти?",
                "Выход",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            new LoginWindow().setVisible(true);
            dispose();
        }
    }

    private void setIcon() {
        try {
            File iconFile = new File("src/images/iconFrame.png");
            if (iconFile.exists()) {
                Image img = ImageIO.read(iconFile);
                setIconImage(img);
            }
        } catch (IOException e) {
            System.out.println("Иконка не загружена");
        }
    }

    public void refreshAllPanelsForMovie(int movieId) {
        for (MoviesPanel panel : moviesPanels) {
            panel.refreshMovieState(movieId);
        }
    }

    @Override
    public void dispose() {
        ModelInitializer.shutdown();
        super.dispose();
    }
}