package com.example.chess_project_p2p_hybrid.client.controller;

import com.example.chess_project_p2p_hybrid.client.ChessClient;
import com.example.chess_project_p2p_hybrid.client.connection.Message;
import com.example.chess_project_p2p_hybrid.client.connection.MessageHandler;
import com.example.chess_project_p2p_hybrid.client.util.ClientSession;
import com.example.chess_project_p2p_hybrid.client.util.SceneNavigator;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML
    private TextField playerNameField;
    @FXML
    private TextField hostField;
    @FXML
    private TextField portField;
    @FXML
    private Button connectButton;
    @FXML
    private Label statusLabel;

    private final Gson gson = new Gson();

    @FXML
    private void initialize() {
        connectButton.setOnAction(e -> attemptConnect());
    }

    private void attemptConnect() {
        String name = playerNameField.getText();
        String host = hostField.getText();
        String portStr = portField.getText();

        if (name == null || name.isBlank()) {
            setStatus("Vui lòng nhập tên người chơi");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException ex) {
            setStatus("Port không hợp lệ");
            return;
        }

        connectButton.setDisable(true);
        setStatus("Đang kết nối...");

        // Khởi tạo ChessClient
        ClientSession session = ClientSession.getInstance();
        ChessClient client = new ChessClient(session);
        session.setChessClient(client);
        
        // Đặt handler tạm thời để xử lý phản hồi login (nếu cần xử lý riêng)
        // Tuy nhiên, ChessClient đã tự động xử lý Login.
        // Ta chỉ cần lắng nghe status update hoặc message hệ thống.
        
        session.setMessageHandler(new LoginMessageHandler());
        client.setStatusCallback(this::setStatus);

        // Kết nối
        client.connectToServer(host, port, name);
    }

    private void setStatus(String text) {
        Platform.runLater(() -> statusLabel.setText(text));
    }

    private class LoginMessageHandler implements MessageHandler {
        @Override
        public void onSystem(Message message) {
            // ChessClient đã xử lý phần lớn, nhưng ta cần biết khi nào vào được Lobby/Room
            // Tuy nhiên, với thiết kế mới, ChessClient tự động QuickMatch.
            // Ta có thể chuyển màn hình ngay khi nhận được thông báo "waiting" hoặc "room_created"
            
            try {
                JsonObject json = gson.fromJson(message.getContent(), JsonObject.class);
                String event = json.has("event") ? json.get("event").getAsString() : "";
                
                if ("login_success".equals(event) || "waiting".equals(event) || "room_created".equals(event) || "joined".equals(event)) {
                    Platform.runLater(() -> {
                        SceneNavigator.showMainScene();
                    });
                }
            } catch (Exception e) {
                // ignore
            }
        }

        @Override
        public void onError(Message message) {
            Platform.runLater(() -> {
                connectButton.setDisable(false);
                setStatus("Lỗi: " + message.getContent());
            });
        }
    }
}
