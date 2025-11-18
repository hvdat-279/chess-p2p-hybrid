package com.example.chess_project_p2p_hybrid.client.controller;

import com.example.chess_project_p2p_hybrid.client.connection.Message;
import com.example.chess_project_p2p_hybrid.client.connection.MessageHandler;
import com.example.chess_project_p2p_hybrid.client.connection.MessageType;
import com.example.chess_project_p2p_hybrid.client.connection.Peer;
import com.example.chess_project_p2p_hybrid.client.util.ClientSession;
import com.example.chess_project_p2p_hybrid.client.util.SceneNavigator;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LoginController {

    @FXML private TextField playerNameField;
    @FXML private TextField hostField;
    @FXML private TextField portField;
    @FXML private Button connectButton;
    @FXML private Label statusLabel;

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

        new Thread(() -> {
            try {
                Peer peer = new Peer(host, port);
                peer.connect();

                ClientSession session = ClientSession.getInstance();
                session.attachPeer(peer);
                session.setPlayerName(name);

                peer.setHandler(new LoginMessageHandler(session));

                Map<String, Object> payload = new HashMap<>();
                payload.put("playerName", name);
                payload.put("roomId", "");  // không tạo phòng ở login
                payload.put("createRoom", false);

                peer.send(new Message(name, "server", MessageType.LOGIN, gson.toJson(payload)));
                Platform.runLater(() -> setStatus("Đã gửi yêu cầu, chờ phản hồi..."));
            } catch (IOException e) {
                Platform.runLater(() -> {
                    connectButton.setDisable(false);
                    setStatus("Không thể kết nối tới server: " + e.getMessage());
                });
            }
        }).start();
    }

    private void setStatus(String text) {
        statusLabel.setText(text);
    }

    private class LoginMessageHandler implements MessageHandler {
        LoginMessageHandler(ClientSession session) {
            // Handler chỉ xử lý login response, không cần lưu session
        }

        @Override
        public void onSystem(Message message) {
            JsonObject json = gson.fromJson(message.getContent(), JsonObject.class);
            String event = json.has("event") ? json.get("event").getAsString() : "";
            if ("logged_in".equals(event)) {
                // Chuyển sang MainController và setup handler
                Platform.runLater(() -> {
                    SceneNavigator.showMainScene();
                    // Đảm bảo handler được set sau khi MainController initialize
                    // Handler sẽ được set trong MainController.initialize()
                });
            } else if ("error".equals(event)) {
                Platform.runLater(() -> {
                    connectButton.setDisable(false);
                    setStatus("Server error: " + (json.has("message") ? json.get("message").getAsString() : "Unknown error"));
                });
            }
        }

        @Override
        public void onError(Message message) {
            Platform.runLater(() -> {
                connectButton.setDisable(false);
                setStatus("Server error: " + message.getContent());
            });
        }
    }
}
