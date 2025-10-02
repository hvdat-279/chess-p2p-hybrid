package com.example.chess_project_p2p_hybrid.server;

import java.net.ServerSocket;
import java.net.Socket;

public class ChessServer {
    private static final int PORT = 9999;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Chess Server running on port " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New connection: " + socket.getInetAddress());

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
