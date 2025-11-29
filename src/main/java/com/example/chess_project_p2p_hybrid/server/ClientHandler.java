package com.example.chess_project_p2p_hybrid.server;

import com.example.chess_project_p2p_hybrid.client.connection.Message;
import com.example.chess_project_p2p_hybrid.client.connection.MessageType;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ChessServer server;
    private PrintWriter out;
    private BufferedReader in;
    
    private String playerName;
    private int p2pPort; // Port mà client này đang lắng nghe P2P
    private String roomId;
    
    private static final Gson GSON = new Gson();

    public ClientHandler(Socket socket, ChessServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            String line;
            while ((line = in.readLine()) != null) {
                try {
                    Message msg = Message.fromJson(line);
                    handleMessage(msg);
                } catch (Exception e) {
                    System.err.println("Invalid message format: " + line);
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + playerName);
        } finally {
            cleanup();
        }
    }

    private void handleMessage(Message msg) {
        switch (msg.getType()) {
            case LOGIN -> handleLogin(msg);
            case SYSTEM -> handleSystem(msg);
            case MOVE, CHAT -> server.relayMessage(this, msg); // Relay nếu client gửi lên
            default -> {}
        }
    }

    private void handleLogin(Message msg) {
        try {
            JsonObject json = JsonParser.parseString(msg.getContent()).getAsJsonObject();
            this.playerName = json.get("name").getAsString();
            if (json.has("p2p_port")) {
                this.p2pPort = json.get("p2p_port").getAsInt();
            }
            
            System.out.println("Player logged in: " + playerName + " (P2P Port: " + p2pPort + ")");
            
            // Gửi thông báo đăng nhập thành công về cho Client
            JsonObject response = new JsonObject();
            response.addProperty("event", "login_success");
            send(new Message("server", playerName, MessageType.SYSTEM, response.toString()));
            
            // Không tự động ghép cặp nữa
            // server.getRoomManager().quickMatch(this);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleSystem(Message msg) {
        try {
            JsonObject json = JsonParser.parseString(msg.getContent()).getAsJsonObject();
            String event = json.has("event") ? json.get("event").getAsString() : "";
            
            switch (event) {
                case "quick_match" -> server.getRoomManager().quickMatch(this);
                case "create_room" -> {
                    server.getRoomManager().createPrivateRoom(this);
                }
                case "join_room" -> {
                    if (json.has("roomId")) {
                        String roomId = json.get("roomId").getAsString();
                        server.getRoomManager().joinPrivateRoom(this, roomId);
                    }
                }
                case "leave_room" -> {
                    server.getRoomManager().removeClient(this);
                    // Reset roomId for this client
                    this.roomId = null;
                }
                default -> {
                    // Relay các sự kiện system khác (resign, undo, new_game_request...) cho đối thủ
                    server.relayMessage(this, msg);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void send(Message msg) {
        if (out != null) {
            out.println(msg.toJson());
        }
    }

    private void cleanup() {
        server.getRoomManager().removeClient(this);
        try { socket.close(); } catch (IOException e) {}
    }

    public String getPlayerName() { return playerName; }
    public int getP2pPort() { return p2pPort; }
    public String getIpAddress() { return socket.getInetAddress().getHostAddress(); }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
}
