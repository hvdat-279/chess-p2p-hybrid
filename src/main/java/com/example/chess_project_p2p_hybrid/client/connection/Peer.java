package com.example.chess_project_p2p_hybrid.client.connection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Đại diện cho kết nối tới ChessServer (mô hình hub-spoke).
 * Gửi/nhận Message dưới dạng JSON.
 */
public class Peer implements AutoCloseable {
    private final String host;
    private final int port;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private MessageHandler handler;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public Peer(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void connect() throws IOException {
        socket = new Socket(host, port);
        socket.setSoTimeout(30000); // 30 seconds timeout
        socket.setTcpNoDelay(true); // Disable Nagle's algorithm for low latency
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        startListener();
    }
    
    public void setSoTimeout(int timeoutMs) {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.setSoTimeout(timeoutMs);
            }
        } catch (IOException e) {
            // Ignore
        }
    }

    public boolean isConnected() {
        if (socket == null || socket.isClosed()) return false;
        try {
            // Quick check: try to read without blocking
            return socket.getInputStream().available() >= 0 || socket.isConnected();
        } catch (IOException e) {
            return false;
        }
    }

    public void setHandler(MessageHandler handler) {
        this.handler = handler;
    }

    public void send(Message message) {
        if (!isConnected()) throw new IllegalStateException("Peer is not connected");
        try {
            out.println(message.toJson());
            out.flush(); // Ensure message is sent immediately
        } catch (Exception e) {
            throw new IllegalStateException("Failed to send message: " + e.getMessage(), e);
        }
    }

    private void startListener() {
        executor.submit(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    Message msg = Message.fromJson(line);
                    dispatch(msg);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
    }

    private void dispatch(Message message) {
        if (handler == null || message == null) return;
        switch (message.getType()) {
            case MOVE -> handler.onMove(message);
            case CHAT -> handler.onChat(message);
            case SYSTEM -> handler.onSystem(message);
            case ERROR -> handler.onError(message);
            default -> {}
        }
    }

    @Override
    public void close() {
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
        executor.shutdownNow();
    }
}