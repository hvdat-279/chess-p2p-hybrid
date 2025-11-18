package com.example.chess_project_p2p_hybrid.server;

import com.example.chess_project_p2p_hybrid.client.connection.Message;
import com.example.chess_project_p2p_hybrid.client.connection.MessageType;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server đóng vai trò trung gian chuyển tiếp message giữa các client trong cùng phòng.
 */
public class ChessServer {
    private static final int PORT = 9999;
    private static final Map<String, Room> ROOMS = new ConcurrentHashMap<>();
    private static final Gson GSON = new Gson();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Chess Server running on port " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket);
                new Thread(handler).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class Room {
        private final String id;
        private final List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());

        private Room(String id) {
            this.id = id;
        }

        public void broadcast(Message message, ClientHandler exclude) {
            synchronized (clients) {
                int sentCount = 0;
                for (ClientHandler client : clients) {
                    if (client != exclude) {
                        try {
                            client.send(message);
                            sentCount++;
                            System.out.println("[Room] Sent " + message.getType() + " to " + client.playerName);
                        } catch (Exception e) {
                            System.err.println("[Room] Failed to send to " + client.playerName + ": " + e.getMessage());
                        }
                    }
                }
                System.out.println("[Room] Broadcast " + message.getType() + " to " + sentCount + " client(s)");
            }
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private Room room;
        private String playerName = "Unknown";

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    Message message = Message.fromJson(line);
                    handle(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                cleanup();
            }
        }

        private void handle(Message message) {
            if (message == null) return;
            switch (message.getType()) {
                case LOGIN -> handleLogin(message);
                case CHAT, MOVE -> {
                    if (message.getType() == MessageType.MOVE) {
                        System.out.println("[Server] Received MOVE from " + playerName + " in room " + (room != null ? room.id : "null") + ", content: " + message.getContent());
                    }
                    relay(message);
                }
                case SYSTEM -> handleSystem(message);
                case LOGOUT -> cleanup();
                default -> {}
            }
        }

        private void handleSystem(Message message) {
            try {
                JsonObject json = GSON.fromJson(message.getContent(), JsonObject.class);
                String event = json.has("event") ? json.get("event").getAsString() : "";
                System.out.println("[Server] Received SYSTEM event: " + event + " from " + playerName);
                
                switch (event) {
                    case "create_room" -> handleCreateRoom();
                    case "join_room" -> {
                        if (json.has("roomId")) {
                            handleJoinRoom(json.get("roomId").getAsString());
                        } else {
                            sendError("Room ID không được cung cấp.");
                        }
                    }
                    case "quick_match" -> handleQuickMatch();
                    case "undo", "resign", "new_game_request", "new_game_accept", "new_game_reject", "new_game" -> {
                        // Relay các game events cho các client khác trong room
                        System.out.println("[Server] Relaying game event: " + event + " from " + playerName);
                        relay(message);
                    }
                    default -> {
                        System.out.println("[Server] Unknown event: " + event + ", relaying anyway");
                        // Relay unknown events để đảm bảo tương thích
                        relay(message);
                    }
                }
            } catch (Exception e) {
                System.err.println("[Server] Error handling system message: " + e.getMessage());
                e.printStackTrace();
                sendError("Lỗi xử lý message: " + e.getMessage());
            }
        }

        private void handleCreateRoom() {
            // Rời phòng cũ nếu có
            if (room != null) {
                room.clients.remove(this);
                if (room.clients.isEmpty()) {
                    ROOMS.remove(room.id);
                }
            }
            
            // Tạo phòng mới
            String roomId = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            room = ROOMS.computeIfAbsent(roomId, Room::new);
            room.clients.add(this);
            String color = "WHITE";
            
            Map<String, Object> response = new HashMap<>();
            response.put("event", "room_created");
            response.put("roomId", roomId);
            response.put("color", color);
            response.put("players", room.clients.stream().map(c -> c.playerName).toArray(String[]::new));
            send(new Message("server", playerName, MessageType.SYSTEM, GSON.toJson(response)));
            System.out.println("Room created: " + roomId + " by " + playerName);
        }

