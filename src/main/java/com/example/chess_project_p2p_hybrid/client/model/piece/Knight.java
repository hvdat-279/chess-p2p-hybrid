package com.example.chess_project_p2p_hybrid.client.model.piece;

import com.example.chess_project_p2p_hybrid.client.model.board.BoardView;
import com.example.chess_project_p2p_hybrid.client.model.board.Position;
import com.example.chess_project_p2p_hybrid.client.model.game.Move;

import java.util.ArrayList;
import java.util.List;

/**
 * Lớp đại diện cho quân Mã (Knight) trong bàn cờ.
 */
public class Knight extends Piece {
    private static final int[][] DELTAS = {
            {-2, -1}, {-2, 1},
            {-1, -2}, {-1, 2},
            {1, -2}, {1, 2},
            {2, -1}, {2, 1}
    };

    public Knight(Color color) {
        super(color, PieceType.KNIGHT);
    }


    /**
     * Sinh tất cả các nước đi hợp lệ của quân Mã từ vị trí hiện tại.
     * Mã là quân duy nhất có thể “nhảy” qua các quân khác.
     * Nó di chuyển theo hình chữ “L”:
     * - 2 ô theo một hướng (ngang hoặc dọc)
     * - sau đó 1 ô theo hướng vuông góc.
     *
     * @param from Vị trí hiện tại của quân Mã
     * @param b    Trạng thái bàn cờ (BoardView)
     * @return Danh sách các nước đi hợp lệ (Move)
     */
    @Override
    public List<Move> generateMoves(Position from, BoardView b) {
        List<Move> moves = new ArrayList<>();

        // Duyệt qua 8 hướng di chuyển có thể
        for (int[] d : DELTAS) {
            Position to = Position.of(from.row() + d[0], from.col() + d[1]);
            // Nếu vượt ngoài bàn cờ thì bỏ qua
            if (!to.isValid()) continue;
            // Nếu ô trống → di chuyển bình thường
            if (b.isEmpty(to)) moves.add(Move.normal(from, to, null));
            // Nếu ô có quân đối thủ → có thể ăn
            else if (b.isEnemyPiece(to, color)) moves.add(Move.capture(from, to, null));
        }
        return moves;
    }
}
