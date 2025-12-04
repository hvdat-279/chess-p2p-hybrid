package com.example.chess_project_p2p_hybrid.client.util;

import com.example.chess_project_p2p_hybrid.client.ChessClient;
import com.example.chess_project_p2p_hybrid.client.connection.MessageHandler;
import com.example.chess_project_p2p_hybrid.client.model.piece.Color;
import com.example.chess_project_p2p_hybrid.client.controller.MainController;
import com.example.chess_project_p2p_hybrid.client.controller.ChatController;

/**
 * Lưu trữ thông tin phiên làm việc của client.
 * Updated: Sử dụng ChessClient thay cho ConnectionManager cũ.
 */
public final class ClientSession {
    private static final ClientSession INSTANCE = new ClientSession();

    private ChessClient chessClient;
    private String playerName;
    private String opponentName = null;
    private String roomId;
    private Color playerColor = Color.WHITE;
    private MessageHandler messageHandler;
    private MainController mainController;
    private ChatController chatController;
    private boolean newGameRequestPending = false;
    private String newGameRequestFrom = null;
    private boolean isLocalGame = false; // Flag để phân biệt local game (2 người 1 máy) vs online game

    private ClientSession() {}

    public static ClientSession getInstance() {
        return INSTANCE;
    }

    public void setChessClient(ChessClient client) {
        this.chessClient = client;
    }

    public ChessClient getChessClient() {
        return chessClient;
    }

    public boolean isConnected() {
        // Tạm thời coi như connected nếu chessClient đã được khởi tạo
        // Logic check connection chi tiết nằm trong ChessClient
        return chessClient != null;
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

    public boolean isLocalGame() {
        return isLocalGame;
    }

    public void setLocalGame(boolean localGame) {
        this.isLocalGame = localGame;
    }

    public void disconnect() {
        if (chessClient != null) {
            chessClient.shutdown();
        }
        chessClient = null;
        opponentName = "Waiting...";
        roomId = null;
        playerColor = Color.WHITE;
        mainController = null;
        chatController = null;
        clearNewGameRequest();
        isLocalGame = false;
    }
}
