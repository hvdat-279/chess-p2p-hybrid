package com.example.chess_project_p2p_hybrid.server;

import com.example.chess_project_p2p_hybrid.client.connection.Message;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class ChessServer {
    private static final int PORT = 9999;
    private final RoomManager roomManager;

    public ChessServer() {
        this.roomManager = new RoomManager();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Chess Server (Hybrid Hub) running on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket, this);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public RoomManager getRoomManager() {
        return roomManager;
    }

    // Relay message cho các thành viên khác trong phòng
    public void relayMessage(ClientHandler sender, Message msg) {
        String roomId = sender.getRoomId();
        if (roomId == null) return;

        List<ClientHandler> members = roomManager.getRoomMembers(roomId);
        if (members != null) {
            for (ClientHandler member : members) {
                if (member != sender) {
                    member.send(msg);
                }
            }
        }
    }

    public static void main(String[] args) {
        new ChessServer().start();
    }
}
