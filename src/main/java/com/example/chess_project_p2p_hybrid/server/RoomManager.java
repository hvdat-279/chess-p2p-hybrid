package com.example.chess_project_p2p_hybrid.server;

import com.example.chess_project_p2p_hybrid.client.connection.Message;
import com.example.chess_project_p2p_hybrid.client.connection.MessageType;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RoomManager {
    private final Map<String, List<ClientHandler>> rooms = new ConcurrentHashMap<>();
    private ClientHandler waitingClient = null; // Hàng đợi đơn giản (1 người)
    
    private static final Gson GSON = new Gson();

    public synchronized void quickMatch(ClientHandler client) {
        if (waitingClient != null && waitingClient != client) {
            // Ghép cặp
            String roomId = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            createRoom(roomId, waitingClient, client);
            waitingClient = null;
        } else {
            // Đưa vào hàng đợi
            waitingClient = client;
            client.send(new Message("server", client.getPlayerName(), MessageType.SYSTEM, "{\"event\":\"waiting\", \"message\":\"Đang tìm đối thủ...\"}"));
        }
    }

    private void createRoom(String roomId, ClientHandler p1, ClientHandler p2) {
        List<ClientHandler> members = new ArrayList<>();
        members.add(p1);
        members.add(p2);
        rooms.put(roomId, members);
        
        p1.setRoomId(roomId);
        p2.setRoomId(roomId);
        
        System.out.println("Room " + roomId + " created for " + p1.getPlayerName() + " vs " + p2.getPlayerName());

        // 1. Gửi thông báo vào phòng (SYSTEM)
        JsonObject roomInfo = new JsonObject();
        roomInfo.addProperty("event", "room_created");
        roomInfo.addProperty("roomId", roomId);
        
        // P1 là WHITE
        roomInfo.addProperty("color", "WHITE");
        roomInfo.addProperty("opponent", p2.getPlayerName());
        p1.send(new Message("server", p1.getPlayerName(), MessageType.SYSTEM, roomInfo.toString()));

        // P2 là BLACK
        roomInfo.addProperty("color", "BLACK");
        roomInfo.addProperty("opponent", p1.getPlayerName());
        p2.send(new Message("server", p2.getPlayerName(), MessageType.SYSTEM, roomInfo.toString()));
        
        // 2. Trao đổi thông tin P2P (PEER_INFO)
        // P1 (White) sẽ chủ động kết nối tới P2 (Black)
        sendPeerInfo(p1, p2, true);  // P1 connect to P2
        sendPeerInfo(p2, p1, false); // P2 wait
    }

    private void sendPeerInfo(ClientHandler recipient, ClientHandler target, boolean isHost) {
        JsonObject json = new JsonObject();
        // Sử dụng IP mà Server nhìn thấy (Public IP nếu qua Internet)
        // thay vì IP mà Client tự báo (thường là Local IP)
        json.addProperty("host", target.getIpAddress());
        json.addProperty("port", target.getP2pPort());
        json.addProperty("isHost", isHost);
        
        recipient.send(new Message("server", recipient.getPlayerName(), MessageType.PEER_INFO, json.toString()));
    }

    public void removeClient(ClientHandler client) {
        if (waitingClient == client) {
            waitingClient = null;
        }
        String roomId = client.getRoomId();
        if (roomId != null && rooms.containsKey(roomId)) {
            List<ClientHandler> members = rooms.get(roomId);
            members.remove(client);
            
            // Thông báo cho người còn lại
            for (ClientHandler other : members) {
                other.send(new Message("server", other.getPlayerName(), MessageType.SYSTEM, "{\"event\":\"opponent_left\"}"));
            }
            
            if (members.isEmpty()) {
                rooms.remove(roomId);
            }
        }
    }

    public List<ClientHandler> getRoomMembers(String roomId) {
        return rooms.get(roomId);
    }
    public synchronized void createPrivateRoom(ClientHandler host) {
        String roomId = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        List<ClientHandler> members = new ArrayList<>();
        members.add(host);
        rooms.put(roomId, members);
        host.setRoomId(roomId);
        
        System.out.println("Private room created: " + roomId + " by " + host.getPlayerName());
        
        // Gửi thông báo cho host biết mã phòng
        JsonObject response = new JsonObject();
        response.addProperty("event", "room_created"); // Hoặc "private_room_created" nếu muốn xử lý riêng
        response.addProperty("roomId", roomId);
        response.addProperty("message", "Đang chờ người chơi khác tham gia...");
        response.addProperty("isHost", true);
        
        host.send(new Message("server", host.getPlayerName(), MessageType.SYSTEM, response.toString()));
    }

    public synchronized void joinPrivateRoom(ClientHandler client, String roomId) {
        List<ClientHandler> members = rooms.get(roomId);
        if (members == null) {
            client.send(new Message("server", client.getPlayerName(), MessageType.SYSTEM, "{\"event\":\"error\", \"message\":\"Phòng không tồn tại!\"}"));
            return;
        }
        
        if (members.size() >= 2) {
            client.send(new Message("server", client.getPlayerName(), MessageType.SYSTEM, "{\"event\":\"error\", \"message\":\"Phòng đã đầy!\"}"));
            return;
        }
        
        // Join thành công
        ClientHandler host = members.get(0);
        createRoom(roomId, host, client); // Tái sử dụng logic createRoom để bắt đầu game
    }
}
