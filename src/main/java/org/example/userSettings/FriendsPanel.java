package org.example.userSettings;

import org.example.movieSettings.MovieDAO;
import org.example.userSettings.UserProfilePanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class FriendsPanel extends JPanel {
    private final User currentUser;
    private final UserDAO userDAO;
    private final MovieDAO movieDAO;

    private JTabbedPane friendsTabbedPane;
    private JPanel searchPanel;
    private JPanel friendsListPanel;
    private JPanel requestsPanel;

    private JTextField searchField;
    private JPanel searchResultsPanel;
    private JScrollPane searchScrollPane;

    private JPanel friendsCardsPanel;
    private JScrollPane friendsScrollPane;

    private JPanel incomingCardsPanel;
    private JPanel outgoingCardsPanel;

    public FriendsPanel(User user, UserDAO uDao, MovieDAO mDao) {
        this.currentUser = user;
        this.userDAO = uDao;
        this.movieDAO = mDao;

        setLayout(new BorderLayout());
        setBackground(new Color(25, 15, 30));

        // Верхняя панель с заголовком
        JPanel topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);

        // Центральная панель с вкладками
        friendsTabbedPane = new JTabbedPane();
        friendsTabbedPane.setBackground(new Color(35, 25, 45));
        friendsTabbedPane.setForeground(Color.WHITE);
        friendsTabbedPane.setFont(new Font("Arial", Font.BOLD, 14));

        // Вкладка поиска
        searchPanel = createSearchPanel();
        friendsTabbedPane.addTab("Поиск пользователей", searchPanel);

        // Вкладка друзей
        friendsListPanel = createFriendsListPanel();
        friendsTabbedPane.addTab("Мои друзья", friendsListPanel);

        // Вкладка заявок
        requestsPanel = createRequestsPanel();
        friendsTabbedPane.addTab("Заявки", requestsPanel);

        add(friendsTabbedPane, BorderLayout.CENTER);

        // Загружаем данные
        refreshAll();
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(35, 25, 45));
        panel.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel titleLabel = new JLabel("Друзья");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);

        panel.add(titleLabel, BorderLayout.WEST);

        return panel;
    }

    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(25, 15, 30));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Верхняя часть с поиском
        JPanel searchTopPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        searchTopPanel.setBackground(new Color(25, 15, 30));

        JLabel searchLabel = new JLabel("Поиск:");
        searchLabel.setFont(new Font("Arial", Font.BOLD, 16));
        searchLabel.setForeground(Color.WHITE);
        searchTopPanel.add(searchLabel);

        searchField = new JTextField(30);
        searchField.setFont(new Font("Arial", Font.PLAIN, 14));
        searchField.setBackground(new Color(60, 45, 75));
        searchField.setForeground(Color.WHITE);
        searchField.setCaretColor(Color.WHITE);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 70, 120)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        searchField.addActionListener(e -> searchUsers());
        searchTopPanel.add(searchField);

        JButton searchBtn = createStyledButton("Найти", new Color(110, 70, 140));
        searchBtn.addActionListener(e -> searchUsers());
        searchTopPanel.add(searchBtn);

        panel.add(searchTopPanel, BorderLayout.NORTH);

        // Панель для результатов поиска
        searchResultsPanel = new JPanel();
        searchResultsPanel.setLayout(new BoxLayout(searchResultsPanel, BoxLayout.Y_AXIS));
        searchResultsPanel.setBackground(new Color(45, 30, 55));

        searchScrollPane = new JScrollPane(searchResultsPanel);
        searchScrollPane.setBorder(BorderFactory.createLineBorder(new Color(100, 70, 120)));
        searchScrollPane.getViewport().setBackground(new Color(45, 30, 55));

        panel.add(searchScrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createFriendsListPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(25, 15, 30));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Мои друзья");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(new EmptyBorder(0, 0, 15, 0));
        panel.add(titleLabel, BorderLayout.NORTH);

        // Панель для карточек друзей
        friendsCardsPanel = new JPanel();
        friendsCardsPanel.setLayout(new BoxLayout(friendsCardsPanel, BoxLayout.Y_AXIS));
        friendsCardsPanel.setBackground(new Color(45, 30, 55));

        friendsScrollPane = new JScrollPane(friendsCardsPanel);
        friendsScrollPane.setBorder(BorderFactory.createLineBorder(new Color(100, 70, 120)));
        friendsScrollPane.getViewport().setBackground(new Color(45, 30, 55));

        panel.add(friendsScrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createRequestsPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 10, 10));
        panel.setBackground(new Color(25, 15, 30));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Входящие заявки
        JPanel incomingPanel = new JPanel(new BorderLayout(10, 10));
        incomingPanel.setBackground(new Color(35, 25, 45));
        incomingPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 70, 120)),
                new EmptyBorder(15, 15, 15, 15)
        ));

        JLabel incomingLabel = new JLabel("Входящие заявки");
        incomingLabel.setFont(new Font("Arial", Font.BOLD, 16));
        incomingLabel.setForeground(Color.WHITE);
        incomingPanel.add(incomingLabel, BorderLayout.NORTH);

        incomingCardsPanel = new JPanel();
        incomingCardsPanel.setLayout(new BoxLayout(incomingCardsPanel, BoxLayout.Y_AXIS));
        incomingCardsPanel.setBackground(new Color(45, 30, 55));

        JScrollPane incomingScroll = new JScrollPane(incomingCardsPanel);
        incomingScroll.setBorder(BorderFactory.createLineBorder(new Color(100, 70, 120)));
        incomingScroll.getViewport().setBackground(new Color(45, 30, 55));

        incomingPanel.add(incomingScroll, BorderLayout.CENTER);

        // Исходящие заявки
        JPanel outgoingPanel = new JPanel(new BorderLayout(10, 10));
        outgoingPanel.setBackground(new Color(35, 25, 45));
        outgoingPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 70, 120)),
                new EmptyBorder(15, 15, 15, 15)
        ));

        JLabel outgoingLabel = new JLabel("Исходящие заявки");
        outgoingLabel.setFont(new Font("Arial", Font.BOLD, 16));
        outgoingLabel.setForeground(Color.WHITE);
        outgoingPanel.add(outgoingLabel, BorderLayout.NORTH);

        outgoingCardsPanel = new JPanel();
        outgoingCardsPanel.setLayout(new BoxLayout(outgoingCardsPanel, BoxLayout.Y_AXIS));
        outgoingCardsPanel.setBackground(new Color(45, 30, 55));

        JScrollPane outgoingScroll = new JScrollPane(outgoingCardsPanel);
        outgoingScroll.setBorder(BorderFactory.createLineBorder(new Color(100, 70, 120)));
        outgoingScroll.getViewport().setBackground(new Color(45, 30, 55));

        outgoingPanel.add(outgoingScroll, BorderLayout.CENTER);

        panel.add(incomingPanel);
        panel.add(outgoingPanel);

        return panel;
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.WHITE, 1),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor.brighter());
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.YELLOW, 2),
                        BorderFactory.createEmptyBorder(7, 14, 7, 14)
                ));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.WHITE, 1),
                        BorderFactory.createEmptyBorder(8, 15, 8, 15)
                ));
            }
        });

        return button;
    }

    private void searchUsers() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Введите имя пользователя для поиска",
                    "Поиск",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        new Thread(() -> {
            List<User> users = userDAO.searchUsers(query, currentUser.getId());

            SwingUtilities.invokeLater(() -> {
                displaySearchResults(users);
            });
        }).start();
    }

    private void displaySearchResults(List<User> users) {
        searchResultsPanel.removeAll();

        if (users.isEmpty()) {
            JLabel emptyLabel = new JLabel("Пользователи не найдены");
            emptyLabel.setFont(new Font("Arial", Font.PLAIN, 16));
            emptyLabel.setForeground(new Color(150, 150, 150));
            emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
            emptyLabel.setBorder(new EmptyBorder(50, 0, 50, 0));
            searchResultsPanel.add(emptyLabel);
        } else {
            for (User user : users) {
                JPanel userCard = createSearchUserCard(user);
                searchResultsPanel.add(userCard);
                searchResultsPanel.add(Box.createVerticalStrut(5));
            }
        }

        searchResultsPanel.revalidate();
        searchResultsPanel.repaint();
    }

    private JPanel createSearchUserCard(User user) {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(new Color(45, 30, 55));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 70, 120), 1),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        // Имя пользователя
        JLabel nameLabel = new JLabel(user.getUsername());
        nameLabel.setFont(new Font("Arial", Font.BOLD, 16));
        nameLabel.setForeground(Color.WHITE);

        // Определяем статус дружбы
        String status = userDAO.getFriendStatus(currentUser.getId(), user.getId());

        // Кнопка действия
        JButton actionBtn = new JButton();
        actionBtn.setFont(new Font("Arial", Font.BOLD, 12));
        actionBtn.setFocusPainted(false);
        actionBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        switch (status) {
            case "accepted":
                actionBtn.setText("В ДРУЗЬЯХ");
                actionBtn.setBackground(new Color(60, 100, 80));
                actionBtn.setForeground(Color.WHITE);
                actionBtn.setEnabled(false);
                break;
            case "pending":
                actionBtn.setText("ЗАЯВКА ОТПРАВЛЕНА");
                actionBtn.setBackground(new Color(80, 80, 80));
                actionBtn.setForeground(new Color(200, 200, 200));
                actionBtn.setEnabled(false);
                break;
            case "incoming":
                actionBtn.setText("ПРИНЯТЬ ЗАЯВКУ");
                actionBtn.setBackground(new Color(200, 100, 0));
                actionBtn.setForeground(Color.WHITE);
                actionBtn.addActionListener(e -> acceptRequest(user));
                break;
            default:
                actionBtn.setText("ДОБАВИТЬ В ДРУЗЬЯ");
                actionBtn.setBackground(new Color(110, 70, 140));
                actionBtn.setForeground(Color.WHITE);
                actionBtn.addActionListener(e -> sendFriendRequest(user));
                break;
        }

        actionBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.WHITE, 1),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));

        card.add(nameLabel, BorderLayout.CENTER);
        card.add(actionBtn, BorderLayout.EAST);

        return card;
    }

    private void refreshAll() {
        new Thread(() -> {
            // Загружаем друзей
            List<User> friends = userDAO.getFriends(currentUser.getId());
            // Загружаем входящие заявки
            List<User> incoming = userDAO.getIncomingRequests(currentUser.getId());
            // Загружаем исходящие заявки
            List<User> outgoing = userDAO.getOutgoingRequests(currentUser.getId());

            SwingUtilities.invokeLater(() -> {
                // Обновляем список друзей
                updateFriendsPanel(friends);

                // Обновляем заявки
                updateRequestsPanel(incoming, outgoing);

                // Обновляем заголовки с количеством
                friendsTabbedPane.setTitleAt(1, "Мои друзья (" + friends.size() + ")");
                friendsTabbedPane.setTitleAt(2, "Заявки (" + incoming.size() + "/" + outgoing.size() + ")");
            });
        }).start();
    }

    private void updateFriendsPanel(List<User> friends) {
        friendsCardsPanel.removeAll();

        if (friends.isEmpty()) {
            JLabel emptyLabel = new JLabel("У вас пока нет друзей");
            emptyLabel.setFont(new Font("Arial", Font.PLAIN, 16));
            emptyLabel.setForeground(new Color(150, 150, 150));
            emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
            emptyLabel.setBorder(new EmptyBorder(50, 0, 50, 0));
            friendsCardsPanel.add(emptyLabel);
        } else {
            for (User friend : friends) {
                JPanel friendCard = createFriendCard(friend);
                friendsCardsPanel.add(friendCard);
                friendsCardsPanel.add(Box.createVerticalStrut(5));
            }
        }

        friendsCardsPanel.revalidate();
        friendsCardsPanel.repaint();
    }

    private JPanel createFriendCard(User friend) {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(new Color(45, 30, 55));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 70, 120), 1),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        // Делаем карточку кликабельной
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Имя друга
        JLabel nameLabel = new JLabel(friend.getUsername());
        nameLabel.setFont(new Font("Arial", Font.BOLD, 16));
        nameLabel.setForeground(Color.WHITE);

        // Кнопка удаления из друзей
        JButton removeBtn = new JButton("УДАЛИТЬ");
        removeBtn.setFont(new Font("Arial", Font.BOLD, 12));
        removeBtn.setBackground(new Color(140, 70, 70));
        removeBtn.setForeground(Color.WHITE);
        removeBtn.setFocusPainted(false);
        removeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        removeBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.WHITE, 1),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));

        // Отдельный слушатель для кнопки
        removeBtn.addActionListener(e -> removeFriend(friend));

        card.add(nameLabel, BorderLayout.CENTER);
        card.add(removeBtn, BorderLayout.EAST);

        // Слушатель для карточки (открытие профиля)
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                // Проверяем, что клик был не по кнопке
                if (e.getSource() != removeBtn) {
                    openUserProfile(friend);
                }
            }

            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                card.setBackground(new Color(60, 45, 75));
                card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(150, 120, 170), 2),
                        BorderFactory.createEmptyBorder(14, 19, 14, 19)
                ));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                card.setBackground(new Color(45, 30, 55));
                card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(100, 70, 120), 1),
                        BorderFactory.createEmptyBorder(15, 20, 15, 20)
                ));
            }
        });

        return card;
    }

    private void openUserProfile(User user) {
        // Создаем новое окно для профиля
        JFrame profileFrame = new JFrame("Профиль - " + user.getUsername());
        profileFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        profileFrame.setSize(900, 700);
        profileFrame.setLocationRelativeTo(null);

        // Создаем панель профиля
        UserProfilePanel profilePanel = new UserProfilePanel(user, currentUser, movieDAO, userDAO);
        profileFrame.add(profilePanel);

        profileFrame.setVisible(true);
    }

    private void updateRequestsPanel(List<User> incoming, List<User> outgoing) {
        // Обновляем входящие
        incomingCardsPanel.removeAll();
        if (incoming.isEmpty()) {
            JLabel emptyLabel = new JLabel("Нет входящих заявок");
            emptyLabel.setFont(new Font("Arial", Font.PLAIN, 14));
            emptyLabel.setForeground(new Color(150, 150, 150));
            emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
            emptyLabel.setBorder(new EmptyBorder(20, 0, 20, 0));
            incomingCardsPanel.add(emptyLabel);
        } else {
            for (User user : incoming) {
                JPanel requestCard = createIncomingRequestCard(user);
                incomingCardsPanel.add(requestCard);
                incomingCardsPanel.add(Box.createVerticalStrut(5));
            }
        }

        // Обновляем исходящие
        outgoingCardsPanel.removeAll();
        if (outgoing.isEmpty()) {
            JLabel emptyLabel = new JLabel("Нет исходящих заявок");
            emptyLabel.setFont(new Font("Arial", Font.PLAIN, 14));
            emptyLabel.setForeground(new Color(150, 150, 150));
            emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
            emptyLabel.setBorder(new EmptyBorder(20, 0, 20, 0));
            outgoingCardsPanel.add(emptyLabel);
        } else {
            for (User user : outgoing) {
                JPanel requestCard = createOutgoingRequestCard(user);
                outgoingCardsPanel.add(requestCard);
                outgoingCardsPanel.add(Box.createVerticalStrut(5));
            }
        }

        incomingCardsPanel.revalidate();
        incomingCardsPanel.repaint();
        outgoingCardsPanel.revalidate();
        outgoingCardsPanel.repaint();
    }

    private JPanel createIncomingRequestCard(User user) {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(new Color(45, 30, 55));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 70, 120), 1),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        JLabel nameLabel = new JLabel(user.getUsername() + " хочет добавить вас в друзья");
        nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        nameLabel.setForeground(Color.WHITE);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setBackground(new Color(45, 30, 55));

        JButton acceptBtn = new JButton("ПРИНЯТЬ");
        acceptBtn.setFont(new Font("Arial", Font.BOLD, 12));
        acceptBtn.setBackground(new Color(60, 100, 80));
        acceptBtn.setForeground(Color.WHITE);
        acceptBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.WHITE, 1),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        acceptBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        acceptBtn.addActionListener(e -> acceptRequest(user));

        JButton rejectBtn = new JButton("ОТКЛОНИТЬ");
        rejectBtn.setFont(new Font("Arial", Font.BOLD, 12));
        rejectBtn.setBackground(new Color(140, 70, 70));
        rejectBtn.setForeground(Color.WHITE);
        rejectBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.WHITE, 1),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        rejectBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        rejectBtn.addActionListener(e -> rejectRequest(user));

        buttonPanel.add(acceptBtn);
        buttonPanel.add(rejectBtn);

        card.add(nameLabel, BorderLayout.CENTER);
        card.add(buttonPanel, BorderLayout.EAST);

        return card;
    }

    private JPanel createOutgoingRequestCard(User user) {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(new Color(45, 30, 55));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 70, 120), 1),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        JLabel nameLabel = new JLabel(user.getUsername() + " (ожидает ответа)");
        nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        nameLabel.setForeground(Color.WHITE);

        JButton cancelBtn = new JButton("ОТМЕНИТЬ ЗАЯВКУ");
        cancelBtn.setFont(new Font("Arial", Font.BOLD, 12));
        cancelBtn.setBackground(new Color(140, 70, 70));
        cancelBtn.setForeground(Color.WHITE);
        cancelBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.WHITE, 1),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelBtn.addActionListener(e -> cancelRequest(user));

        card.add(nameLabel, BorderLayout.CENTER);
        card.add(cancelBtn, BorderLayout.EAST);

        return card;
    }

    private void sendFriendRequest(User friend) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Отправить заявку в друзья пользователю " + friend.getUsername() + "?",
                "Подтверждение",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            boolean success = userDAO.sendFriendRequest(currentUser.getId(), friend.getId());
            if (success) {
                JOptionPane.showMessageDialog(this,
                        "Заявка отправлена",
                        "Успех",
                        JOptionPane.INFORMATION_MESSAGE);
                refreshAll();
                searchUsers();
            }
        }
    }

    private void acceptRequest(User friend) {
        boolean success = userDAO.acceptFriendRequest(currentUser.getId(), friend.getId());
        if (success) {
            JOptionPane.showMessageDialog(this,
                    "Вы приняли заявку от " + friend.getUsername(),
                    "Успех",
                    JOptionPane.INFORMATION_MESSAGE);
            refreshAll();
            searchUsers();
        }
    }

    private void rejectRequest(User friend) {
        boolean success = userDAO.rejectFriendRequest(currentUser.getId(), friend.getId());
        if (success) {
            JOptionPane.showMessageDialog(this,
                    "Заявка от " + friend.getUsername() + " отклонена",
                    "Успех",
                    JOptionPane.INFORMATION_MESSAGE);
            refreshAll();
            searchUsers();
        }
    }

    private void cancelRequest(User friend) {
        boolean success = userDAO.removeFriend(currentUser.getId(), friend.getId());
        if (success) {
            JOptionPane.showMessageDialog(this,
                    "Заявка пользователю " + friend.getUsername() + " отменена",
                    "Успех",
                    JOptionPane.INFORMATION_MESSAGE);
            refreshAll();
            searchUsers();
        }
    }

    private void removeFriend(User friend) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Удалить " + friend.getUsername() + " из друзей?",
                "Подтверждение",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            boolean success = userDAO.removeFriend(currentUser.getId(), friend.getId());
            if (success) {
                JOptionPane.showMessageDialog(this,
                        "Пользователь удален из друзей",
                        "Успех",
                        JOptionPane.INFORMATION_MESSAGE);
                refreshAll();
                searchUsers();
            }
        }
    }
}