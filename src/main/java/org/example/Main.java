package org.example;

import org.example.firstWindow.LoginWindow;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {

        // Запускаем приложение
        SwingUtilities.invokeLater(() -> {
            new LoginWindow().setVisible(true);
        });
    }
}