package com.example.chess_project_p2p_hybrid.client.controller;

import com.example.chess_project_p2p_hybrid.client.connection.Message;
import com.example.chess_project_p2p_hybrid.client.connection.MessageType;

import com.example.chess_project_p2p_hybrid.client.util.ClientSession;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class ChatController {

    @FXML
    private ListView<String> messageList;
    @FXML
    private TextField messageField;
    @FXML
    private Button sendButton;

    private final ObservableList<String> messages = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        messageList.setItems(messages);
        sendButton.setOnAction(e -> sendMessage());
        ClientSession.getInstance().setChatController(this);
    }

    private void sendMessage() {
        String text = messageField.getText();
        if (text == null || text.isBlank())
            return;
        ClientSession session = ClientSession.getInstance();
        if (session.isConnected()) {
            // Gửi chat qua ChessClient (tự động chọn P2P hoặc Relay)
            session.getChessClient().send(new Message(session.getPlayerName(), "opponent", MessageType.CHAT, text));
        }
        appendMessage("Tôi: " + text);
        messageField.clear();
    }

    public void appendMessage(String message) {
        Platform.runLater(() -> {
            messages.add(message);
            messageList.scrollTo(messages.size() - 1);
        });
    }
}