        private void handleJoinRoom(String targetRoomId) {
            if (targetRoomId == null || targetRoomId.isBlank()) {
                sendError("Room ID không hợp lệ.");
                return;
            }
            Room targetRoom = ROOMS.get(targetRoomId.toUpperCase());
            if (targetRoom == null) {
                sendError("Phòng không tồn tại: " + targetRoomId);
                System.out.println("Join room failed: Room " + targetRoomId + " not found. Available rooms: " + ROOMS.keySet());
                return;
            }
            System.out.println("Player " + playerName + " joining room " + targetRoomId);
            if (targetRoom.clients.size() >= 2) {
                sendError("Phòng đã đầy.");
                return;
            }
            // rời phòng cũ nếu có
            if (room != null) {
                room.clients.remove(this);
                if (room.clients.isEmpty()) {
                    ROOMS.remove(room.id);
                }
            }
            // vào phòng mới
            room = targetRoom;
            int oldSize = room.clients.size();
            room.clients.add(this);
            // Người đầu tiên là WHITE, người thứ 2 là BLACK
            String color = oldSize == 0 ? "WHITE" : "BLACK";
            Map<String, Object> response = new HashMap<>();
            response.put("event", "joined");
            response.put("roomId", room.id);
            response.put("color", color);
            response.put("players", room.clients.stream().map(c -> c.playerName).toArray(String[]::new));
            send(new Message("server", playerName, MessageType.SYSTEM, GSON.toJson(response)));
            // thông báo cho người khác trong phòng
            Map<String, Object> broadcast = new HashMap<>();
            broadcast.put("event", "player_joined");
            broadcast.put("player", playerName);
            broadcast.put("roomId", room.id);
            broadcast.put("players", room.clients.stream().map(c -> c.playerName).toArray(String[]::new));
            room.broadcast(new Message("server", "all", MessageType.SYSTEM, GSON.toJson(broadcast)), this);
            
            // Nếu có 2 người, gửi signal để cả 2 client reset game và đồng bộ
            if (room.clients.size() == 2) {
                // Gửi game_start signal cho TẤT CẢ clients (bao gồm cả người vừa join)
                Map<String, Object> syncSignal = new HashMap<>();
                syncSignal.put("event", "game_start");
                syncSignal.put("roomId", room.id);
                syncSignal.put("players", room.clients.stream().map(c -> c.playerName).toArray(String[]::new));
                // Broadcast cho tất cả, không exclude ai cả
                for (ClientHandler client : room.clients) {
                    client.send(new Message("server", client.playerName, MessageType.SYSTEM, GSON.toJson(syncSignal)));
                }
            }
        }

        private void handleQuickMatch() {
            // tìm phòng có 1 người
            Room availableRoom = null;
            for (Room r : ROOMS.values()) {
                if (r.clients.size() == 1) {
                    availableRoom = r;
                    break;
                }
            }
            if (availableRoom != null) {
                // ghép vào phòng có sẵn
                if (room != null) {
                    room.clients.remove(this);
                    if (room.clients.isEmpty()) {
                        ROOMS.remove(room.id);
                    }
                }
                room = availableRoom;
                room.clients.add(this);
                String color = "BLACK"; // Người thứ 2 luôn là BLACK
                Map<String, Object> response = new HashMap<>();
                response.put("event", "joined");
                response.put("roomId", room.id);
                response.put("color", color);
                response.put("players", room.clients.stream().map(c -> c.playerName).toArray(String[]::new));
                send(new Message("server", playerName, MessageType.SYSTEM, GSON.toJson(response)));
                // thông báo cho người khác trong phòng
                Map<String, Object> broadcast = new HashMap<>();
                broadcast.put("event", "player_joined");
                broadcast.put("player", playerName);
                broadcast.put("roomId", room.id);
                broadcast.put("players", room.clients.stream().map(c -> c.playerName).toArray(String[]::new));
                room.broadcast(new Message("server", "all", MessageType.SYSTEM, GSON.toJson(broadcast)), this);
                
                // Nếu có 2 người, gửi signal để cả 2 client reset game và đồng bộ
                if (room.clients.size() == 2) {
                    // Gửi game_start signal cho TẤT CẢ clients (bao gồm cả người vừa join)
                    Map<String, Object> syncSignal = new HashMap<>();
                    syncSignal.put("event", "game_start");
                    syncSignal.put("roomId", room.id);
                    syncSignal.put("players", room.clients.stream().map(c -> c.playerName).toArray(String[]::new));
                    // Broadcast cho tất cả, không exclude ai cả
                    for (ClientHandler client : room.clients) {
                        client.send(new Message("server", client.playerName, MessageType.SYSTEM, GSON.toJson(syncSignal)));
                    }
                }
            } else {
                // không có phòng, tạo mới
                handleCreateRoom();
            }
        }

