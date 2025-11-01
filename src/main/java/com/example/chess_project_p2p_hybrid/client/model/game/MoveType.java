package com.example.chess_project_p2p_hybrid.client.model.game;

public enum MoveType {
    NORMAL, // Nước đi thông thường.
    CAPTURE, // Nước ăn quân đối phương.
    DOUBLE_PAWN, // Tốt đi hai ô từ vị trí ban đầu.
    EN_PASSANT, // Bắt tốt qua đường (theo luật cờ vua).
    CASTLE_KINGSIDE, // Nhập thành cánh vua (vua đi 2 ô sang phải).
    CASTLE_QUEENSIDE, // Nhập thành cánh hậu (vua đi 2 ô sang trái).
    PROMOTION // Phong cấp tốt (thành hậu, xe, mã hoặc tượng).
}
