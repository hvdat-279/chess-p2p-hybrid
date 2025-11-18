package com.example.chess_project_p2p_hybrid.client.util;

import com.example.chess_project_p2p_hybrid.client.connection.Peer;
import com.example.chess_project_p2p_hybrid.client.connection.MessageHandler;
import com.example.chess_project_p2p_hybrid.client.model.piece.Color;
import com.example.chess_project_p2p_hybrid.client.controller.MainController;
import com.example.chess_project_p2p_hybrid.client.controller.ChatController;

/**
 * Lưu trữ thông tin phiên làm việc của client sau khi đăng nhập.
 * Được dùng chung giữa các controller (Login, Main, Chat).
 */
public final class ClientSession {
    private static final ClientSession INSTANCE = new ClientSession();

    private Peer peer;
    private String playerName;
    private String opponentName = "Waiting...";
    private String roomId;
    private Color playerColor = Color.WHITE;
    private MessageHandler messageHandler;
    private MainController mainController;
    private ChatController chatController;
    private boolean newGameRequestPending = false; // Đang chờ đối thủ đồng ý
    private String newGameRequestFrom = null; // Ai đã gửi request

    private ClientSession() {}

    public static ClientSession getInstance() {
        return INSTANCE;
    }

    public void attachPeer(Peer peer) {
        this.peer = peer;
    }

    public Peer getPeer() {
        return peer;
    }

    public boolean isConnected() {
        return peer != null && peer.isConnected();
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getOpponentName() {
        return opponentName;
    }

    public void setOpponentName(String opponentName) {
        this.opponentName = opponentName;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setPlayerColor(Color color) {
        this.playerColor = color;
    }

    public Color getPlayerColor() {
        return playerColor;
    }

    public void setMessageHandler(MessageHandler handler) {
        this.messageHandler = handler;
        if (peer != null) {
            peer.setHandler(handler);
        }
    }

    public MessageHandler getMessageHandler() {
        return messageHandler;
    }

    public void setMainController(MainController controller) {
        this.mainController = controller;
    }

    public MainController getMainController() {
        return mainController;
    }

    public void setChatController(ChatController controller) {
        this.chatController = controller;
    }

    public ChatController getChatController() {
        return chatController;
    }

    public void setNewGameRequestPending(boolean pending) {
        this.newGameRequestPending = pending;
    }
    
    public boolean isNewGameRequestPending() {
        return newGameRequestPending;
    }
    
    public void setNewGameRequestFrom(String from) {
        this.newGameRequestFrom = from;
    }
    
    public String getNewGameRequestFrom() {
        return newGameRequestFrom;
    }
    
    public void clearNewGameRequest() {
        this.newGameRequestPending = false;
        this.newGameRequestFrom = null;
    }

    public void disconnect() {
        if (peer != null) {
            peer.close();
        }
        peer = null;
        opponentName = "Waiting...";
        roomId = null;
        playerColor = Color.WHITE;
        mainController = null;
        chatController = null;
        clearNewGameRequest();
    }
}

