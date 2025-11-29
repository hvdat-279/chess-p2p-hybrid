package com.example.chess_project_p2p_hybrid.client.connection;

import java.util.function.Consumer;

/**
 * PeerFallbackManager.java
 * Quản lý logic gửi tin nhắn:
 * - Ưu tiên DirectPeer (P2P).
 * - Nếu lỗi -> Fallback sang Peer (Server Relay).
 */
public class PeerFallbackManager {
    private final ServerConnection serverConnection;
    private final DirectPeer directPeer;
    private Consumer<String> statusCallback;

    public PeerFallbackManager(ServerConnection serverConnection, DirectPeer directPeer) {
        this.serverConnection = serverConnection;
        this.directPeer = directPeer;
    }

    public void setStatusCallback(Consumer<String> callback) {
        this.statusCallback = callback;
    }

    private void log(String msg) {
        if (statusCallback != null) statusCallback.accept(msg);
        System.out.println("[FallbackManager] " + msg);
    }

    /**
     * Gửi tin nhắn thông minh.
     * @param message Tin nhắn cần gửi
     * @return true nếu gửi thành công (qua bất kỳ đường nào)
     */
    public boolean send(Message message) {
        boolean isGameData = (message.getType() == MessageType.MOVE || message.getType() == MessageType.CHAT);

        // 1. Ưu tiên P2P cho Move/Chat
        if (isGameData && directPeer.isConnected()) {
            if (directPeer.send(message)) {
                return true;
            }
            log("P2P send failed. Attempting fallback...");
        }

        // 2. Fallback: Gửi qua Server
        // Nếu là Move/Chat mà P2P tạch -> Gửi qua Server để Relay
        if (serverConnection.isConnected()) {
            if (isGameData) {
                log("Relaying message via Server...");
            }
            serverConnection.send(message);
            return true;
        }

        log("Failed to send message. Both P2P and Server are down.");
        return false;
    }
}
