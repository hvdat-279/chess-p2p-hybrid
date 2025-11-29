package com.example.chess_project_p2p_hybrid.client;

import com.example.chess_project_p2p_hybrid.client.connection.*;
import com.example.chess_project_p2p_hybrid.client.util.ClientSession;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;

import java.util.function.Consumer;

/**
 * ChessClient.java
 * Facade chính cho phía Client.
 * Kết nối các thành phần: Peer, DirectPeer, FallbackManager.
 */
public class ChessClient {
    private final ServerConnection serverConnection;
    private final DirectPeer directPeer;
    private final PeerFallbackManager fallbackManager;
    private final ClientSession session;
    
    private Consumer<String> statusCallback;
    
    private String lastHost;
    private int lastPort;

    public ChessClient(ClientSession session) {
        this.session = session;
        
        // Khởi tạo các thành phần
        // Lưu ý: Host/Port server nên lấy từ config, tạm thời hardcode hoặc truyền vào sau
        this.serverConnection = new ServerConnection("localhost", 9999); 
        this.directPeer = new DirectPeer();
        this.fallbackManager = new PeerFallbackManager(serverConnection, directPeer);
        
        setupHandlers();
    }
    
    public void setStatusCallback(Consumer<String> callback) {
        this.statusCallback = callback;
        this.fallbackManager.setStatusCallback(callback);
        this.directPeer.setOnStatusUpdate(callback);
    }
    
    private void updateStatus(String msg) {
        if (statusCallback != null) Platform.runLater(() -> statusCallback.accept(msg));
    }

    private void setupHandlers() {
        // Xử lý tin nhắn từ Server
        serverConnection.setOnMessageReceived(this::handleServerMessage);
        serverConnection.setOnMessageReceived(this::handleServerMessage);
        serverConnection.setOnDisconnect(() -> {
            updateStatus("Mất kết nối tới Server!");
            if (session.getMainController() != null) {
                Platform.runLater(() -> session.getMainController().attemptReconnect());
            }
        });

        // Xử lý tin nhắn từ P2P
        directPeer.setOnMessageReceived(this::handleP2PMessage);
        directPeer.setOnDisconnect(() -> updateStatus("Mất kết nối P2P! Chuyển sang chế độ Relay."));
        directPeer.setOnConnectionEstablished(this::sendHandshake);
    }

    public void connectToServer(String host, int port, String playerName) {
        new Thread(() -> {
            try {
                updateStatus("Đang kết nối Server...");
                this.lastHost = host;
                this.lastPort = port;
                
                // Re-init peer nếu cần đổi host/port
                // Ở đây giả sử dùng peer đã tạo
                serverConnection.connect(host, port);
                
                // Đăng nhập & Gửi port P2P
                JsonObject loginPayload = new JsonObject();
                loginPayload.addProperty("event", "login");
                loginPayload.addProperty("name", playerName);
                loginPayload.addProperty("p2p_port", directPeer.getListeningPort());
                
                serverConnection.send(new Message(playerName, "server", MessageType.LOGIN, loginPayload.toString()));
                
                session.setPlayerName(playerName);

                updateStatus("Đã kết nối Server.");
                
            } catch (Exception e) {
                updateStatus("Lỗi kết nối Server: " + e.getMessage());
            }
        }).start();
    }

    public void send(Message message) {
        fallbackManager.send(message);
    }
    
    // Xử lý tin nhắn đến từ Server (System, PeerInfo, Relay Move)
    private void handleServerMessage(Message msg) {
        MessageHandler handler = session.getMessageHandler();
        if (handler == null) return;

        switch (msg.getType()) {
            case SYSTEM -> handler.onSystem(msg);
            case PEER_INFO -> handlePeerInfo(msg);
            case MOVE -> {
                // Nhận Move qua đường Relay (Server)
                System.out.println("[Client] Received RELAYED MOVE");
                handler.onMove(msg);
            }
            case CHAT -> handler.onChat(msg);
            case ERROR -> handler.onError(msg);
            default -> {}
        }
    }

    // Xử lý tin nhắn đến từ P2P (Move, Chat trực tiếp)
    private void handleP2PMessage(Message msg) {
        MessageHandler handler = session.getMessageHandler();
        if (handler == null) return;

        switch (msg.getType()) {
            case MOVE -> {
                System.out.println("[Client] Received P2P MOVE");
                handler.onMove(msg);
            }
            case CHAT -> handler.onChat(msg);
            case SYSTEM -> handler.onSystem(msg);
            default -> {}
        }
    }

    private void handlePeerInfo(Message msg) {
        try {
            JsonObject json = JsonParser.parseString(msg.getContent()).getAsJsonObject();
            String host = json.get("host").getAsString();
            int port = json.get("port").getAsInt();
            boolean isHost = json.get("isHost").getAsBoolean();
            
            updateStatus("Tìm thấy đối thủ: " + host + ":" + port);
            
            // Server chỉ định ai là người chủ động kết nối
            if (isHost) {
                directPeer.connect(host, port);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void sendHandshake() {
        JsonObject json = new JsonObject();
        json.addProperty("event", "handshake");
        json.addProperty("name", session.getPlayerName());
        directPeer.send(new Message(session.getPlayerName(), "opponent", MessageType.SYSTEM, json.toString()));
    }
    
    public void resetP2P() {
        directPeer.close();
    }

    public void shutdown() {
        serverConnection.close();
        directPeer.shutdown();
    }

    public void reconnect() {
        if (lastHost != null && !lastHost.isEmpty()) {
            connectToServer(lastHost, lastPort, session.getPlayerName());
        }
    }
}
