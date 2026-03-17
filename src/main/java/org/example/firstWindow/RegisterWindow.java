package org.example.firstWindow;

import org.example.userSettings.User;
import org.example.userSettings.UserDAO;

import javax.swing.*;
import java.awt.*;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;

public class RegisterWindow extends JFrame {
    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JPasswordField confirmPasswordField;
    private final UserDAO userDAO;

    public RegisterWindow() {
        userDAO = new UserDAO();

        setTitle("Регистрация нового пользователя");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(450, 350);
        setLocationRelativeTo(null);
        setResizable(false);

        // Основная панель
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(45, 30, 55));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        // Заголовок
        JLabel titleLabel = new JLabel("Регистрация", SwingConstants.CENTER);
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

        // Подтверждение пароля
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        JLabel confirmLabel = new JLabel("Подтвердите:");
        confirmLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        confirmLabel.setForeground(Color.WHITE);
        fieldsPanel.add(confirmLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        confirmPasswordField = new JPasswordField(15);
        confirmPasswordField.setFont(new Font("Arial", Font.PLAIN, 14));
        fieldsPanel.add(confirmPasswordField, gbc);

        // Панель с кнопками
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBackground(new Color(45, 30, 55));

        JButton registerBtn = createStyledButton("Зарегистрироваться", new Color(110, 70, 140));
        registerBtn.addActionListener(e -> register());

        JButton cancelBtn = createStyledButton("Отмена", new Color(80, 60, 90));
        cancelBtn.addActionListener(e -> dispose());

        buttonPanel.add(registerBtn);
        buttonPanel.add(cancelBtn);

        // Собираем всё вместе
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        mainPanel.add(fieldsPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // Обработка Enter
        usernameField.addActionListener(e -> register());
        passwordField.addActionListener(e -> register());
        confirmPasswordField.addActionListener(e -> register());
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

    private void register() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());

        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Заполните все поля",
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this,
                    "Пароли не совпадают",
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (password.length() < 3) {
            JOptionPane.showMessageDialog(this,
                    "Пароль должен быть не менее 3 символов",
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        User user = userDAO.register(username, password);

        if (user != null) {
            JOptionPane.showMessageDialog(this,
                    "Регистрация прошла успешно! Теперь можете войти.",
                    "Успех",
                    JOptionPane.INFORMATION_MESSAGE);
            dispose(); // закрываем окно регистрации
        } else {
            JOptionPane.showMessageDialog(this,
                    "Пользователь с таким именем уже существует",
                    "Ошибка регистрации",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
