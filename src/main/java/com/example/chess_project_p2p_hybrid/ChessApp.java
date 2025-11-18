package com.example.chess_project_p2p_hybrid;


import com.example.chess_project_p2p_hybrid.client.util.SceneNavigator;
import javafx.application.Application;
import javafx.stage.Stage;

public class ChessApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        SceneNavigator.setPrimaryStage(stage);
        SceneNavigator.showLoginScene();
    }

    public static void main(String[] args) {
        launch();
    }
}
