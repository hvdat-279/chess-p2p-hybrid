package com.example.chess_project_p2p_hybrid.client.connection;

public enum MessageType {
    LOGIN,          // Đăng nhập
    SYSTEM,         // Thông báo hệ thống (Room created, Joined...)
    PEER_INFO,      // Server gửi thông tin IP/Port của đối thủ
    MOVE,           // Nước đi (P2P hoặc Relay)
    CHAT,           // Chat (P2P hoặc Relay)
    ERROR,          // Lỗi
    PING,           // Kiểm tra kết nối
    PONG            // Phản hồi kiểm tra
}
