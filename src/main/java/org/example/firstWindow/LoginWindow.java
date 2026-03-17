package org.example.firstWindow;

import org.example.Program;
import org.example.userSettings.User;
import org.example.userSettings.UserDAO;

import javax.swing.*;
import java.awt.*;
// import java.awt.event.ActionEvent;
// import java.awt.event.ActionListener;

public class LoginWindow extends JFrame {
    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final UserDAO userDAO;

    public LoginWindow() {
        userDAO = new UserDAO();

        setTitle("Вход в систему");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);
        setResizable(false);

        // Основная панель
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(45, 30, 55));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        // Заголовок
        JLabel titleLabel = new JLabel("MyMovies", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));

        // Панель с полями
        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setBackground(new Color(45, 30, 55));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Логин
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel userLabel = new JLabel("Логин:");
        userLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        userLabel.setForeground(Color.WHITE);
        fieldsPanel.add(userLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        usernameField = new JTextField(15);
        usernameField.setFont(new Font("Arial", Font.PLAIN, 14));
        fieldsPanel.add(usernameField, gbc);

        // Пароль
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        JLabel passLabel = new JLabel("Пароль:");
        passLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        passLabel.setForeground(Color.WHITE);
        fieldsPanel.add(passLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        passwordField = new JPasswordField(15);
        passwordField.setFont(new Font("Arial", Font.PLAIN, 14));
        fieldsPanel.add(passwordField, gbc);

        // Панель с кнопками
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBackground(new Color(45, 30, 55));

        JButton loginBtn = createStyledButton("Войти", new Color(110, 70, 140));
        loginBtn.addActionListener(e -> login());

        JButton registerBtn = createStyledButton("Регистрация", new Color(80, 60, 90));
        registerBtn.addActionListener(e -> openRegisterWindow());

        buttonPanel.add(loginBtn);
        buttonPanel.add(registerBtn);

        // Собираем всё вместе
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        mainPanel.add(fieldsPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // Обработка Enter
        usernameField.addActionListener(e -> login());
        passwordField.addActionListener(e -> login());
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.PLAIN, 14));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

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

    private void login() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Заполните все поля",
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        User user = userDAO.login(username, password);

        if (user != null) {
            // Открываем главное окно
            Program mainWindow = new Program(user);
            mainWindow.setVisible(true);
            dispose(); // закрываем окно входа
        } else {
            JOptionPane.showMessageDialog(this,
                    "Неверный логин или пароль",
                    "Ошибка входа",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openRegisterWindow() {
        RegisterWindow registerWindow = new RegisterWindow();
        registerWindow.setVisible(true);
    }

    // для теста
//    public static void main(String[] args) {
//        SwingUtilities.invokeLater(() -> new LoginWindow().setVisible(true));
//    }
}