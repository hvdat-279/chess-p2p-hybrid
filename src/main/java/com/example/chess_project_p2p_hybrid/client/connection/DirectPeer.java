package com.example.chess_project_p2p_hybrid.client.connection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * DirectPeer.java
 * Quản lý kết nối trực tiếp (P2P) giữa 2 người chơi.
 * Đặc điểm:
 * - Low latency (TCP NoDelay).
 * - Dùng để gửi MOVE và CHAT.
 * - Tự động đóng vai trò Server (lắng nghe) hoặc Client (kết nối) tùy ngữ cảnh.
 */
public class DirectPeer {
    private ServerSocket serverSocket; // Để lắng nghe kết nối đến
    private Socket activeSocket;       // Socket đang hoạt động (dù là accept hay connect)
    private PrintWriter out;
    private BufferedReader in;

    private int listeningPort;
    private boolean isConnected = false;

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private Consumer<Message> onMessageReceived;
    private Consumer<String> onStatusUpdate;
    private Runnable onDisconnect;
    private Runnable onConnectionEstablished;

    public DirectPeer() {
        this(0); // Default to random port
    }

    public DirectPeer(int port) {
        this.listeningPort = port;
        startListening();
    }

    /**
     * Mở port lắng nghe ngẫu nhiên để chờ đối thủ kết nối.
     */
    private void startListening() {
        try {
            // Nếu listeningPort được set (khác 0) từ constructor thì dùng nó
            // Nếu là 0 thì ServerSocket sẽ tự chọn port
            serverSocket = new ServerSocket(listeningPort); 
            if (listeningPort == 0) {
                listeningPort = serverSocket.getLocalPort();
            }
            // log("Listening for P2P on port: " + listeningPort);

            executor.submit(() -> {
                while (!Thread.currentThread().isInterrupted() && serverSocket != null && !serverSocket.isClosed()) {
                    try {
                        Socket client = serverSocket.accept();
                        handleConnection(client);
                        // Trong mô hình 1-1, sau khi accept 1 người thì có thể dừng accept thêm nếu muốn
                        // Nhưng để đơn giản, ta cứ để nó chạy, logic handleConnection sẽ lo việc check
                    } catch (IOException e) {
                        if (!serverSocket.isClosed()) log("Accept error: " + e.getMessage());
                    }
                }
            });
        } catch (IOException e) {
            log("Failed to start listening: " + e.getMessage());
        }
    }

    /**
     * Chủ động kết nối tới đối thủ (khi nhận được PEER_INFO từ Server).
     */
    public void connect(String host, int port) {
        executor.submit(() -> {
            try {
                log("Connecting to P2P peer at " + host + ":" + port);
                Socket socket = new Socket(host, port);
                handleConnection(socket);
            } catch (IOException e) {
                log("Failed to connect to peer: " + e.getMessage());
                if (onStatusUpdate != null) onStatusUpdate.accept("P2P Connect Failed: " + e.getMessage());
            }
        });
    }

    /**
     * Thiết lập luồng đọc/ghi cho socket (dùng chung cho cả chiều In và Out).
     */
    private synchronized void handleConnection(Socket socket) {
        if (isConnected) {
            log("Already connected. Ignoring new connection.");
            try {
                socket.close();
            } catch (IOException e) {
            }
            return;
        }

        try {
            this.activeSocket = socket;
            this.activeSocket.setTcpNoDelay(true); // Quan trọng cho game realtime

            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            this.isConnected = true;
            log("P2P Connection Established!");

            if (onStatusUpdate != null) onStatusUpdate.accept("Kết nối trực tiếp (P2P) thành công!");
            if (onConnectionEstablished != null) onConnectionEstablished.run();

            // Bắt đầu đọc tin nhắn
            executor.submit(this::readLoop);

        } catch (IOException e) {
            log("Stream setup error: " + e.getMessage());
            close();
        }
    }

    private void readLoop() {
        try {
            String line;
            while (isConnected && in != null && (line = in.readLine()) != null) {
                try {
                    Message msg = Message.fromJson(line);
                    if (onMessageReceived != null) {
                        onMessageReceived.accept(msg);
                    }
                } catch (Exception e) {
                    log("Parse error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            log("Connection lost: " + e.getMessage());
        } finally {
            close();
        }
    }

    public synchronized boolean send(Message message) {
        if (!isConnected || out == null) return false;
        try {
            out.println(message.toJson());
            return true;
        } catch (Exception e) {
            log("Send error: " + e.getMessage());
            close();
            return false;
        }
    }

    public synchronized void close() {
        isConnected = false;
        try {
            if (activeSocket != null) activeSocket.close();
            if (in != null) in.close();
            if (out != null) out.close();
        } catch (IOException e) {
        }

        activeSocket = null;
        in = null;
        out = null;

        if (onDisconnect != null) onDisconnect.run();
        log("P2P Connection Closed.");
    }

    public void shutdown() {
        close();
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
        }
        executor.shutdownNow();
    }

    public int getListeningPort() {
        return listeningPort;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setOnMessageReceived(Consumer<Message> handler) {
        this.onMessageReceived = handler;
    }

    public void setOnStatusUpdate(Consumer<String> handler) {
        this.onStatusUpdate = handler;
    }

    public void setOnDisconnect(Runnable handler) {
        this.onDisconnect = handler;
    }

    public void setOnConnectionEstablished(Runnable handler) {
        this.onConnectionEstablished = handler;
    }

    private void log(String msg) {
        System.out.println("[DirectPeer] " + msg);
    }
}
