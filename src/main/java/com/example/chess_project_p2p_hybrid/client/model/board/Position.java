package com.example.chess_project_p2p_hybrid.client.model.board;

/**
 * Định vị quân cờ
 * Kiểm tra di chuyển hợp lệ
 * ....
 */
public record Position(int row, int col) {
    // row: 0..7, col: 0..7 (8x8)
    // Kiểm tra vị trí có nằm trong bàn cờ không
    public boolean isValid() {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }

    // Tạo Position dễ đọc
    public static Position of(int row, int col) {
        return new Position(row, col);
    }
}
