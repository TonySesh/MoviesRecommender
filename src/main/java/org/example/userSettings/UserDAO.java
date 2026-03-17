package org.example.userSettings;

import org.example.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public User register(String username, String password) {
        String sql = "INSERT INTO users (username, password) VALUES (?, ?) RETURNING user_id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password); // в реальном приложении нужно хешировать

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int userId = rs.getInt(1);
                System.out.println("Зарегистрирован пользователь: " + username + " (ID: " + userId + ")");
                return new User(userId, username, password);
            }

        } catch (SQLException e) {
            if (e.getMessage().contains("unique constraint")) {
                System.out.println("Пользователь с таким именем уже существует");
            } else {
                e.printStackTrace();
            }
        }

        return null;
    }

    public User login(String username, String password) {
        String sql = "SELECT user_id, username, password FROM users WHERE username = ? AND password = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password); // в реальном приложении нужно сравнивать хеши

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                User user = new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("password")
                );
                System.out.println("Вход выполнен: " + username + " (ID: " + user.getId() + ")");
                return user;
            } else {
                System.out.println("Неверное имя пользователя или пароль");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<User> getAllUsers(int excludeUserId) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT user_id, username FROM users WHERE user_id != ? ORDER BY username";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, excludeUserId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                users.add(new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        ""
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return users;
    }

    public User getUserById(int userId) {
        String sql = "SELECT user_id, username FROM users WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        ""
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<User> searchUsers(String query, int excludeUserId) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT user_id, username FROM users WHERE user_id != ? AND username ILIKE ? ORDER BY username LIMIT 20";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, excludeUserId);
            pstmt.setString(2, "%" + query + "%");
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                users.add(new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        ""
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return users;
    }

    // Отправить заявку в друзья
    public boolean sendFriendRequest(int userId, int friendId) {
        String sql = "INSERT INTO friends (user_id, friend_id, status) VALUES (?, ?, 'pending') " +
                "ON CONFLICT (user_id, friend_id) DO NOTHING";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setInt(2, friendId);
            int result = pstmt.executeUpdate();

            return result > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // Принять заявку в друзья
    public boolean acceptFriendRequest(int userId, int friendId) {
        String sql = "UPDATE friends SET status = 'accepted' WHERE user_id = ? AND friend_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, friendId); // тот, кто отправлял
            pstmt.setInt(2, userId);   // текущий пользователь
            int result = pstmt.executeUpdate();

            // Добавляем обратную связь
            if (result > 0) {
                String sql2 = "INSERT INTO friends (user_id, friend_id, status) VALUES (?, ?, 'accepted') " +
                        "ON CONFLICT (user_id, friend_id) DO UPDATE SET status = 'accepted'";
                try (PreparedStatement pstmt2 = conn.prepareStatement(sql2)) {
                    pstmt2.setInt(1, userId);
                    pstmt2.setInt(2, friendId);
                    pstmt2.executeUpdate();
                }
            }

            return result > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // Отклонить заявку в друзья
    public boolean rejectFriendRequest(int userId, int friendId) {
        String sql = "UPDATE friends SET status = 'rejected' WHERE user_id = ? AND friend_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, friendId);
            pstmt.setInt(2, userId);
            int result = pstmt.executeUpdate();

            return result > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // Удалить из друзей
    public boolean removeFriend(int userId, int friendId) {
        String sql = "DELETE FROM friends WHERE (user_id = ? AND friend_id = ?) OR (user_id = ? AND friend_id = ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setInt(2, friendId);
            pstmt.setInt(3, friendId);
            pstmt.setInt(4, userId);
            int result = pstmt.executeUpdate();

            return result > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // Получить список друзей (принятые)
    public List<User> getFriends(int userId) {
        List<User> friends = new ArrayList<>();
        String sql = "SELECT u.user_id, u.username FROM users u " +
                "JOIN friends f ON (f.friend_id = u.user_id AND f.user_id = ?) " +
                "WHERE f.status = 'accepted' " +
                "UNION " +
                "SELECT u.user_id, u.username FROM users u " +
                "JOIN friends f ON (f.user_id = u.user_id AND f.friend_id = ?) " +
                "WHERE f.status = 'accepted'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setInt(2, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                friends.add(new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        ""
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return friends;
    }

    // Получить входящие заявки
    public List<User> getIncomingRequests(int userId) {
        List<User> requests = new ArrayList<>();
        String sql = "SELECT u.user_id, u.username FROM users u " +
                "JOIN friends f ON f.user_id = u.user_id " +
                "WHERE f.friend_id = ? AND f.status = 'pending'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                requests.add(new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        ""
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return requests;
    }

    // Получить исходящие заявки
    public List<User> getOutgoingRequests(int userId) {
        List<User> requests = new ArrayList<>();
        String sql = "SELECT u.user_id, u.username FROM users u " +
                "JOIN friends f ON f.friend_id = u.user_id " +
                "WHERE f.user_id = ? AND f.status = 'pending'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                requests.add(new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        ""
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return requests;
    }

    // Проверить статус дружбы
    public String getFriendStatus(int userId, int otherUserId) {
        String sql = "SELECT status FROM friends WHERE user_id = ? AND friend_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setInt(2, otherUserId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getString("status");
            }

            // Проверяем обратную связь
            sql = "SELECT status FROM friends WHERE user_id = ? AND friend_id = ?";
            try (PreparedStatement pstmt2 = conn.prepareStatement(sql)) {
                pstmt2.setInt(1, otherUserId);
                pstmt2.setInt(2, userId);
                rs = pstmt2.executeQuery();
                if (rs.next()) {
                    String status = rs.getString("status");
                    if (status.equals("pending")) return "incoming";
                    return status;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return "none";
    }
}