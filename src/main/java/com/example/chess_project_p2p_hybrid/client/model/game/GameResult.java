package com.example.chess_project_p2p_hybrid.client.model.game;

public enum GameResult {
    ONGOING, // Ván cờ đang diễn ra, chưa có kết quả.
    CHECKMATE_WHITE, // Trắng thắng, nghĩa là đen bị chiếu hết (Black bị checkmate).
    CHECKMATE_BLACK, // Đen thắng, nghĩa là trắng bị chiếu hết (White bị checkmate).
    STALEMATE, // Hòa do bế tắc – không có nước đi hợp lệ nhưng vua không bị chiếu.
    DRAW_BY_AGREEMENT // Hòa do hai bên đồng ý (giống luật cờ vua thật, khi hai người chơi thống nhất dừng ván).
}
