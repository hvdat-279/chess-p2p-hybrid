package com.example.chess_project_p2p_hybrid.client.controller;

import com.example.chess_project_p2p_hybrid.client.connection.Message;
import com.example.chess_project_p2p_hybrid.client.connection.MessageType;
import com.example.chess_project_p2p_hybrid.client.connection.Peer;
import com.example.chess_project_p2p_hybrid.client.util.ClientSession;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class ChatController {

    @FXML private ListView<String> messageList;
    @FXML private TextField messageField;
    @FXML private Button sendButton;

    private final ObservableList<String> messages = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        messageList.setItems(messages);
        sendButton.setOnAction(e -> sendMessage());
        ClientSession.getInstance().setChatController(this);
    }

    private void sendMessage() {
        String text = messageField.getText();
        if (text == null || text.isBlank()) return;
        ClientSession session = ClientSession.getInstance();
        Peer peer = session.getPeer();
        if (peer != null && session.isConnected()) {
            peer.send(new Message(session.getPlayerName(), session.getRoomId(), MessageType.CHAT, text));
        }
        appendMessage("Me: " + text);
        messageField.clear();
    }

    public void appendMessage(String message) {
        Platform.runLater(() -> {
            messages.add(message);
            messageList.scrollTo(messages.size() - 1);
        });
    }
}
