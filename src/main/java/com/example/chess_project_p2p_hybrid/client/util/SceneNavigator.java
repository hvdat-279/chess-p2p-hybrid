package com.example.chess_project_p2p_hybrid.client.util;

import com.example.chess_project_p2p_hybrid.ChessApp;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Tiện ích chuyển đổi scene giữa Login/Main và mở cửa sổ chat.
 */
public final class SceneNavigator {
    private static Stage primaryStage;

    private SceneNavigator() {
    }

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static void showLoginScene() {
        setScene("login-view.fxml", "Cờ vua P2P Hybrid - Đăng nhập");
    }

    public static void showMainScene() {
        setScene("main-view.fxml", "Cờ vua P2P Hybrid");
    }

    public static void showChatWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(ChessApp.class.getResource("chat-view.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Phòng chat");
            stage.setScene(new Scene(root));
            stage.initOwner(primaryStage);
            stage.initModality(Modality.NONE);
            stage.show();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setScene(String fxml, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(ChessApp.class.getResource(fxml));
            Parent root = loader.load();
            primaryStage.setTitle(title);
            primaryStage.setScene(new Scene(root));
            primaryStage.show();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