        private void sendError(String error) {
            Map<String, Object> errorMsg = new HashMap<>();
            errorMsg.put("event", "error");
            errorMsg.put("message", error);
            send(new Message("server", playerName, MessageType.SYSTEM, GSON.toJson(errorMsg)));
        }

        private void handleLogin(Message message) {
            LoginPayload payload = GSON.fromJson(message.getContent(), LoginPayload.class);
            playerName = payload.playerName();
            String roomId = payload.roomId();
            boolean createRoom = payload.createRoom();
            
            // Nếu không tạo phòng và không có roomId -> chỉ đăng nhập, chưa vào phòng
            if (!createRoom && (roomId == null || roomId.isBlank())) {
                Map<String, Object> response = new HashMap<>();
                response.put("event", "logged_in");
                response.put("playerName", playerName);
                send(new Message("server", playerName, MessageType.SYSTEM, GSON.toJson(response)));
                return;
            }
            
            // Logic tạo/join phòng (giữ nguyên cho tương thích ngược)
            if (createRoom || roomId == null || roomId.isBlank()) {
                roomId = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            } else if (!ROOMS.containsKey(roomId)) {
                // nếu join phòng chưa tồn tại thì tự tạo
                ROOMS.put(roomId, new Room(roomId));
            }

            room = ROOMS.computeIfAbsent(roomId, Room::new);
            room.clients.add(this);

            // xác định màu dựa theo thứ tự vào phòng
            String color = room.clients.size() == 1 ? "WHITE" : "BLACK";

            // gửi thông tin join thành công cho client hiện tại
            Map<String, Object> content = new HashMap<>();
            content.put("event", "joined");
            content.put("roomId", roomId);
            content.put("color", color);
            content.put("players", room.clients.stream().map(c -> c.playerName).toArray(String[]::new));

            send(new Message("server", playerName, MessageType.SYSTEM, GSON.toJson(content)));

            // thông báo cho người khác trong phòng
            Map<String, Object> broadcast = new HashMap<>();
            broadcast.put("event", "player_joined");
            broadcast.put("player", playerName);
            broadcast.put("roomId", roomId);
            room.broadcast(new Message("server", "all", MessageType.SYSTEM, GSON.toJson(broadcast)), this);
        }

        private void relay(Message message) {
            if (room == null) {
                System.err.println("[Server] Cannot relay message: room is null for player " + playerName);
                return;
            }
            int otherClients = room.clients.size() - 1;
            System.out.println("[Server] Relaying " + message.getType() + " from " + playerName + " to " + otherClients + " other client(s) in room " + room.id);
            if (otherClients == 0) {
                System.out.println("[Server] Warning: No other clients to relay to!");
            }
            room.broadcast(message, this);
            System.out.println("[Server] Broadcast completed");
        }

        private void send(Message message) {
            out.println(message.toJson());
        }

        private void cleanup() {
            try {
                if (room != null) {
                    room.clients.remove(this);
                    if (room.clients.isEmpty()) {
                        ROOMS.remove(room.id);
                    } else {
                        Map<String, Object> broadcast = new HashMap<>();
                        broadcast.put("event", "player_left");
                        broadcast.put("player", playerName);
                        room.broadcast(new Message("server", "all", MessageType.SYSTEM, GSON.toJson(broadcast)), this);
                    }
                }
                if (socket != null) socket.close();
            } catch (IOException ignored) {}
        }
    }

    private record LoginPayload(String playerName, String roomId, boolean createRoom) {}
}
