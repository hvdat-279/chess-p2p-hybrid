package com.example.chess_project_p2p_hybrid.client.connection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * ServerConnection.java
 * Quản lý kết nối TCP tới ChessServer (Hub).
 * Nhiệm vụ:
 * 1. Đăng nhập, tham gia phòng.
 * 2. Nhận thông tin đối thủ (PEER_INFO).
 * 3. Gửi/Nhận tin nhắn Relay khi P2P lỗi.
 */
public class ServerConnection {
    private String serverHost;
    private int serverPort;
    
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Consumer<Message> onMessageReceived;
    private Runnable onDisconnect;
    
    private boolean isConnected = false;

    public ServerConnection(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    public void connect(String host, int port) throws IOException {
        this.serverHost = host;
        this.serverPort = port;
        connect();
    }

    public void connect() throws IOException {
        if (isConnected) {
            close();
        }
        socket = new Socket(serverHost, serverPort);
        // Server connection không cần tcpNoDelay quá gắt, nhưng set true cũng tốt
        socket.setTcpNoDelay(true); 
        
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        
        isConnected = true;
        startReading();
    }

    public void send(Message message) {
        if (!isConnected || out == null) {
            System.err.println("[ServerConnection] Cannot send: Not connected to server.");
            return;
        }
        try {
            out.println(message.toJson());
        } catch (Exception e) {
            System.err.println("[ServerConnection] Error sending to server: " + e.getMessage());
            handleDisconnect();
        }
    }

    public void setOnMessageReceived(Consumer<Message> handler) {
        this.onMessageReceived = handler;
    }

    public void setOnDisconnect(Runnable handler) {
        this.onDisconnect = handler;
    }

    public boolean isConnected() {
        return isConnected && socket != null && !socket.isClosed();
    }

    public void close() {
        isConnected = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            // ignore
        }
        // Do not shutdown executor here if we want to reuse it for reconnect?
        // But executor is single thread. If we reconnect, we submit a new task.
        // It's better to keep executor alive or re-create it.
        // For simplicity, let's keep it alive.
    }

    private void startReading() {
        executor.submit(() -> {
            try {
                String line;
                while (isConnected && (line = in.readLine()) != null) {
                    try {
                        Message msg = Message.fromJson(line);
                        if (onMessageReceived != null) {
                            onMessageReceived.accept(msg);
                        }
                    } catch (Exception e) {
                        System.err.println("[ServerConnection] Parse error: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                // Server chết hoặc mất mạng
                System.err.println("[ServerConnection] Connection lost: " + e.getMessage());
            } finally {
                handleDisconnect();
            }
        });
    }

    private void handleDisconnect() {
        if (isConnected) {
            isConnected = false;
            if (onDisconnect != null) {
                onDisconnect.run();
            }
        }
    }
}