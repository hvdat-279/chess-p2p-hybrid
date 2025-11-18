package com.example.chess_project_p2p_hybrid.client.connection;

import com.example.chess_project_p2p_hybrid.client.util.ClientSession;
import javafx.application.Platform;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Quản lý kết nối tới server với retry logic và error handling.
 */
public class ConnectionManager {
    private static final int CONNECTION_TIMEOUT_MS = 5000;
    private static final int MAX_RETRIES = 3;
    
    private final ClientSession session;
    private Peer currentPeer;
    private Consumer<String> statusCallback;
    
    public ConnectionManager(ClientSession session) {
        this.session = session;
    }
    
    public void setStatusCallback(Consumer<String> callback) {
        this.statusCallback = callback;
    }
    
    private void updateStatus(String message) {
        if (statusCallback != null) {
            Platform.runLater(() -> statusCallback.accept(message));
        }
    }
    
    /**
     * Kết nối tới server với retry logic.
     */
    public CompletableFuture<Boolean> connect(String host, int port, String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                try {
                    updateStatus("Đang kết nối... (Lần thử " + attempt + "/" + MAX_RETRIES + ")");
                    
                    Peer peer = new Peer(host, port);
                    peer.connect();
                    
                    this.currentPeer = peer;
                    session.attachPeer(peer);
                    session.setPlayerName(playerName);
                    
                    updateStatus("Kết nối thành công!");
                    return true;
                    
                } catch (IOException e) {
                    updateStatus("Lỗi kết nối (Lần " + attempt + "): " + e.getMessage());
                    if (attempt < MAX_RETRIES) {
                        try {
                            Thread.sleep(1000 * attempt); // Exponential backoff
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            return false;
                        }
                    } else {
                        updateStatus("Không thể kết nối sau " + MAX_RETRIES + " lần thử");
                        return false;
                    }
                }
            }
            return false;
        });
    }
    
    /**
     * Gửi message với error handling.
     */
    public boolean sendMessage(Message message) {
        if (currentPeer == null || !currentPeer.isConnected()) {
            updateStatus("Chưa kết nối tới server");
            return false;
        }
        
        try {
            currentPeer.send(message);
            return true;
        } catch (Exception e) {
            updateStatus("Lỗi gửi message: " + e.getMessage());
            handleConnectionError();
            return false;
        }
    }
    
    /**
     * Xử lý lỗi kết nối.
     */
    private void handleConnectionError() {
        if (currentPeer != null) {
            try {
                currentPeer.close();
            } catch (Exception e) {
                // Ignore
            }
            currentPeer = null;
        }
        session.disconnect();
        updateStatus("Mất kết nối với server");
    }
    
    /**
     * Đóng kết nối.
     */
    public void disconnect() {
        if (currentPeer != null) {
            try {
                currentPeer.close();
            } catch (Exception e) {
                // Ignore
            }
            currentPeer = null;
        }
    }
    
    public boolean isConnected() {
        return currentPeer != null && currentPeer.isConnected();
    }
    
    public Peer getPeer() {
        return currentPeer;
    }
}

